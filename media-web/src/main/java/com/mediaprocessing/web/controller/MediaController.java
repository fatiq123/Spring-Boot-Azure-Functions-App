package com.mediaprocessing.web.controller;

import com.mediaprocessing.common.model.MediaItem;
import com.mediaprocessing.common.model.ProcessingRequest;
import com.mediaprocessing.web.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;
    
    @GetMapping("/")
    public String home(Model model) {
        List<MediaItem> mediaItems = mediaService.getAllMedia();
        model.addAttribute("mediaItems", mediaItems);
        return "index";
    }
    
    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }
    
    @PostMapping("/upload")
    public String uploadMedia(@RequestParam("file") MultipartFile file, Model model) {
        try {
            MediaItem mediaItem = mediaService.uploadMedia(file);
            return "redirect:/media/" + mediaItem.getId();
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to upload file: " + e.getMessage());
            return "upload";
        }
    }
    
    @GetMapping("/media/{id}")
    public String viewMedia(@PathVariable String id, Model model) {
        MediaItem mediaItem = mediaService.getMediaById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        
        model.addAttribute("media", mediaItem);
        return "media-detail";
    }
    
    @PostMapping("/api/media/upload")
    @ResponseBody
    public ResponseEntity<MediaItem> uploadMediaApi(@RequestParam("file") MultipartFile file) {
        try {
            MediaItem mediaItem = mediaService.uploadMedia(file);
            return ResponseEntity.ok(mediaItem);
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/api/media")
    @ResponseBody
    public ResponseEntity<List<MediaItem>> getAllMedia() {
        List<MediaItem> mediaItems = mediaService.getAllMedia();
        return ResponseEntity.ok(mediaItems);
    }
    
    @GetMapping("/api/media/{id}")
    @ResponseBody
    public ResponseEntity<MediaItem> getMediaById(@PathVariable String id) {
        return mediaService.getMediaById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/api/media/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteMedia(@PathVariable String id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/api/media/{id}/process")
    @ResponseBody
    public ResponseEntity<Map<String, String>> processMedia(
            @PathVariable String id,
            @RequestParam("type") String processingType,
            @RequestBody(required = false) Map<String, String> parameters) {
        
        try {
            ProcessingRequest.ProcessingType type = ProcessingRequest.ProcessingType.valueOf(processingType);
            
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            
            mediaService.processMedia(id, type, parameters);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "Processing request queued");
            response.put("mediaId", id);
            response.put("processingType", processingType);
            
            return ResponseEntity.accepted().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Error processing media: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
