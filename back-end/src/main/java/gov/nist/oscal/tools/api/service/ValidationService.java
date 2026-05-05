package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.metaschema.databind.io.IDeserializer;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private final HistoryService historyService;
    private final FileStorageService fileStorageService;
    private final MetapathConstraintService constraintService;
    private final UserRepository userRepository;

    @Autowired
    public ValidationService(HistoryService historyService,
                             FileStorageService fileStorageService,
                             MetapathConstraintService constraintService,
                             UserRepository userRepository) {
        this.historyService = historyService;
        this.fileStorageService = fileStorageService;
        this.constraintService = constraintService;
        this.userRepository = userRepository;
    }

    @WithSpan("oscal.validate_internal")
    public ValidationResult validate(ValidationRequest request, String username) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult();
        result.setModelType(request.getModelType());
        result.setFormat(request.getFormat());

        try {
            // Determine the format
            Format format = getFormat(request.getFormat());

            // Resolve the calling user (for per-user custom rule enforcement).
            // username may be null for unauthenticated probes; in that case
            // we fall through to the singleton context.
            Long userId = (username == null)
                ? null
                : userRepository.findByUsername(username).map(u -> u.getId()).orElse(null);

            // Build a binding context that enforces the user's custom rules
            // on top of the built-in NIST constraints. When no custom rules
            // apply, the service returns OscalBindingContext.instance().
            String modelKey = request.getModelType().getValue();
            IBindingContext context = constraintService.contextFor(modelKey, userId);

            // Get the appropriate deserializer for the model type
            IDeserializer<?> deserializer = newDeserializer(context, request.getModelType(), format);

            // Write content to a temporary file
            Path tempFile = Files.createTempFile("oscal-validation-", getFileExtension(format));
            try {
                Files.writeString(tempFile, request.getContent(), StandardCharsets.UTF_8);

                // Attempt to deserialize - this performs validation
                deserializer.deserialize(tempFile);

            } finally {
                // Clean up temporary file
                Files.deleteIfExists(tempFile);
            }

            // If we got here, the document is valid
            result.setValid(true);
            logger.info("Successfully validated {} document in {} format",
                request.getModelType().getValue(), request.getFormat());

            // Save the file to storage only if it's a new file (not already saved)
            if (request.getFileId() == null || request.getFileId().trim().isEmpty()) {
                try {
                    String fileName = request.getFileName() != null ? request.getFileName() : "document" + getFileExtension(format);
                    fileStorageService.saveFile(request.getContent(), fileName, request.getModelType(), request.getFormat(), username);
                    logger.info("Saved new file to storage: {}", fileName);
                } catch (Exception saveException) {
                    logger.warn("Failed to save file to storage: {}", saveException.getMessage());
                    // Don't fail validation if file saving fails
                }
            } else {
                logger.info("Skipping file save - document already saved with ID: {}", request.getFileId());
            }

        } catch (Exception e) {
            // Validation failed
            result.setValid(false);
            ValidationError error = new ValidationError();
            error.setMessage(e.getMessage());
            error.setSeverity("error");
            result.addError(error);

            logger.error("Validation failed for {} document: {}",
                request.getModelType().getValue(), e.getMessage());
        }

        // Save to history
        long durationMs = System.currentTimeMillis() - startTime;
        saveToHistory(request, result, durationMs);

        return result;
    }

    private void saveToHistory(ValidationRequest request, ValidationResult result, long durationMs) {
        try {
            OperationHistory history = new OperationHistory();
            history.setOperationType("VALIDATE");
            history.setFileName(request.getFileName() != null ? request.getFileName() : "Document");
            history.setSuccess(result.isValid());
            history.setModelType(request.getModelType().getValue());
            history.setFormat(request.getFormat().toString());
            history.setDurationMs(durationMs);

            String details = result.isValid()
                ? "Document is valid"
                : String.format("Validation failed with %d error(s)", result.getErrors().size());
            history.setDetails(details);

            historyService.saveOperation(history);
        } catch (Exception e) {
            logger.warn("Failed to save operation to history: {}", e.getMessage());
        }
    }

    private Format getFormat(OscalFormat oscalFormat) {
        switch (oscalFormat) {
            case XML:
                return Format.XML;
            case JSON:
                return Format.JSON;
            case YAML:
                return Format.YAML;
            default:
                throw new IllegalArgumentException("Unsupported format: " + oscalFormat);
        }
    }

    private String getFileExtension(Format format) {
        switch (format) {
            case XML:
                return ".xml";
            case JSON:
                return ".json";
            case YAML:
                return ".yaml";
            default:
                return ".txt";
        }
    }

    private IDeserializer<?> newDeserializer(IBindingContext context, OscalModelType modelType, Format format) throws IOException {
        switch (modelType) {
            case CATALOG:
                return context.newDeserializer(format, gov.nist.secauto.oscal.lib.model.Catalog.class);
            case PROFILE:
                return context.newDeserializer(format, gov.nist.secauto.oscal.lib.model.Profile.class);
            case COMPONENT_DEFINITION:
                return context.newDeserializer(format, gov.nist.secauto.oscal.lib.model.ComponentDefinition.class);
            case SYSTEM_SECURITY_PLAN:
                return context.newDeserializer(format, gov.nist.secauto.oscal.lib.model.SystemSecurityPlan.class);
            case ASSESSMENT_PLAN:
                return context.newDeserializer(format, gov.nist.secauto.oscal.lib.model.AssessmentPlan.class);
            case ASSESSMENT_RESULTS:
                return context.newDeserializer(format, gov.nist.secauto.oscal.lib.model.AssessmentResults.class);
            case PLAN_OF_ACTION_AND_MILESTONES:
                return context.newDeserializer(format, gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones.class);
            default:
                throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
    }
}
