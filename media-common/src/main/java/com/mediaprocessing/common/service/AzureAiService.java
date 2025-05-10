package com.mediaprocessing.common.service;

import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.models.*;
import com.azure.core.util.BinaryData;
import com.mediaprocessing.common.config.AzureAiConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@Slf4j
public class AzureAiService {
    private final ImageAnalysisClient imageAnalysisClient;
    
    public AzureAiService(AzureAiConfig config) {
        this.imageAnalysisClient = config.createImageAnalysisClient();
    }
    
    public Map<String, Object> analyzeImage(byte[] imageData) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Convert byte array to BinaryData
            BinaryData binaryData = BinaryData.fromBytes(imageData);
            
            // Analyze tags
            ImageAnalysisResult tagsResult = imageAnalysisClient.analyze(
                    binaryData,
                    Arrays.asList(VisualFeatures.TAGS),
                    null);
                    
            // Analyze objects
            ImageAnalysisResult objectsResult = imageAnalysisClient.analyze(
                    binaryData,
                    Arrays.asList(VisualFeatures.OBJECTS),
                    null);
                    
            // Analyze caption
            ImageAnalysisResult captionResult = imageAnalysisClient.analyze(
                    binaryData,
                    Arrays.asList(VisualFeatures.CAPTION),
                    null);
            
            // Extract tags
            if (tagsResult.getTags() != null) {
                results.put("tags", tagsResult.getTags());
            }
            
            // Extract caption
            if (captionResult.getCaption() != null) {
                results.put("description", captionResult.getCaption());
            }
            
            // Extract objects
            if (objectsResult.getObjects() != null) {
                results.put("objects", objectsResult.getObjects());
            }
            
        } catch (Exception e) {
            log.error("Error analyzing image with Azure AI", e);
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    public Map<String, Object> detectFaces(byte[] imageData) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Convert byte array to BinaryData
            BinaryData binaryData = BinaryData.fromBytes(imageData);
            
            ImageAnalysisResult peopleResult = imageAnalysisClient.analyze(
                    binaryData,
                    Arrays.asList(VisualFeatures.PEOPLE),
                    null);
            
            if (peopleResult.getPeople() != null) {
                results.put("faces", peopleResult.getPeople());
            }
            
        } catch (Exception e) {
            log.error("Error detecting faces with Azure AI", e);
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    public Map<String, Object> recognizeText(byte[] imageData) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Convert byte array to BinaryData
            BinaryData binaryData = BinaryData.fromBytes(imageData);
            
            ImageAnalysisResult readResult = imageAnalysisClient.analyze(
                    binaryData,
                    Arrays.asList(VisualFeatures.READ),
                    null);
            
            if (readResult.getRead() != null) {
                results.put("text", readResult.getRead());
            }
            
        } catch (Exception e) {
            log.error("Error recognizing text with Azure AI", e);
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    public Map<String, Object> moderateContent(byte[] imageData) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Note: Content moderation is now a separate service in Azure AI
            results.put("message", "Content moderation requires Azure Content Moderator service");
            
        } catch (Exception e) {
            log.error("Error moderating content with Azure AI", e);
            results.put("error", e.getMessage());
        }
        
        return results;
    }
}
