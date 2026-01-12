package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.ConversionRequest;
import gov.nist.oscal.tools.api.model.ConversionResult;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import dev.metaschema.databind.io.IDeserializer;
import dev.metaschema.databind.io.ISerializer;
import dev.metaschema.core.model.IBoundObject;
import dev.metaschema.oscal.lib.OscalBindingContext;
import dev.metaschema.oscal.lib.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ConversionService {

    private final OscalBindingContext bindingContext;
    private final HistoryService historyService;
    private final FileStorageService fileStorageService;

    public ConversionService(HistoryService historyService, FileStorageService fileStorageService) {
        this.bindingContext = OscalBindingContext.instance();
        this.historyService = historyService;
        this.fileStorageService = fileStorageService;
    }

    public ConversionResult convert(ConversionRequest request, String username) {
        long startTime = System.currentTimeMillis();
        ConversionResult result = new ConversionResult(
            false,
            null,
            request.getFromFormat(),
            request.getToFormat()
        );

        try {
            // Step 1: Deserialize the input content
            Object oscalObject = deserializeContent(
                request.getContent(),
                request.getFromFormat(),
                request.getModelType()
            );

            // Step 2: Serialize to the target format
            String convertedContent = serializeContent(
                oscalObject,
                request.getToFormat(),
                request.getModelType()
            );

            result.setSuccess(true);
            result.setContent(convertedContent);

            // Save both input and converted files to storage
            try {
                String inputFileName = request.getFileName() != null ? request.getFileName() : "document" + getFileExtension(request.getFromFormat());
                String outputFileName = request.getFileName() != null
                    ? request.getFileName().replaceFirst("\\.[^.]+$", "") + getFileExtension(request.getToFormat())
                    : "document" + getFileExtension(request.getToFormat());

                // Save the input file
                fileStorageService.saveFile(request.getContent(), inputFileName, request.getModelType(), request.getFromFormat(), username);

                // Save the converted file
                fileStorageService.saveFile(convertedContent, outputFileName, request.getModelType(), request.getToFormat(), username);
            } catch (Exception saveException) {
                // Don't fail conversion if file saving fails
                System.err.println("Failed to save files to storage: " + saveException.getMessage());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("Conversion failed: " + e.getMessage());
        }

        // Save to history
        long durationMs = System.currentTimeMillis() - startTime;
        saveToHistory(request, result, durationMs);

        return result;
    }

    private void saveToHistory(ConversionRequest request, ConversionResult result, long durationMs) {
        try {
            gov.nist.oscal.tools.api.entity.OperationHistory history = new gov.nist.oscal.tools.api.entity.OperationHistory();
            history.setOperationType("CONVERT");
            history.setFileName(request.getFileName() != null ? request.getFileName() : "Document");
            history.setSuccess(result.isSuccess());
            history.setModelType(request.getModelType().getValue());
            history.setFormat(request.getFromFormat() + " → " + request.getToFormat());
            history.setDurationMs(durationMs);

            String details = result.isSuccess()
                ? String.format("Converted from %s to %s", request.getFromFormat(), request.getToFormat())
                : "Conversion failed: " + result.getError();
            history.setDetails(details);

            historyService.saveOperation(history);
        } catch (Exception e) {
            // Log but don't fail the conversion if history save fails
            System.err.println("Failed to save conversion to history: " + e.getMessage());
        }
    }

    private Object deserializeContent(String content, OscalFormat format, OscalModelType modelType) throws IOException {
        // Get the appropriate deserializer
        IDeserializer<?> deserializer = getDeserializer(modelType, format);

        // Write content to temporary file (required by OSCAL library)
        Path tempFile = Files.createTempFile("oscal-input-", getFileExtension(format));
        try {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            return deserializer.deserialize(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String serializeContent(Object oscalObject, OscalFormat format, OscalModelType modelType) throws IOException {
        // Get the appropriate serializer
        ISerializer<?> serializer = getSerializer(modelType, format);

        // Serialize to string
        StringWriter writer = new StringWriter();

        // Use unchecked cast since we know the serializer matches the object type
        // All OSCAL model classes implement IBoundObject
        @SuppressWarnings("unchecked")
        ISerializer<IBoundObject> typedSerializer = (ISerializer<IBoundObject>) serializer;
        typedSerializer.serialize((IBoundObject) oscalObject, writer);

        return writer.toString();
    }

    private IDeserializer<?> getDeserializer(OscalModelType modelType, OscalFormat format) {
        dev.metaschema.databind.io.Format metaschemaFormat = convertFormat(format);

        switch (modelType) {
            case CATALOG:
                return bindingContext.newDeserializer(metaschemaFormat, Catalog.class);
            case PROFILE:
                return bindingContext.newDeserializer(metaschemaFormat, Profile.class);
            case COMPONENT_DEFINITION:
                return bindingContext.newDeserializer(metaschemaFormat, ComponentDefinition.class);
            case SYSTEM_SECURITY_PLAN:
                return bindingContext.newDeserializer(metaschemaFormat, SystemSecurityPlan.class);
            case ASSESSMENT_PLAN:
                return bindingContext.newDeserializer(metaschemaFormat, AssessmentPlan.class);
            case ASSESSMENT_RESULTS:
                return bindingContext.newDeserializer(metaschemaFormat, AssessmentResults.class);
            case PLAN_OF_ACTION_AND_MILESTONES:
                return bindingContext.newDeserializer(metaschemaFormat, PlanOfActionAndMilestones.class);
            case MAPPING_COLLECTION:
                return bindingContext.newDeserializer(metaschemaFormat, MappingCollection.class);
            default:
                throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
    }

    private ISerializer<?> getSerializer(OscalModelType modelType, OscalFormat format) {
        dev.metaschema.databind.io.Format metaschemaFormat = convertFormat(format);

        switch (modelType) {
            case CATALOG:
                return bindingContext.newSerializer(metaschemaFormat, Catalog.class);
            case PROFILE:
                return bindingContext.newSerializer(metaschemaFormat, Profile.class);
            case COMPONENT_DEFINITION:
                return bindingContext.newSerializer(metaschemaFormat, ComponentDefinition.class);
            case SYSTEM_SECURITY_PLAN:
                return bindingContext.newSerializer(metaschemaFormat, SystemSecurityPlan.class);
            case ASSESSMENT_PLAN:
                return bindingContext.newSerializer(metaschemaFormat, AssessmentPlan.class);
            case ASSESSMENT_RESULTS:
                return bindingContext.newSerializer(metaschemaFormat, AssessmentResults.class);
            case PLAN_OF_ACTION_AND_MILESTONES:
                return bindingContext.newSerializer(metaschemaFormat, PlanOfActionAndMilestones.class);
            case MAPPING_COLLECTION:
                return bindingContext.newSerializer(metaschemaFormat, MappingCollection.class);
            default:
                throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
    }

    private dev.metaschema.databind.io.Format convertFormat(OscalFormat format) {
        switch (format) {
            case JSON:
                return dev.metaschema.databind.io.Format.JSON;
            case XML:
                return dev.metaschema.databind.io.Format.XML;
            case YAML:
                return dev.metaschema.databind.io.Format.YAML;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private String getFileExtension(OscalFormat format) {
        switch (format) {
            case JSON:
                return ".json";
            case XML:
                return ".xml";
            case YAML:
                return ".yaml";
            default:
                return ".txt";
        }
    }
}
