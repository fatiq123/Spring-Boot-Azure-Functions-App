package com.mediaprocessing.functions.ai;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.mediaprocessing.common.config.AzureAiConfig;
import com.mediaprocessing.common.config.AzureStorageConfig;
import com.mediaprocessing.common.model.ProcessingRequest;
import com.mediaprocessing.common.service.AzureAiService;
import com.mediaprocessing.common.service.BlobStorageService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class AiProcessingFunction {

    @FunctionName("ProcessMediaWithAi")
    public HttpResponseMessage processMediaWithAi(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.FUNCTION,
                route = "ai/process"
            ) HttpRequestMessage<Optional<ProcessingRequest>> request,
            final ExecutionContext context) {
        
        log.info("HTTP trigger function for AI processing");
        
        ProcessingRequest processingRequest = request.getBody().orElse(null);
        if (processingRequest == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a valid processing request in the request body")
                    .build();
        }
        
        try {
            // Initialize services
            AzureStorageConfig storageConfig = new AzureStorageConfig();
            storageConfig.setConnectionString(System.getenv("AzureWebJobsStorage"));
            storageConfig.setContainerName(processingRequest.getContainerName());
            storageConfig.setThumbnailContainerName("thumbnails");
            storageConfig.setProcessedContainerName("processed");
            
            AzureAiConfig aiConfig = new AzureAiConfig();
            aiConfig.setComputerVisionEndpoint(System.getenv("ComputerVisionEndpoint"));
            aiConfig.setComputerVisionKey(System.getenv("ComputerVisionKey"));
            aiConfig.setTextAnalyticsEndpoint(System.getenv("TextAnalyticsEndpoint"));
            aiConfig.setTextAnalyticsKey(System.getenv("TextAnalyticsKey"));
            aiConfig.setFormRecognizerEndpoint(System.getenv("FormRecognizerEndpoint"));
            aiConfig.setFormRecognizerKey(System.getenv("FormRecognizerKey"));
            
            BlobStorageService blobService = new BlobStorageService(storageConfig);
            AzureAiService aiService = new AzureAiService(aiConfig);
            
            // Download the media
            byte[] mediaData = blobService.downloadMedia(processingRequest.getBlobName());
            Map<String, Object> results;
            
            // Process based on request type
            switch (processingRequest.getProcessingType()) {
                case IMAGE_ANALYSIS:
                    results = aiService.analyzeImage(mediaData);
                    break;
                    
                case FACE_DETECTION:
                    results = aiService.detectFaces(mediaData);
                    break;
                    
                case TEXT_EXTRACTION:
                    results = aiService.recognizeText(mediaData);
                    break;
                    
                case CONTENT_MODERATION:
                    results = aiService.moderateContent(mediaData);
                    break;
                    
                default:
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("Unsupported AI processing type: " + processingRequest.getProcessingType())
                            .build();
            }
            
            // Store the results as metadata
            Map<String, String> metadata = blobService.getBlobMetadata(processingRequest.getBlobName());
            metadata.put("aiProcessed", "true");
            metadata.put("aiProcessingType", processingRequest.getProcessingType().toString());
            blobService.setBlobMetadata(processingRequest.getBlobName(), metadata);
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "status", "success",
                            "mediaId", processingRequest.getMediaId(),
                            "blobName", processingRequest.getBlobName(),
                            "results", results
                    ))
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing media with AI: {}", e.getMessage(), e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
