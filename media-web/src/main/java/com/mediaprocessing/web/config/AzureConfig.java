package com.mediaprocessing.web.config;

import com.mediaprocessing.common.config.AzureAiConfig;
import com.mediaprocessing.common.config.AzureStorageConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {

    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;
    
    @Value("${azure.storage.container-name}")
    private String containerName;
    
    @Value("${azure.storage.thumbnail-container-name}")
    private String thumbnailContainerName;
    
    @Value("${azure.storage.processed-container-name}")
    private String processedContainerName;
    
    @Value("${azure.ai.computer-vision.endpoint}")
    private String computerVisionEndpoint;
    
    @Value("${azure.ai.computer-vision.key}")
    private String computerVisionKey;
    
    @Value("${azure.ai.text-analytics.endpoint}")
    private String textAnalyticsEndpoint;
    
    @Value("${azure.ai.text-analytics.key}")
    private String textAnalyticsKey;
    
    @Value("${azure.ai.form-recognizer.endpoint:}")
    private String formRecognizerEndpoint;
    
    @Value("${azure.ai.form-recognizer.key:}")
    private String formRecognizerKey;
    
    @Bean
    public AzureStorageConfig azureStorageConfig() {
        AzureStorageConfig config = new AzureStorageConfig();
        config.setConnectionString(storageConnectionString);
        config.setContainerName(containerName);
        config.setThumbnailContainerName(thumbnailContainerName);
        config.setProcessedContainerName(processedContainerName);
        return config;
    }
    
    @Bean
    public AzureAiConfig azureAiConfig() {
        AzureAiConfig config = new AzureAiConfig();
        config.setComputerVisionEndpoint(computerVisionEndpoint);
        config.setComputerVisionKey(computerVisionKey);
        config.setTextAnalyticsEndpoint(textAnalyticsEndpoint);
        config.setTextAnalyticsKey(textAnalyticsKey);
        config.setFormRecognizerEndpoint(formRecognizerEndpoint);
        config.setFormRecognizerKey(formRecognizerKey);
        return config;
    }
}
