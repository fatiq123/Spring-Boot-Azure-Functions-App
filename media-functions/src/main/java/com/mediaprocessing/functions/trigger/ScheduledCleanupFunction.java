package com.mediaprocessing.functions.trigger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ScheduledCleanupFunction {

    @FunctionName("CleanupTemporaryFiles")
    public void cleanupTemporaryFiles(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 0 * * *") // Run at midnight every day
            String timerInfo,
            final ExecutionContext context) {
        
        log.info("Scheduled cleanup function executed at: {}", OffsetDateTime.now());
        
        try {
            String connectionString = System.getenv("AzureWebJobsStorage");
            String tempContainerName = "temp";
            int retentionDays = 7; // Keep files for 7 days
            
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(tempContainerName);
            
            if (!containerClient.exists()) {
                log.info("Temp container does not exist. Nothing to clean up.");
                return;
            }
            
            OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(retentionDays);
            List<String> blobsToDelete = new ArrayList<>();
            
            // List all blobs in the container
            for (BlobItem blobItem : containerClient.listBlobs()) {
                if (blobItem.getProperties().getLastModified().isBefore(cutoffTime)) {
                    blobsToDelete.add(blobItem.getName());
                }
            }
            
            // Delete old blobs
            for (String blobName : blobsToDelete) {
                containerClient.getBlobClient(blobName).delete();
                log.info("Deleted old blob: {}", blobName);
            }
            
            log.info("Cleanup completed. Deleted {} old files.", blobsToDelete.size());
            
        } catch (Exception e) {
            log.error("Error during scheduled cleanup: {}", e.getMessage(), e);
        }
    }
}
