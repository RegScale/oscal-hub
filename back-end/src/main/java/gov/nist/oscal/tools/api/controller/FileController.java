package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.FileUploadRequest;
import gov.nist.oscal.tools.api.model.SavedFile;
import gov.nist.oscal.tools.api.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "APIs for managing saved OSCAL files")
public class FileController {

    private final FileStorageService fileStorageService;

    @Autowired
    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
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
    public ResponseEntity<SavedFile> uploadFile(@Valid @RequestBody FileUploadRequest request, Principal principal) {
        try {
            String fileName = request.getFileName() != null ? request.getFileName() : "document." + request.getFormat().toString().toLowerCase();
            SavedFile savedFile = fileStorageService.saveFile(
                request.getContent(),
                fileName,
                request.getModelType(),
                request.getFormat(),
                principal.getName()
            );
            return ResponseEntity.ok(savedFile);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
