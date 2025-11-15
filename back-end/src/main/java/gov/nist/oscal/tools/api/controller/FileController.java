package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.FileUploadRequest;
import gov.nist.oscal.tools.api.model.SavedFile;
import gov.nist.oscal.tools.api.service.FileStorageService;
import gov.nist.oscal.tools.api.service.FileValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "APIs for managing saved OSCAL files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;

    @Autowired
    public FileController(FileStorageService fileStorageService, FileValidationService fileValidationService) {
        this.fileStorageService = fileStorageService;
        this.fileValidationService = fileValidationService;
    }

    @Operation(
        summary = "List all saved files",
        description = "Retrieve a list of all saved OSCAL files for the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Files retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<SavedFile>> listFiles(Principal principal) {
        try {
            List<SavedFile> files = fileStorageService.listFiles(principal.getName());
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get file by ID",
        description = "Retrieve metadata for a specific saved file"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File found"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{fileId}")
    public ResponseEntity<SavedFile> getFile(@PathVariable String fileId, Principal principal) {
        try {
            SavedFile file = fileStorageService.getFile(fileId, principal.getName());
            return ResponseEntity.ok(file);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get file content",
        description = "Retrieve the content of a saved file"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File content retrieved"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Map<String, String>> getFileContent(@PathVariable String fileId, Principal principal) {
        try {
            String content = fileStorageService.getFileContent(fileId, principal.getName());
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Delete file",
        description = "Delete a saved file by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File deleted successfully"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId, Principal principal) {
        try {
            boolean deleted = fileStorageService.deleteFile(fileId, principal.getName());
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Upload and save file",
        description = "Upload an OSCAL document and save it to storage"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Failed to save file")
    })
    @PostMapping
    public ResponseEntity<?> uploadFile(@Valid @RequestBody FileUploadRequest request, Principal principal) {
        try {
            String fileName = request.getFileName() != null ? request.getFileName() : "document." + request.getFormat().toString().toLowerCase();

            // Validate file before saving
            fileValidationService.validateOscalFile(request.getContent(), fileName);

            SavedFile savedFile = fileStorageService.saveFile(
                request.getContent(),
                fileName,
                request.getModelType(),
                request.getFormat(),
                principal.getName()
            );
            return ResponseEntity.ok(savedFile);
        } catch (IllegalArgumentException e) {
            // Validation error - return 400 Bad Request with detailed message
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            // Other errors - return 500 Internal Server Error
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @Operation(
        summary = "Serve organization logo",
        description = "Serve organization logo files (public endpoint, no authentication required)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logo retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Logo not found")
    })
    @GetMapping("/org-logos/{filename}")
    public ResponseEntity<Resource> serveLogo(@PathVariable String filename) {
        try {
            Path logoPath = Paths.get("uploads/org-logos").resolve(filename).normalize();
            Resource resource = new UrlResource(logoPath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(logoPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
