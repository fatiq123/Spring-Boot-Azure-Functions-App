package com.mediaprocessing.common.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureStorageConfig {
    private String connectionString;
    private String containerName;
    private String thumbnailContainerName;
    private String processedContainerName;
    
    public BlobServiceClient createBlobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}
