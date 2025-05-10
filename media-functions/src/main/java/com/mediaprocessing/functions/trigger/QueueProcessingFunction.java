package com.mediaprocessing.functions.trigger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.mediaprocessing.common.config.AzureStorageConfig;
import com.mediaprocessing.common.config.AzureAiConfig;
import com.mediaprocessing.common.model.ProcessingRequest;
import com.mediaprocessing.common.service.AzureAiService;
import com.mediaprocessing.common.service.BlobStorageService;
import com.mediaprocessing.common.service.ImageProcessingService;
import com.mediaprocessing.common.service.VideoProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class QueueProcessingFunction {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("ProcessMediaQueue")
    public void processMediaQueue(
            @QueueTrigger(
                name = "message",
                queueName = "media-processing-queue",
                connection = "AzureWebJobsStorage"
            ) String message,
            final ExecutionContext context) {
        
        log.info("Queue trigger function processed a message: {}", message);
        
        try {
            // Parse the message to get the processing request
            ProcessingRequest request = objectMapper.readValue(message, ProcessingRequest.class);
            
            // Initialize services
            AzureStorageConfig storageConfig = new AzureStorageConfig();
            storageConfig.setConnectionString(System.getenv("AzureWebJobsStorage"));
            storageConfig.setContainerName(request.getContainerName());
            storageConfig.setThumbnailContainerName("thumbnails");
            storageConfig.setProcessedContainerName("processed");
            
            BlobStorageService blobService = new BlobStorageService(storageConfig);
            
            // Download the media
            byte[] mediaData = blobService.downloadMedia(request.getBlobName());
            
            // Process based on request type
            switch (request.getProcessingType()) {
                case THUMBNAIL:
                case WATERMARK:
                case RESIZE:
                case FILTER:
                case FORMAT_CONVERSION:
                    processImage(request, blobService, mediaData);
                    break;
                    
                case VIDEO_THUMBNAIL:
                case VIDEO_WATERMARK:
                case VIDEO_COMPRESS:
                case AUDIO_EXTRACT:
                case VIDEO_PREVIEW:
                    processVideo(request, blobService, mediaData);
                    break;
                    
                case IMAGE_ANALYSIS:
                case FACE_DETECTION:
                case TEXT_EXTRACTION:
                case CONTENT_MODERATION:
                case OBJECT_RECOGNITION:
                    processAiAnalysis(request, blobService, mediaData);
                    break;
                    
                default:
                    log.warn("Unsupported processing type in queue: {}", request.getProcessingType());
            }
            
        } catch (Exception e) {
            log.error("Error processing queue message: {}", e.getMessage(), e);
        }
    }
    
    private void processImage(ProcessingRequest request, BlobStorageService blobService, byte[] imageData) throws Exception {
        ImageProcessingService imageService = new ImageProcessingService();
        byte[] processedData;
        String suffix;
        
        switch (request.getProcessingType()) {
            case THUMBNAIL:
                int width = Integer.parseInt(request.getParameters().getOrDefault("width", "200"));
                int height = Integer.parseInt(request.getParameters().getOrDefault("height", "200"));
                processedData = imageService.generateThumbnail(imageData, width, height);
                suffix = "thumb";
                break;
                
            case WATERMARK:
                String watermarkText = request.getParameters().getOrDefault("text", "Copyright");
                processedData = imageService.addWatermark(imageData, watermarkText);
                suffix = "watermark";
                break;
                
            case RESIZE:
                int resizeWidth = Integer.parseInt(request.getParameters().getOrDefault("width", "800"));
                int resizeHeight = Integer.parseInt(request.getParameters().getOrDefault("height", "600"));
                processedData = imageService.resizeImage(imageData, resizeWidth, resizeHeight);
                suffix = "resize";
                break;
                
            case FILTER:
                String filterType = request.getParameters().getOrDefault("type", "grayscale");
                processedData = imageService.applyFilter(imageData, filterType);
                suffix = "filter-" + filterType;
                break;
                
            case FORMAT_CONVERSION:
                String targetFormat = request.getParameters().getOrDefault("format", "jpg");
                processedData = imageService.convertFormat(imageData, targetFormat);
                suffix = "convert-" + targetFormat;
                break;
                
            default:
                log.warn("Unsupported image processing type: {}", request.getProcessingType());
                return;
        }
        
        // Upload the processed image
        blobService.uploadProcessedMedia(processedData, request.getBlobName(), suffix, "image/jpeg");
        log.info("Processed image from queue: {}, type: {}", request.getBlobName(), request.getProcessingType());
    }
    
    private void processVideo(ProcessingRequest request, BlobStorageService blobService, byte[] videoData) throws Exception {
        VideoProcessingService videoService = new VideoProcessingService();
        byte[] processedData;
        String suffix;
        String contentType;
        
        switch (request.getProcessingType()) {
            case VIDEO_THUMBNAIL:
                processedData = videoService.extractThumbnail(videoData);
                suffix = "thumb";
                contentType = "image/jpeg";
                break;
                
            case VIDEO_WATERMARK:
                String watermarkText = request.getParameters().getOrDefault("text", "Copyright");
                processedData = videoService.addWatermarkToVideo(videoData, watermarkText);
                suffix = "watermark";
                contentType = "video/mp4";
                break;
                
            case VIDEO_COMPRESS:
                String quality = request.getParameters().getOrDefault("quality", "medium");
                processedData = videoService.compressVideo(videoData, quality);
                suffix = "compress-" + quality;
                contentType = "video/mp4";
                break;
                
            case AUDIO_EXTRACT:
                processedData = videoService.extractAudio(videoData);
                suffix = "audio";
                contentType = "audio/mp3";
                break;
                
            case VIDEO_PREVIEW:
                int duration = Integer.parseInt(request.getParameters().getOrDefault("duration", "10"));
                processedData = videoService.createPreviewClip(videoData, duration);
                suffix = "preview-" + duration + "s";
                contentType = "video/mp4";
                break;
                
            default:
                log.warn("Unsupported video processing type: {}", request.getProcessingType());
                return;
        }
        
        // Upload the processed video
        blobService.uploadProcessedMedia(processedData, request.getBlobName(), suffix, contentType);
        log.info("Processed video from queue: {}, type: {}", request.getBlobName(), request.getProcessingType());
    }
    
    private void processAiAnalysis(ProcessingRequest request, BlobStorageService blobService, byte[] mediaData) throws Exception {
        // Initialize Azure AI configuration
        AzureAiConfig aiConfig = new AzureAiConfig();
        aiConfig.setComputerVisionEndpoint(System.getenv("ComputerVisionEndpoint"));
        aiConfig.setComputerVisionKey(System.getenv("ComputerVisionKey"));
        aiConfig.setTextAnalyticsEndpoint(System.getenv("TextAnalyticsEndpoint"));
        aiConfig.setTextAnalyticsKey(System.getenv("TextAnalyticsKey"));
        
        AzureAiService aiService = new AzureAiService(aiConfig);
        Map<String, Object> results;
        String suffix;
        
        switch (request.getProcessingType()) {
            case IMAGE_ANALYSIS:
                results = aiService.analyzeImage(mediaData);
                suffix = "analysis";
                break;
                
            case FACE_DETECTION:
                results = aiService.detectFaces(mediaData);
                suffix = "faces";
                break;
                
            case TEXT_EXTRACTION:
                results = aiService.recognizeText(mediaData);
                suffix = "text";
                break;
                
            case CONTENT_MODERATION:
                results = aiService.moderateContent(mediaData);
                suffix = "moderation";
                break;
                
            case OBJECT_RECOGNITION:
                results = aiService.analyzeImage(mediaData); // Uses the same method as IMAGE_ANALYSIS
                suffix = "objects";
                break;
                
            default:
                log.warn("Unsupported AI processing type: {}", request.getProcessingType());
                return;
        }
        
        // Convert results to JSON
        String resultsJson = objectMapper.writeValueAsString(results);
        byte[] resultsData = resultsJson.getBytes();
        
        // Upload the AI analysis results
        blobService.uploadProcessedMedia(resultsData, request.getBlobName(), suffix, "application/json");
        log.info("Processed AI analysis from queue: {}, type: {}", request.getBlobName(), request.getProcessingType());
    }
}
