package com.mediaprocessing.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItem {
    private String id;
    private String name;
    private String originalUrl;
    private String thumbnailUrl;
    private MediaType type;
    private long size;
    private String contentType;
    private LocalDateTime uploadedAt;
    private List<String> processedUrls;
    private Map<String, String> metadata;
    private Map<String, Object> aiAnalysis;
    
    public enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO
    }
}
