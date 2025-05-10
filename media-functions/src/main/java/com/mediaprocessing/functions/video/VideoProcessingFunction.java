package com.mediaprocessing.functions.video;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.mediaprocessing.common.config.AzureStorageConfig;
import com.mediaprocessing.common.model.ProcessingRequest;
import com.mediaprocessing.common.service.BlobStorageService;
import com.mediaprocessing.common.service.VideoProcessingService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class VideoProcessingFunction {

    @FunctionName("ProcessVideoOnUpload")
    public void processVideoOnUpload(
            @BlobTrigger(
                name = "blob",
                path = "media/{name}",
                dataType = "binary",
                connection = "AzureWebJobsStorage"
            ) byte[] content,
            @BindingName("name") String blobName,
            final ExecutionContext context) {
        
        // Only process video files
        if (!isVideoFile(blobName)) {
            log.info("Skipping non-video file: {}", blobName);
            return;
        }
        
        log.info("Java Blob trigger function processed a video blob. Name: {}, Size: {} bytes", blobName, content.length);
        
        try {
            // Initialize services
            AzureStorageConfig storageConfig = new AzureStorageConfig();
            storageConfig.setConnectionString(System.getenv("AzureWebJobsStorage"));
            storageConfig.setContainerName("media");
            storageConfig.setThumbnailContainerName("thumbnails");
            storageConfig.setProcessedContainerName("processed");
            
            BlobStorageService blobService = new BlobStorageService(storageConfig);
            VideoProcessingService videoService = new VideoProcessingService();
            
            // Generate thumbnail from video
            byte[] thumbnailData = videoService.extractThumbnail(content);
            blobService.uploadThumbnail(thumbnailData, blobName, "image/jpeg");
            
            log.info("Generated thumbnail for video: {}", blobName);
            
        } catch (Exception e) {
            log.error("Error processing video: {}", e.getMessage(), e);
        }
    }
    
    @FunctionName("ProcessVideoOnDemand")
    public HttpResponseMessage processVideoOnDemand(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.FUNCTION,
                route = "video/process"
            ) HttpRequestMessage<Optional<ProcessingRequest>> request,
            final ExecutionContext context) {
        
        log.info("HTTP trigger function for on-demand video processing");
        
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
            
            BlobStorageService blobService = new BlobStorageService(storageConfig);
            VideoProcessingService videoService = new VideoProcessingService();
            
            // Download the original video
            byte[] videoData = blobService.downloadMedia(processingRequest.getBlobName());
            byte[] processedData;
            String suffix;
            String contentType;
            
            // Process based on request type
            switch (processingRequest.getProcessingType()) {
                case VIDEO_THUMBNAIL:
                    processedData = videoService.extractThumbnail(videoData);
                    suffix = "thumb";
                    contentType = "image/jpeg";
                    break;
                    
                case VIDEO_WATERMARK:
                    String watermarkText = processingRequest.getParameters().getOrDefault("text", "Copyright");
                    processedData = videoService.addWatermarkToVideo(videoData, watermarkText);
                    suffix = "watermark";
                    contentType = "video/mp4";
                    break;
                    
                case VIDEO_COMPRESS:
                    String quality = processingRequest.getParameters().getOrDefault("quality", "medium");
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
                    int duration = Integer.parseInt(processingRequest.getParameters().getOrDefault("duration", "10"));
                    processedData = videoService.createPreviewClip(videoData, duration);
                    suffix = "preview-" + duration + "s";
                    contentType = "video/mp4";
                    break;
                    
                default:
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("Unsupported processing type: " + processingRequest.getProcessingType())
                            .build();
            }
            
            // Upload the processed video
            String processedBlobName = blobService.uploadProcessedMedia(
                    processedData, 
                    processingRequest.getBlobName(), 
                    suffix, 
                    contentType
            );
            
            // Return the URL of the processed video
            String processedUrl = blobService.getProcessedMediaUrl(processingRequest.getBlobName(), suffix);
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "status", "success",
                            "processedUrl", processedUrl,
                            "blobName", processedBlobName
                    ))
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing video: {}", e.getMessage(), e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    private boolean isVideoFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".mp4") || 
               lowerCaseName.endsWith(".avi") || 
               lowerCaseName.endsWith(".mov") || 
               lowerCaseName.endsWith(".wmv") || 
               lowerCaseName.endsWith(".mkv");
    }
}
