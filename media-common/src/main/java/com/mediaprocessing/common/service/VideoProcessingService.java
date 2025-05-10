package com.mediaprocessing.common.service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class VideoProcessingService {

    public byte[] extractThumbnail(byte[] videoData) throws IOException, JCodecException {
        // Create a temporary file to store the video data
        Path tempFile = Files.createTempFile("video", ".mp4");
        Files.write(tempFile, videoData);
        
        try {
            // Extract frame from the video
            Picture picture = FrameGrab.getFrameFromFile(tempFile.toFile(), 1); // Get the first frame
            BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
            
            // Convert the frame to a JPEG image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", outputStream);
            
            return outputStream.toByteArray();
        } finally {
            // Clean up the temporary file
            Files.deleteIfExists(tempFile);
        }
    }
    
    public byte[] addWatermarkToVideo(byte[] videoData, String watermarkText) throws IOException {
        Path tempInputFile = Files.createTempFile("input_video", ".mp4");
        Path tempOutputFile = Files.createTempFile("output_video", ".mp4");
        Files.write(tempInputFile, videoData);
        
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempInputFile.toFile());
            grabber.start();
            
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    tempOutputFile.toFile(),
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels()
            );
            
            // Copy codec parameters
            recorder.setVideoCodec(grabber.getVideoCodec());
            recorder.setAudioCodec(grabber.getAudioCodec());
            recorder.setFormat(grabber.getFormat());
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAspectRatio(grabber.getAspectRatio());
            recorder.setVideoBitrate(grabber.getVideoBitrate());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            
            recorder.start();
            
            Java2DFrameConverter converter = new Java2DFrameConverter();
            
            // Process each frame
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                if (frame.image != null) {
                    // Convert frame to BufferedImage
                    BufferedImage bufferedImage = converter.convert(frame);
                    
                    // Add watermark
                    Graphics2D g2d = bufferedImage.createGraphics();
                    g2d.setFont(new Font("Arial", Font.BOLD, 36));
                    g2d.setColor(new Color(255, 255, 255, 128)); // Semi-transparent white
                    
                    FontMetrics fontMetrics = g2d.getFontMetrics();
                    int textWidth = fontMetrics.stringWidth(watermarkText);
                    int textHeight = fontMetrics.getHeight();
                    
                    // Draw text in center
                    g2d.drawString(watermarkText, 
                            (bufferedImage.getWidth() - textWidth) / 2, 
                            (bufferedImage.getHeight() + textHeight) / 2);
                    g2d.dispose();
                    
                    // Convert back to Frame
                    Frame watermarkedFrame = converter.convert(bufferedImage);
                    recorder.record(watermarkedFrame);
                } else {
                    // Audio frame or other type, just record as is
                    recorder.record(frame);
                }
            }
            
            grabber.stop();
            recorder.stop();
            
            // Read the output file
            return Files.readAllBytes(tempOutputFile);
        } finally {
            // Clean up temporary files
            Files.deleteIfExists(tempInputFile);
            Files.deleteIfExists(tempOutputFile);
        }
    }
    
    public byte[] compressVideo(byte[] videoData, String quality) throws IOException {
        Path tempInputFile = Files.createTempFile("input_video", ".mp4");
        Path tempOutputFile = Files.createTempFile("output_video", ".mp4");
        Files.write(tempInputFile, videoData);
        
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempInputFile.toFile());
            grabber.start();
            
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    tempOutputFile.toFile(),
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels()
            );
            
            // Set compression parameters based on quality
            recorder.setFormat("mp4");
            recorder.setVideoCodec(28); // H.264 codec
            recorder.setAudioCodec(86018); // AAC codec
            
            int bitrate;
            switch (quality.toLowerCase()) {
                case "low":
                    bitrate = 500000; // 500 Kbps
                    break;
                case "medium":
                    bitrate = 1000000; // 1 Mbps
                    break;
                case "high":
                    bitrate = 2000000; // 2 Mbps
                    break;
                default:
                    bitrate = 1000000; // Default to medium
            }
            
            recorder.setVideoBitrate(bitrate);
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioBitrate(128000); // 128 Kbps audio
            
            recorder.start();
            
            // Process each frame
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }
            
            grabber.stop();
            recorder.stop();
            
            // Read the output file
            return Files.readAllBytes(tempOutputFile);
        } finally {
            // Clean up temporary files
            Files.deleteIfExists(tempInputFile);
            Files.deleteIfExists(tempOutputFile);
        }
    }
    
    public byte[] extractAudio(byte[] videoData) throws IOException {
        Path tempInputFile = Files.createTempFile("input_video", ".mp4");
        Path tempOutputFile = Files.createTempFile("output_audio", ".mp3");
        Files.write(tempInputFile, videoData);
        
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempInputFile.toFile());
            grabber.start();
            
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    tempOutputFile.toFile(),
                    grabber.getAudioChannels()
            );
            
            // Set audio parameters
            recorder.setFormat("mp3");
            recorder.setAudioCodec(86018); // AAC codec
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioBitrate(192000); // 192 Kbps audio
            
            recorder.start();
            
            // Process each frame
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                if (frame.samples != null) {
                    // Only record audio frames
                    recorder.record(frame);
                }
            }
            
            grabber.stop();
            recorder.stop();
            
            // Read the output file
            return Files.readAllBytes(tempOutputFile);
        } finally {
            // Clean up temporary files
            Files.deleteIfExists(tempInputFile);
            Files.deleteIfExists(tempOutputFile);
        }
    }
    
    public byte[] createPreviewClip(byte[] videoData, int durationSeconds) throws IOException {
        Path tempInputFile = Files.createTempFile("input_video", ".mp4");
        Path tempOutputFile = Files.createTempFile("output_preview", ".mp4");
        Files.write(tempInputFile, videoData);
        
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempInputFile.toFile());
            grabber.start();
            
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    tempOutputFile.toFile(),
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels()
            );
            
            // Copy codec parameters
            recorder.setVideoCodec(grabber.getVideoCodec());
            recorder.setAudioCodec(grabber.getAudioCodec());
            recorder.setFormat(grabber.getFormat());
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAspectRatio(grabber.getAspectRatio());
            recorder.setVideoBitrate(grabber.getVideoBitrate());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            
            recorder.start();
            
            // Calculate how many frames to grab based on duration
            int totalFramesToGrab = (int) (durationSeconds * grabber.getFrameRate());
            int frameCount = 0;
            
            // Process frames up to the limit
            Frame frame;
            while ((frame = grabber.grab()) != null && frameCount < totalFramesToGrab) {
                recorder.record(frame);
                frameCount++;
            }
            
            grabber.stop();
            recorder.stop();
            
            // Read the output file
            return Files.readAllBytes(tempOutputFile);
        } finally {
            // Clean up temporary files
            Files.deleteIfExists(tempInputFile);
            Files.deleteIfExists(tempOutputFile);
        }
    }
    
    public Map<String, Object> getVideoMetadata(byte[] videoData) throws IOException {
        Path tempFile = Files.createTempFile("video", ".mp4");
        Files.write(tempFile, videoData);
        
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempFile.toFile());
            grabber.start();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("width", grabber.getImageWidth());
            metadata.put("height", grabber.getImageHeight());
            metadata.put("format", grabber.getFormat());
            metadata.put("duration", grabber.getLengthInTime() / 1000000.0); // Convert to seconds
            metadata.put("frameRate", grabber.getFrameRate());
            metadata.put("videoCodec", grabber.getVideoCodecName());
            metadata.put("audioCodec", grabber.getAudioCodecName());
            metadata.put("audioChannels", grabber.getAudioChannels());
            metadata.put("sampleRate", grabber.getSampleRate());
            
            grabber.stop();
            
            return metadata;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
