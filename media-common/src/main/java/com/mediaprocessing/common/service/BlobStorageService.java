package com.mediaprocessing.common.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobProperties;
import com.mediaprocessing.common.config.AzureStorageConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class BlobStorageService {
    private final BlobServiceClient blobServiceClient;
    private final String containerName;
    private final String thumbnailContainerName;
    private final String processedContainerName;

    public BlobStorageService(AzureStorageConfig config) {
        this.blobServiceClient = config.createBlobServiceClient();
        this.containerName = config.getContainerName();
        this.thumbnailContainerName = config.getThumbnailContainerName();
        this.processedContainerName = config.getProcessedContainerName();
        
        // Ensure containers exist
        createContainerIfNotExists(containerName);
        createContainerIfNotExists(thumbnailContainerName);
        createContainerIfNotExists(processedContainerName);
    }
    
    private void createContainerIfNotExists(String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
            log.info("Container created: {}", containerName);
        }
    }
    
    public String uploadMedia(byte[] data, String fileName, String contentType) {
        String blobName = UUID.randomUUID().toString() + "-" + fileName;
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName);
        
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(contentType);
        
        blobClient.upload(new ByteArrayInputStream(data), data.length, true);
        blobClient.setHttpHeaders(headers);
        
        log.info("Uploaded blob: {}", blobName);
        return blobName;
    }
    
    public String uploadThumbnail(byte[] data, String originalBlobName, String contentType) {
        String thumbnailName = "thumb-" + originalBlobName;
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(thumbnailContainerName)
                .getBlobClient(thumbnailName);
        
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(contentType);
        
        blobClient.upload(new ByteArrayInputStream(data), data.length, true);
        blobClient.setHttpHeaders(headers);
        
        log.info("Uploaded thumbnail: {}", thumbnailName);
        return thumbnailName;
    }
    
    public String uploadProcessedMedia(byte[] data, String originalBlobName, String suffix, String contentType) {
        String processedName = suffix + "-" + originalBlobName;
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(processedContainerName)
                .getBlobClient(processedName);
        
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(contentType);
        
        blobClient.upload(new ByteArrayInputStream(data), data.length, true);
        blobClient.setHttpHeaders(headers);
        
        log.info("Uploaded processed media: {}", processedName);
        return processedName;
    }
    
    public byte[] downloadMedia(String blobName) {
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    public byte[] downloadThumbnail(String blobName) {
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(thumbnailContainerName)
                .getBlobClient("thumb-" + blobName);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    public byte[] downloadProcessedMedia(String blobName, String suffix) {
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(processedContainerName)
                .getBlobClient(suffix + "-" + blobName);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    public Map<String, String> getBlobMetadata(String blobName) {
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName);
        
        BlobProperties properties = blobClient.getProperties();
        return properties.getMetadata() != null ? properties.getMetadata() : new HashMap<>();
    }
    
    public void setBlobMetadata(String blobName, Map<String, String> metadata) {
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName);
        
        blobClient.setMetadata(metadata);
    }
    
    public String getMediaUrl(String blobName) {
        return blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName)
                .getBlobUrl();
    }
    
    public String getThumbnailUrl(String blobName) {
        return blobServiceClient
                .getBlobContainerClient(thumbnailContainerName)
                .getBlobClient("thumb-" + blobName)
                .getBlobUrl();
    }
    
    public String getProcessedMediaUrl(String blobName, String suffix) {
        return blobServiceClient
                .getBlobContainerClient(processedContainerName)
                .getBlobClient(suffix + "-" + blobName)
                .getBlobUrl();
    }
}
