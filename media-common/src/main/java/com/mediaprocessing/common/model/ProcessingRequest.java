package com.mediaprocessing.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingRequest {
    private String mediaId;
    private String blobName;
    private String containerName;
    private ProcessingType processingType;
    private Map<String, String> parameters;
    
    public enum ProcessingType {
        THUMBNAIL,
        WATERMARK,
        RESIZE,
        FILTER,
        FORMAT_CONVERSION,
        VIDEO_THUMBNAIL,
        VIDEO_WATERMARK,
        VIDEO_COMPRESS,
        AUDIO_EXTRACT,
        VIDEO_PREVIEW,
        IMAGE_ANALYSIS,
        FACE_DETECTION,
        OBJECT_RECOGNITION,
        TEXT_EXTRACTION,
        CONTENT_MODERATION
    }
}
