package com.mediaprocessing.functions.image;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.mediaprocessing.common.config.AzureStorageConfig;
import com.mediaprocessing.common.model.ProcessingRequest;
import com.mediaprocessing.common.service.BlobStorageService;
import com.mediaprocessing.common.service.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class ImageProcessingFunction {

    @FunctionName("ProcessImageOnUpload")
    public void processImageOnUpload(
            @BlobTrigger(
                name = "blob",
                path = "media/{name}",
                dataType = "binary",
                connection = "AzureWebJobsStorage"
            ) byte[] content,
            @BindingName("name") String blobName,
            final ExecutionContext context) {
        
        log.info("Java Blob trigger function processed a blob. Name: {}, Size: {} bytes", blobName, content.length);
        
        try {
            // Initialize services
            AzureStorageConfig storageConfig = new AzureStorageConfig();
            storageConfig.setConnectionString(System.getenv("AzureWebJobsStorage"));
            storageConfig.setContainerName("media");
            storageConfig.setThumbnailContainerName("thumbnails");
            storageConfig.setProcessedContainerName("processed");
            
            BlobStorageService blobService = new BlobStorageService(storageConfig);
            ImageProcessingService imageService = new ImageProcessingService();
            
            // Generate thumbnail (200x200)
            byte[] thumbnailData = imageService.generateThumbnail(content, 200, 200);
            blobService.uploadThumbnail(thumbnailData, blobName, "image/jpeg");
            
            log.info("Generated thumbnail for blob: {}", blobName);
            
        } catch (Exception e) {
            log.error("Error processing image: {}", e.getMessage(), e);
        }
    }
    
    @FunctionName("ProcessImageOnDemand")
    public HttpResponseMessage processImageOnDemand(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.FUNCTION,
                route = "image/process"
            ) HttpRequestMessage<Optional<ProcessingRequest>> request,
            final ExecutionContext context) {
        
        log.info("HTTP trigger function for on-demand image processing");
        
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
            ImageProcessingService imageService = new ImageProcessingService();
            
            // Download the original image
            byte[] imageData = blobService.downloadMedia(processingRequest.getBlobName());
            byte[] processedData;
            String suffix;
            
            // Process based on request type
            switch (processingRequest.getProcessingType()) {
                case THUMBNAIL:
                    int width = Integer.parseInt(processingRequest.getParameters().getOrDefault("width", "200"));
                    int height = Integer.parseInt(processingRequest.getParameters().getOrDefault("height", "200"));
                    processedData = imageService.generateThumbnail(imageData, width, height);
                    suffix = "thumb";
                    break;
                    
                case WATERMARK:
                    String watermarkText = processingRequest.getParameters().getOrDefault("text", "Copyright");
                    processedData = imageService.addWatermark(imageData, watermarkText);
                    suffix = "watermark";
                    break;
                    
                case RESIZE:
                    int resizeWidth = Integer.parseInt(processingRequest.getParameters().getOrDefault("width", "800"));
                    int resizeHeight = Integer.parseInt(processingRequest.getParameters().getOrDefault("height", "600"));
                    processedData = imageService.resizeImage(imageData, resizeWidth, resizeHeight);
                    suffix = "resize";
                    break;
                    
                case FILTER:
                    String filterType = processingRequest.getParameters().getOrDefault("type", "grayscale");
                    processedData = imageService.applyFilter(imageData, filterType);
                    suffix = "filter-" + filterType;
                    break;
                    
                case FORMAT_CONVERSION:
                    String targetFormat = processingRequest.getParameters().getOrDefault("format", "jpg");
                    processedData = imageService.convertFormat(imageData, targetFormat);
                    suffix = "convert-" + targetFormat;
                    break;
                    
                default:
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("Unsupported processing type: " + processingRequest.getProcessingType())
                            .build();
            }
            
            // Upload the processed image
            String processedBlobName = blobService.uploadProcessedMedia(
                    processedData, 
                    processingRequest.getBlobName(), 
                    suffix, 
                    "image/jpeg"
            );
            
            // Return the URL of the processed image
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
            log.error("Error processing image: {}", e.getMessage(), e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
