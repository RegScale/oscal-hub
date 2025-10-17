package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@Tag(name = "Operation History", description = "APIs for viewing and managing operation history")
public class HistoryController {

    private final HistoryService historyService;

    @Autowired
    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Operation(
        summary = "Get all operations",
        description = "Get paginated list of all operations in history"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operations retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Page<OperationHistory>> getAllOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationHistory> operations = historyService.getAllOperations(page, size);
        return ResponseEntity.ok(operations);
    }

    @Operation(
        summary = "Get recent operations",
        description = "Get the 10 most recent operations"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent operations retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<OperationHistory>> getRecentOperations() {
        List<OperationHistory> operations = historyService.getRecentOperations();
        return ResponseEntity.ok(operations);
    }

    @Operation(
        summary = "Get operation by ID",
        description = "Get a specific operation by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation found"),
        @ApiResponse(responseCode = "404", description = "Operation not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OperationHistory> getOperationById(@PathVariable Long id) {
        return historyService.getOperationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get operations by type",
        description = "Get paginated list of operations filtered by type (VALIDATE, CONVERT, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operations retrieved successfully")
    })
    @GetMapping("/type/{operationType}")
    public ResponseEntity<Page<OperationHistory>> getOperationsByType(
            @PathVariable String operationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationHistory> operations = historyService.getOperationsByType(operationType, page, size);
        return ResponseEntity.ok(operations);
    }

    @Operation(
        summary = "Get operations by status",
        description = "Get paginated list of operations filtered by success status"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operations retrieved successfully")
    })
    @GetMapping("/status/{success}")
    public ResponseEntity<Page<OperationHistory>> getOperationsByStatus(
            @PathVariable Boolean success,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationHistory> operations = historyService.getOperationsByStatus(success, page, size);
        return ResponseEntity.ok(operations);
    }

    @Operation(
        summary = "Search operations by filename",
        description = "Search operations by filename (partial match)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<OperationHistory>> searchByFileName(
            @RequestParam String filename,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationHistory> operations = historyService.searchByFileName(filename, page, size);
        return ResponseEntity.ok(operations);
    }

    @Operation(
        summary = "Get operations by date range",
        description = "Get operations within a specific date range"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operations retrieved successfully")
    })
    @GetMapping("/daterange")
    public ResponseEntity<Page<OperationHistory>> getOperationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationHistory> operations = historyService.getOperationsByDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(operations);
    }

    @Operation(
        summary = "Get batch operations",
        description = "Get all operations in a batch by batch operation ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch operations retrieved successfully")
    })
    @GetMapping("/batch/{batchOperationId}")
    public ResponseEntity<List<OperationHistory>> getBatchOperations(@PathVariable String batchOperationId) {
        List<OperationHistory> operations = historyService.getBatchOperations(batchOperationId);
        return ResponseEntity.ok(operations);
    }

    @Operation(
        summary = "Get operation statistics",
        description = "Get overall statistics for all operations"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/stats")
    public ResponseEntity<HistoryService.OperationStats> getStatistics() {
        HistoryService.OperationStats stats = historyService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
        summary = "Delete operation",
        description = "Delete a specific operation from history"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Operation not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperation(@PathVariable Long id) {
        if (historyService.getOperationById(id).isPresent()) {
            historyService.deleteOperation(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
