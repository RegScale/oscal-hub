package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.config.FileValidationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * ClamAV Scanner Service
 *
 * Provides malware scanning for uploaded files using ClamAV daemon (clamd).
 * Communicates via TCP socket using the INSTREAM protocol.
 *
 * <h2>ClamAV INSTREAM Protocol</h2>
 * <ol>
 *   <li>Send command: "zINSTREAM\0"</li>
 *   <li>Stream file in chunks: [4-byte size (big-endian)][chunk data]</li>
 *   <li>Send zero-length chunk to signal end: [0x00000000]</li>
 *   <li>Receive response: "stream: OK\0" or "stream: {virus-name} FOUND\0"</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>CLAMAV_HOST: ClamAV daemon host (default: localhost)</li>
 *   <li>CLAMAV_PORT: ClamAV daemon port (default: 3310)</li>
 *   <li>CLAMAV_TIMEOUT_MS: Connection/read timeout (default: 30000ms)</li>
 *   <li>CLAMAV_CHUNK_SIZE: Stream chunk size (default: 2048 bytes)</li>
 * </ul>
 *
 * @see <a href="https://docs.clamav.net/manual/Usage/Scanning.html#clamd">ClamAV Documentation</a>
 */
@Service
public class ClamAvScannerService {

    private static final Logger logger = LoggerFactory.getLogger(ClamAvScannerService.class);

    // ClamAV protocol commands
    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PING_COMMAND = "zPING\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] VERSION_COMMAND = "zVERSION\0".getBytes(StandardCharsets.US_ASCII);

    // Response patterns
    private static final String RESPONSE_OK = "stream: OK";
    private static final String RESPONSE_FOUND_SUFFIX = "FOUND";
    private static final String RESPONSE_PONG = "PONG";

    private final FileValidationConfig config;

    @Autowired
    public ClamAvScannerService(FileValidationConfig config) {
        this.config = config;
    }

    /**
     * Scan result record containing scan outcome and details
     */
    public record ScanResult(
        boolean clean,
        String threatName,
        long scanTimeMs,
        String rawResponse
    ) {
        /**
         * Create a clean scan result
         */
        public static ScanResult clean(long scanTimeMs, String rawResponse) {
            return new ScanResult(true, null, scanTimeMs, rawResponse);
        }

        /**
         * Create an infected scan result
         */
        public static ScanResult infected(String threatName, long scanTimeMs, String rawResponse) {
            return new ScanResult(false, threatName, scanTimeMs, rawResponse);
        }

        /**
         * Create an error scan result
         */
        public static ScanResult error(String errorMessage) {
            return new ScanResult(false, null, 0, "ERROR: " + errorMessage);
        }
    }

    /**
     * Scan file bytes for malware using ClamAV INSTREAM protocol
     *
     * @param fileBytes The file content to scan
     * @param filename The filename (for logging purposes)
     * @return ScanResult indicating whether file is clean or infected
     */
    public ScanResult scan(byte[] fileBytes, String filename) {
        if (fileBytes == null || fileBytes.length == 0) {
            logger.warn("Empty file provided for scanning: {}", filename);
            return ScanResult.error("Empty file");
        }

        long startTime = System.currentTimeMillis();
        logger.info("Starting ClamAV scan for file: {} ({} bytes)", filename, fileBytes.length);

        try (Socket socket = createSocket()) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send INSTREAM command
            out.write(INSTREAM_COMMAND);
            out.flush();

            // Stream file in chunks
            streamFileContent(out, fileBytes);

            // Read response
            String response = readResponse(in);
            long scanTimeMs = System.currentTimeMillis() - startTime;

            return parseResponse(response, scanTimeMs, filename);

        } catch (IOException e) {
            long scanTimeMs = System.currentTimeMillis() - startTime;
            logger.error("ClamAV scan failed for file {}: {}", filename, e.getMessage());

            // If ClamAV is unavailable, decide based on config whether to fail-open or fail-closed
            if (config.isClamavFailOpen()) {
                logger.warn("ClamAV unavailable, failing open (allowing file): {}", filename);
                return ScanResult.clean(scanTimeMs, "CLAMAV_UNAVAILABLE_FAIL_OPEN");
            } else {
                return ScanResult.error("ClamAV unavailable: " + e.getMessage());
            }
        }
    }

    /**
     * Stream file content using ClamAV chunk protocol
     *
     * Each chunk is sent as: [4-byte size (big-endian)][chunk data]
     * End of stream is signaled by a zero-length chunk: [0x00000000]
     */
    private void streamFileContent(OutputStream out, byte[] fileBytes) throws IOException {
        int chunkSize = config.getClamavChunkSize();
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);

        int offset = 0;
        while (offset < fileBytes.length) {
            int remaining = fileBytes.length - offset;
            int currentChunkSize = Math.min(chunkSize, remaining);

            // Write chunk size (4 bytes, big-endian)
            sizeBuffer.clear();
            sizeBuffer.putInt(currentChunkSize);
            out.write(sizeBuffer.array());

            // Write chunk data
            out.write(fileBytes, offset, currentChunkSize);
            out.flush();

            offset += currentChunkSize;
        }

        // Send zero-length chunk to signal end of stream
        sizeBuffer.clear();
        sizeBuffer.putInt(0);
        out.write(sizeBuffer.array());
        out.flush();

        logger.debug("Streamed {} bytes in {} chunks", fileBytes.length,
            (fileBytes.length + chunkSize - 1) / chunkSize);
    }

    /**
     * Read response from ClamAV daemon
     */
    private String readResponse(InputStream in) throws IOException {
        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        // Read until null terminator or end of stream
        while ((bytesRead = in.read(buffer)) != -1) {
            responseBuffer.write(buffer, 0, bytesRead);

            // Check for null terminator
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == 0) {
                    // Null terminator found, we have the complete response
                    return responseBuffer.toString(StandardCharsets.US_ASCII).trim();
                }
            }
        }

        return responseBuffer.toString(StandardCharsets.US_ASCII).trim();
    }

    /**
     * Parse ClamAV response to determine scan result
     *
     * Response formats:
     * - Clean: "stream: OK"
     * - Infected: "stream: Eicar-Test-Signature FOUND"
     * - Error: "stream: ERROR ..."
     */
    private ScanResult parseResponse(String response, long scanTimeMs, String filename) {
        if (response == null || response.isEmpty()) {
            logger.error("Empty response from ClamAV for file: {}", filename);
            return ScanResult.error("Empty response from ClamAV");
        }

        logger.debug("ClamAV response for {}: {}", filename, response);

        // Check for clean file
        if (response.contains(RESPONSE_OK)) {
            logger.info("File {} scanned clean in {}ms", filename, scanTimeMs);
            return ScanResult.clean(scanTimeMs, response);
        }

        // Check for infected file
        if (response.contains(RESPONSE_FOUND_SUFFIX)) {
            // Extract threat name: "stream: Eicar-Test-Signature FOUND" -> "Eicar-Test-Signature"
            String threatName = extractThreatName(response);
            logger.warn("MALWARE DETECTED in file {}: {} (scan time: {}ms)",
                filename, threatName, scanTimeMs);
            return ScanResult.infected(threatName, scanTimeMs, response);
        }

        // Unknown response format
        logger.error("Unexpected ClamAV response for {}: {}", filename, response);
        return ScanResult.error("Unexpected response: " + response);
    }

    /**
     * Extract threat name from ClamAV FOUND response
     *
     * Input: "stream: Eicar-Test-Signature FOUND"
     * Output: "Eicar-Test-Signature"
     */
    private String extractThreatName(String response) {
        // Remove "stream: " prefix and " FOUND" suffix
        String trimmed = response.replace("stream: ", "").replace(" FOUND", "").trim();
        return trimmed.isEmpty() ? "Unknown" : trimmed;
    }

    /**
     * Check if ClamAV daemon is available
     *
     * @return true if ClamAV responds to PING command
     */
    public boolean isAvailable() {
        try (Socket socket = createSocket()) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send PING command
            out.write(PING_COMMAND);
            out.flush();

            // Read response
            String response = readResponse(in);
            boolean available = response != null && response.contains(RESPONSE_PONG);

            logger.debug("ClamAV availability check: {} (response: {})", available, response);
            return available;

        } catch (IOException e) {
            logger.debug("ClamAV not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get ClamAV version string
     *
     * @return Version string or "unavailable" if connection fails
     */
    public String getVersion() {
        try (Socket socket = createSocket()) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send VERSION command
            out.write(VERSION_COMMAND);
            out.flush();

            // Read response
            String response = readResponse(in);
            return response != null ? response.trim() : "unknown";

        } catch (IOException e) {
            logger.debug("Could not get ClamAV version: {}", e.getMessage());
            return "unavailable";
        }
    }

    /**
     * Create configured socket connection to ClamAV daemon
     */
    private Socket createSocket() throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(config.getClamavTimeoutMs());
        socket.connect(
            new InetSocketAddress(config.getClamavHost(), config.getClamavPort()),
            config.getClamavTimeoutMs()
        );
        return socket;
    }
}
