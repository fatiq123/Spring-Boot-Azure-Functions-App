package com.mediaprocessing.common.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Watermark;
import net.coobird.thumbnailator.filters.ImageFilter;
import net.coobird.thumbnailator.geometry.Positions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ImageProcessingService {

    public byte[] generateThumbnail(byte[] imageData, int width, int height) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        Thumbnails.of(new ByteArrayInputStream(imageData))
                .size(width, height)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .toOutputStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    public byte[] addWatermark(byte[] imageData, String watermarkText) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        
        // Create watermark image
        BufferedImage watermark = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermark.createGraphics();
        
        // Set font properties
        Font font = new Font("Arial", Font.BOLD, 36);
        g2d.setFont(font);
        g2d.setColor(new Color(255, 255, 255, 128)); // Semi-transparent white
        
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(watermarkText);
        int textHeight = fontMetrics.getHeight();
        
        // Draw text in center
        g2d.drawString(watermarkText, 
                (originalImage.getWidth() - textWidth) / 2, 
                (originalImage.getHeight() + textHeight) / 2);
        g2d.dispose();
        
        // Apply watermark
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(originalImage)
                .watermark(Positions.CENTER, watermark, 0.5f)
                .scale(1.0)
                .outputFormat(getImageFormat(imageData))
                .toOutputStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    public byte[] resizeImage(byte[] imageData, int width, int height) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        Thumbnails.of(new ByteArrayInputStream(imageData))
                .size(width, height)
                .keepAspectRatio(true)
                .outputFormat(getImageFormat(imageData))
                .toOutputStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    // Custom grayscale filter implementation
    private static class GrayscaleFilter implements ImageFilter {
        @Override
        public BufferedImage apply(BufferedImage img) {
            BufferedImage grayscaleImage = new BufferedImage(
                    img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d = grayscaleImage.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
            return grayscaleImage;
        }
    }
    
    public byte[] applyFilter(byte[] imageData, String filterType) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        
        switch (filterType.toLowerCase()) {
            case "grayscale":
                Thumbnails.of(originalImage)
                        .scale(1.0)
                        .outputQuality(1.0)
                        .outputFormat(getImageFormat(imageData))
                        .addFilter(new GrayscaleFilter())
                        .toOutputStream(outputStream);
                break;
            case "blur":
                Thumbnails.of(originalImage)
                        .scale(1.0)
                        .outputQuality(1.0)
                        .outputFormat(getImageFormat(imageData))
                        .watermark(Positions.CENTER, 
                                   Thumbnails.of(originalImage).scale(1.0).asBufferedImage(), 
                                   0.0f)
                        .toOutputStream(outputStream);
                break;
            case "sepia":
                // Custom sepia filter implementation
                BufferedImage sepiaImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    for (int x = 0; x < originalImage.getWidth(); x++) {
                        int rgb = originalImage.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        
                        int newRed = (int) (0.393 * r + 0.769 * g + 0.189 * b);
                        int newGreen = (int) (0.349 * r + 0.686 * g + 0.168 * b);
                        int newBlue = (int) (0.272 * r + 0.534 * g + 0.131 * b);
                        
                        newRed = Math.min(newRed, 255);
                        newGreen = Math.min(newGreen, 255);
                        newBlue = Math.min(newBlue, 255);
                        
                        sepiaImage.setRGB(x, y, (newRed << 16) | (newGreen << 8) | newBlue);
                    }
                }
                ImageIO.write(sepiaImage, getImageFormat(imageData), outputStream);
                break;
            default:
                // No filter, return original
                outputStream.write(imageData);
        }
        
        return outputStream.toByteArray();
    }
    
    public byte[] convertFormat(byte[] imageData, String targetFormat) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        Thumbnails.of(new ByteArrayInputStream(imageData))
                .scale(1.0)
                .outputFormat(targetFormat)
                .toOutputStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    private String getImageFormat(byte[] imageData) throws IOException {
        // Try to determine the image format from the image data
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            return "jpg"; // Default to jpg if format cannot be determined
        }
        
        // Check for common image signatures
        if (imageData.length >= 2) {
            if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) {
                return "jpg";
            } else if (imageData[0] == (byte) 0x89 && imageData[1] == (byte) 0x50) {
                return "png";
            } else if (imageData[0] == (byte) 0x47 && imageData[1] == (byte) 0x49) {
                return "gif";
            }
        }
        
        return "jpg"; // Default to jpg
    }
    
    public Map<String, Object> getImageMetadata(byte[] imageData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        Map<String, Object> metadata = new HashMap<>();
        
        if (image != null) {
            metadata.put("width", image.getWidth());
            metadata.put("height", image.getHeight());
            metadata.put("type", image.getType());
            metadata.put("colorModel", image.getColorModel().toString());
        }
        
        return metadata;
    }
}
