package com.mediaprocessing.web.service;

import com.mediaprocessing.common.config.AzureStorageConfig;
import com.mediaprocessing.common.service.BlobStorageService;
import com.mediaprocessing.common.service.ImageProcessingService;
import com.mediaprocessing.common.service.VideoProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaServiceConfig {

    @Bean
    public BlobStorageService blobStorageService(AzureStorageConfig azureStorageConfig) {
        return new BlobStorageService(azureStorageConfig);
    }
    
    @Bean
    public ImageProcessingService imageProcessingService() {
        return new ImageProcessingService();
    }
    
    @Bean
    public VideoProcessingService videoProcessingService() {
        return new VideoProcessingService();
    }
}
