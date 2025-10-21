package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import gov.nist.oscal.tools.api.model.ConditionOfApprovalRequest;
import gov.nist.oscal.tools.api.model.ConditionOfApprovalResponse;
import gov.nist.oscal.tools.api.service.ConditionOfApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conditions")
@Tag(name = "Conditions of Approval", description = "APIs for managing authorization conditions")
public class ConditionOfApprovalController {

    private final ConditionOfApprovalService conditionService;

    @Autowired
    public ConditionOfApprovalController(ConditionOfApprovalService conditionService) {
        this.conditionService = conditionService;
    }

    @Operation(
        summary = "Create new condition",
        description = "Create a new condition of approval for an authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Condition created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ConditionOfApprovalResponse> createCondition(
            @Valid @RequestBody ConditionOfApprovalRequest request) {
        try {
            LocalDate dueDate = request.getDueDate() != null && !request.getDueDate().isEmpty()
                    ? LocalDate.parse(request.getDueDate())
                    : null;

            ConditionOfApproval condition = conditionService.createCondition(
                    request.getAuthorizationId(),
                    request.getCondition(),
                    request.getConditionType(),
                    dueDate
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ConditionOfApprovalResponse(condition));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Update condition",
        description = "Update an existing condition of approval"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Condition updated successfully"),
        @ApiResponse(responseCode = "404", description = "Condition not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ConditionOfApprovalResponse> updateCondition(
            @PathVariable Long id,
            @RequestBody ConditionOfApprovalRequest request) {
        try {
            LocalDate dueDate = request.getDueDate() != null && !request.getDueDate().isEmpty()
                    ? LocalDate.parse(request.getDueDate())
                    : null;

            ConditionOfApproval condition = conditionService.updateCondition(
                    id,
                    request.getCondition(),
                    request.getConditionType(),
                    dueDate
            );

            return ResponseEntity.ok(new ConditionOfApprovalResponse(condition));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get condition by ID",
        description = "Retrieve a specific condition of approval"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Condition found"),
        @ApiResponse(responseCode = "404", description = "Condition not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConditionOfApprovalResponse> getCondition(@PathVariable Long id) {
        try {
            ConditionOfApproval condition = conditionService.getCondition(id);
            return ResponseEntity.ok(new ConditionOfApprovalResponse(condition));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get conditions by authorization",
        description = "Retrieve all conditions for a specific authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conditions retrieved successfully")
    })
    @GetMapping("/authorization/{authorizationId}")
    public ResponseEntity<List<ConditionOfApprovalResponse>> getConditionsByAuthorization(
            @PathVariable Long authorizationId) {
        try {
            List<ConditionOfApproval> conditions = conditionService.getConditionsByAuthorization(authorizationId);
            List<ConditionOfApprovalResponse> responses = conditions.stream()
                    .map(ConditionOfApprovalResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get conditions by authorization and type",
        description = "Retrieve conditions of a specific type for an authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conditions retrieved successfully")
    })
    @GetMapping("/authorization/{authorizationId}/type/{type}")
    public ResponseEntity<List<ConditionOfApprovalResponse>> getConditionsByAuthorizationAndType(
            @PathVariable Long authorizationId,
            @PathVariable ConditionOfApproval.ConditionType type) {
        try {
            List<ConditionOfApproval> conditions = conditionService.getConditionsByAuthorizationAndType(
                    authorizationId, type);
            List<ConditionOfApprovalResponse> responses = conditions.stream()
                    .map(ConditionOfApprovalResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Delete condition",
        description = "Delete a condition of approval"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Condition deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Condition not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCondition(@PathVariable Long id) {
        try {
            conditionService.deleteCondition(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Delete all conditions for authorization",
        description = "Delete all conditions associated with an authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conditions deleted successfully")
    })
    @DeleteMapping("/authorization/{authorizationId}")
    public ResponseEntity<Void> deleteConditionsByAuthorization(@PathVariable Long authorizationId) {
        try {
            conditionService.deleteConditionsByAuthorization(authorizationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
