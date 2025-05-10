package com.mediaprocessing.web.service;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediaprocessing.common.model.MediaItem;
import com.mediaprocessing.common.model.ProcessingRequest;
import com.mediaprocessing.common.service.BlobStorageService;
import com.mediaprocessing.common.service.ImageProcessingService;
import com.mediaprocessing.common.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final BlobStorageService blobStorageService;
    private final ImageProcessingService imageProcessingService;
    private final VideoProcessingService videoProcessingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;
    
    @Value("${azure.storage.container-name}")
    private String containerName;
    
    @Value("${azure.functions.base-url}")
    private String functionsBaseUrl;
    
    // In-memory storage for media items (in a real app, this would be a database)
    private final Map<String, MediaItem> mediaItems = new ConcurrentHashMap<>();
    
    public MediaItem uploadMedia(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        byte[] fileContent = file.getBytes();
        
        // Upload to blob storage
        String blobName = blobStorageService.uploadMedia(fileContent, fileName, contentType);
        
        // Determine media type
        MediaItem.MediaType mediaType;
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                mediaType = MediaItem.MediaType.IMAGE;
            } else if (contentType.startsWith("video/")) {
                mediaType = MediaItem.MediaType.VIDEO;
            } else if (contentType.startsWith("audio/")) {
                mediaType = MediaItem.MediaType.AUDIO;
            } else {
                mediaType = MediaItem.MediaType.IMAGE; // Default to image
            }
        } else {
            mediaType = MediaItem.MediaType.IMAGE; // Default to image
        }
        
        // Generate thumbnail for images immediately
        String thumbnailUrl = null;
        if (mediaType == MediaItem.MediaType.IMAGE) {
            try {
                byte[] thumbnailData = imageProcessingService.generateThumbnail(fileContent, 200, 200);
                blobStorageService.uploadThumbnail(thumbnailData, blobName, "image/jpeg");
                thumbnailUrl = blobStorageService.getThumbnailUrl(blobName);
            } catch (Exception e) {
                log.error("Error generating thumbnail: {}", e.getMessage(), e);
            }
        }
        
        // For videos, queue thumbnail generation
        if (mediaType == MediaItem.MediaType.VIDEO) {
            queueProcessingRequest(blobName, ProcessingRequest.ProcessingType.VIDEO_THUMBNAIL);
        }
        
        // Create media item
        String id = UUID.randomUUID().toString();
        MediaItem mediaItem = MediaItem.builder()
                .id(id)
                .name(fileName)
                .originalUrl(blobStorageService.getMediaUrl(blobName))
                .thumbnailUrl(thumbnailUrl)
                .type(mediaType)
                .size(file.getSize())
                .contentType(contentType)
                .uploadedAt(LocalDateTime.now())
                .processedUrls(new ArrayList<>())
                .metadata(new HashMap<>())
                .aiAnalysis(new HashMap<>())
                .build();
        
        // Store media item
        mediaItems.put(id, mediaItem);
        
        return mediaItem;
    }
    
    public List<MediaItem> getAllMedia() {
        return new ArrayList<>(mediaItems.values());
    }
    
    public Optional<MediaItem> getMediaById(String id) {
        return Optional.ofNullable(mediaItems.get(id));
    }
    
    public void deleteMedia(String id) {
        mediaItems.remove(id);
    }
    
    public void processMedia(String id, ProcessingRequest.ProcessingType processingType, Map<String, String> parameters) {
        MediaItem mediaItem = mediaItems.get(id);
        if (mediaItem == null) {
            throw new IllegalArgumentException("Media item not found: " + id);
        }
        
        // Extract blob name from URL
        String originalUrl = mediaItem.getOriginalUrl();
        String blobName = originalUrl.substring(originalUrl.lastIndexOf('/') + 1);
        
        // Create processing request
        ProcessingRequest request = ProcessingRequest.builder()
                .mediaId(id)
                .blobName(blobName)
                .containerName(containerName)
                .processingType(processingType)
                .parameters(parameters)
                .build();
        
        log.info("Processing media: id={}, type={}, blobName={}", id, processingType, blobName);
        
        // Queue the processing request
        queueProcessingRequest(request);
    }
    
    private void queueProcessingRequest(String blobName, ProcessingRequest.ProcessingType processingType) {
        ProcessingRequest request = ProcessingRequest.builder()
                .blobName(blobName)
                .containerName(containerName)
                .processingType(processingType)
                .parameters(new HashMap<>())
                .build();
        
        queueProcessingRequest(request);
    }
    
    private void queueProcessingRequest(ProcessingRequest request) {
        try {
            // Create queue client
            QueueClient queueClient = new QueueClientBuilder()
                    .connectionString(storageConnectionString)
                    .queueName("media-processing-queue")
                    .buildClient();
            
            // Create the queue if it doesn't exist
            try {
                queueClient.createIfNotExists();
            } catch (Exception e) {
                log.warn("Queue may already exist: {}", e.getMessage());
            }
            
            // Convert request to JSON
            String messageJson = objectMapper.writeValueAsString(request);
            
            // Send message to queue
            queueClient.sendMessage(Base64.getEncoder().encodeToString(messageJson.getBytes()));
            
            log.info("Queued processing request: {}", messageJson);
        } catch (Exception e) {
            log.error("Error queueing processing request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to queue processing request", e);
        }
    }
}
