package com.mediaprocessing.common.config;

import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClientBuilder;
import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureAiConfig {
    private String computerVisionEndpoint;
    private String computerVisionKey;
    private String textAnalyticsEndpoint;
    private String textAnalyticsKey;
    private String formRecognizerEndpoint;
    private String formRecognizerKey;
    
    public ImageAnalysisClient createImageAnalysisClient() {
        return new ImageAnalysisClientBuilder()
                .endpoint(computerVisionEndpoint)
                .credential(new AzureKeyCredential(computerVisionKey))
                .buildClient();
    }
    
    public TextAnalyticsClient createTextAnalyticsClient() {
        return new TextAnalyticsClientBuilder()
                .endpoint(textAnalyticsEndpoint)
                .credential(new AzureKeyCredential(textAnalyticsKey))
                .buildClient();
    }
}
