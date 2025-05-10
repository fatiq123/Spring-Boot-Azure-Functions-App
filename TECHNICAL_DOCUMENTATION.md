# Media Processing Application - Technical Documentation

## Project Overview

The Media Processing Application is a comprehensive cloud-based solution for uploading, processing, and analyzing media files (images and videos) using Azure services. The application leverages Azure Functions for asynchronous processing and Azure AI services for media analysis.

## Project Architecture

The project follows a modular architecture with three main components:

1. **media-common**: A shared library containing common models, services, and utilities
2. **media-functions**: Azure Functions that handle asynchronous media processing tasks
3. **media-web**: Spring Boot web application providing the user interface and API endpoints

### Architecture Diagram

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│   media-web     │────▶│  Azure Storage  │◀────│ media-functions │
│  (Spring Boot)  │     │  (Blobs/Queues) │     │ (Azure Functions)│
│                 │     │                 │     │                 │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         └─────────────▶│  Azure AI       │◀─────────────┘
                        │  Services       │
                        │                 │
                        └─────────────────┘
```

## Technical Stack

- **Backend**: Java 17, Spring Boot 2.7
- **Cloud Services**: Azure Functions, Azure Storage, Azure AI Services
- **Frontend**: Thymeleaf, Bootstrap
- **Build Tool**: Maven
- **Containerization**: Azure App Service (Linux)

## Project Setup Requirements

### Prerequisites

1. **Java Development Kit (JDK) 17**
2. **Maven 3.8+**
3. **Azure CLI**
4. **Azure Functions Core Tools v4**
5. **Azure Subscription**
6. **IDE** (IntelliJ IDEA, Eclipse, or VS Code)

### Azure Resources Required

1. **Resource Group**: Contains all project resources
2. **Storage Account**: For storing media files and queue messages
3. **Function App**: For running the Azure Functions
4. **App Service Plan**: For hosting the Function App and Web App
5. **Computer Vision Service**: For AI-based image analysis
6. **Text Analytics Service**: For text extraction and analysis

## Setting Up Azure Resources

### 1. Create Resource Group

```bash
az group create --name media-processing-rg --location centralus
```

### 2. Create Storage Account

```bash
az storage account create --name mediaprocessingstorage1 --resource-group media-processing-rg --location centralus --sku Standard_LRS
```

### 3. Create Storage Containers

```bash
# Get storage account key
STORAGE_KEY="mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA=="

# Create containers
az storage container create --name media --account-name mediaprocessingstorage1 --account-key $STORAGE_KEY
az storage container create --name thumbnails --account-name mediaprocessingstorage1 --account-key $STORAGE_KEY
az storage container create --name processed --account-name mediaprocessingstorage1 --account-key $STORAGE_KEY
```

### 4. Create Storage Queue

```bash
az storage queue create --name media-processing-queue --account-name mediaprocessingstorage1 --account-key $STORAGE_KEY
```

### 5. Create Computer Vision Service

```bash
az cognitiveservices account create --name media-processing-vision --resource-group media-processing-rg --kind ComputerVision --sku S1 --location centralus --yes
```

### 6. Create App Service Plan

```bash
az appservice plan create --name media-processing-plan --resource-group media-processing-rg --sku B1 --is-linux
```

### 7. Create Function App

```bash
az functionapp create --name media-processing-functions --storage-account mediaprocessingstorage1 --resource-group media-processing-rg --plan media-processing-plan --runtime java --runtime-version 17 --functions-version 4
```

### 8. Configure Function App Settings

```bash
az functionapp config appsettings set --name media-processing-functions --resource-group media-processing-rg --settings "AzureWebJobsStorage=DefaultEndpointsProtocol=https;AccountName=mediaprocessingstorage1;AccountKey=mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA==;EndpointSuffix=core.windows.net"

az functionapp config appsettings set --name media-processing-functions --resource-group media-processing-rg --settings "ComputerVisionEndpoint=https://centralus.api.cognitive.microsoft.com/" "ComputerVisionKey=73d300b4d9f84c2c8b830311007df536"

az functionapp config appsettings set --name media-processing-functions --resource-group media-processing-rg --settings "TextAnalyticsEndpoint=https://centralus.api.cognitive.microsoft.com/" "TextAnalyticsKey=b75d32c94db64a7693c007f78df96d81"
```

## Project Structure Deep Dive

### media-common Module

This module contains shared code used by both the web application and Azure Functions:

- **Models**: Data structures like `MediaItem` and `ProcessingRequest`
- **Services**: Core functionality for blob storage, image processing, and video processing
- **Configuration**: Azure service configurations

Key classes:
- `BlobStorageService`: Handles uploading and downloading media from Azure Blob Storage
- `ImageProcessingService`: Contains image manipulation logic (resize, filter, watermark)
- `VideoProcessingService`: Contains video processing logic (thumbnails, compression)
- `AzureAiService`: Integrates with Azure AI services for media analysis

### media-functions Module

This module contains Azure Functions that process media asynchronously:

- **QueueProcessingFunction**: Triggered by messages in the Azure Storage Queue
- **ScheduledCleanupFunction**: Runs on a timer to clean up temporary files

The functions are triggered by events (queue messages, blob uploads, timers) and perform processing tasks without blocking the web application.

### media-web Module

This module is a Spring Boot web application that provides:

- **Web Interface**: User-friendly UI for uploading and managing media
- **REST API**: Endpoints for programmatic access
- **Media Service**: Coordinates between the UI and Azure services

Key components:
- `MediaController`: Handles HTTP requests for the web interface
- `MediaService`: Business logic for media operations
- `MediaApiController`: REST API endpoints

## Behind the Scenes: How It Works

### 1. Media Upload Process

When a user uploads a media file:

1. The `MediaService` in the web application receives the file
2. It uploads the file to Azure Blob Storage using `BlobStorageService`
3. For images, it generates a thumbnail immediately
4. For videos, it queues a thumbnail generation request
5. It creates a `MediaItem` record with metadata and URLs
6. The UI displays the uploaded media in the gallery

### 2. Media Processing Flow

When a user requests processing (e.g., applying a filter):

1. The `MediaService` creates a `ProcessingRequest` with details
2. It serializes the request to JSON and sends it to the Azure Storage Queue
3. The `QueueProcessingFunction` in Azure Functions is triggered by the new queue message
4. The function deserializes the request and identifies the processing type
5. It downloads the original media from Azure Blob Storage
6. It performs the requested processing using the appropriate service
7. It uploads the processed result back to Azure Blob Storage
8. The web application can then display the processed media

### 3. AI Analysis Flow

When AI analysis is requested:

1. The media is sent to Azure AI services (Computer Vision, Text Analytics)
2. The services return analysis results (tags, descriptions, text content)
3. The results are stored with the media item
4. The web application displays the analysis results

## Queue-Based Architecture Benefits

The application uses a queue-based architecture for several reasons:

1. **Scalability**: Processing tasks can be distributed across multiple function instances
2. **Resilience**: If processing fails, the message can be retried
3. **Asynchronous Processing**: Users don't have to wait for processing to complete
4. **Decoupling**: The web application and processing logic are separated

## Deployment Process

### 1. Deploy the Common Module

```bash
cd media-common
mvn clean install
```

### 2. Deploy the Functions Module

```bash
cd ../media-functions
mvn clean package
mvn azure-functions:deploy
```

### 3. Deploy the Web Application

```bash
cd ../media-web
mvn clean package
java -jar target/media-web-0.0.1-SNAPSHOT.jar
```

For production deployment, you would deploy the web application to Azure App Service:

```bash
az webapp deploy --resource-group media-processing-rg --name media-processing-web --src-path target/media-web-0.0.1-SNAPSHOT.jar --type jar
```

## Local Development Setup

### 1. Configure local.settings.json

For the functions project:

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "DefaultEndpointsProtocol=https;AccountName=mediaprocessingstorage1;AccountKey=mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA==;EndpointSuffix=core.windows.net",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "ComputerVisionEndpoint": "https://centralus.api.cognitive.microsoft.com/",
    "ComputerVisionKey": "73d300b4d9f84c2c8b830311007df536",
    "TextAnalyticsEndpoint": "https://centralus.api.cognitive.microsoft.com/",
    "TextAnalyticsKey": "b75d32c94db64a7693c007f78df96d81"
  }
}
```

### 2. Configure application.properties

For the web application:

```properties
# Azure Storage configuration
azure.storage.connection-string=DefaultEndpointsProtocol=https;AccountName=mediaprocessingstorage1;AccountKey=mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA==;EndpointSuffix=core.windows.net
azure.storage.container-name=media
azure.storage.thumbnail-container-name=thumbnails
azure.storage.processed-container-name=processed

# Azure Functions configuration
azure.functions.base-url=https://media-processing-functions.azurewebsites.net

# Azure AI configuration
azure.ai.computer-vision.endpoint=https://centralus.api.cognitive.microsoft.com/
azure.ai.computer-vision.key=73d300b4d9f84c2c8b830311007df536
```

### 3. Run Locally

```bash
# Run the functions
cd media-functions
mvn clean package
mvn azure-functions:run

# Run the web application (in a separate terminal)
cd ../media-web
mvn spring-boot:run
```

## Key Maven Configuration Details

The project uses several Maven plugins to facilitate building and deployment:

1. **azure-functions-maven-plugin**: Packages and deploys Azure Functions
2. **spring-boot-maven-plugin**: Packages the Spring Boot application
3. **maven-resources-plugin**: Copies configuration files to the staging directory
4. **maven-dependency-plugin**: Manages dependencies for the functions project

The `stagingDirectory` configuration in the functions project is crucial - it defines where the Azure Functions Maven plugin will look for the packaged functions.

## Troubleshooting Common Issues

### 1. "Stage directory not found" Error

This occurs when the Azure Functions Maven plugin can't find the staging directory. Solution:

1. Ensure the `stagingDirectory` property is correctly defined in pom.xml
2. Run `mvn clean package` before `mvn azure-functions:run` or `mvn azure-functions:deploy`
3. Make sure host.json and local.settings.json are in both the project root and src/main/resources

### 2. Functions Not Appearing in Azure Portal

1. Check the deployment logs for errors
2. Verify that the function classes have proper annotations (@FunctionName, @QueueTrigger, etc.)
3. Make sure the Azure Functions app settings are correctly configured

### 3. Queue Processing Not Working

1. Verify the queue exists in Azure Storage
2. Check that the connection string in local.settings.json is correct
3. Look at the function logs for any exceptions
4. Ensure the QueueProcessingFunction is using the correct queue name

## User Guide

### 1. Uploading Media

1. Navigate to the web application
2. Click "Upload" in the navigation bar
3. Select an image or video file
4. Click the "Upload" button
5. The media will appear in the gallery

### 2. Processing Media

1. Click on a media item in the gallery
2. Select a processing option from the sidebar:
   - For images: Thumbnail, Watermark, Resize, Filter, Convert
   - For videos: Extract Thumbnail, Add Watermark, Compress, Extract Audio, Create Preview
3. Configure processing parameters if needed
4. Click "Process"
5. The processing request will be queued
6. Once processing is complete, the result will appear in the media details

### 3. AI Analysis

1. Click on a media item
2. Select "AI Analysis" from the sidebar
3. Choose the type of analysis:
   - Image Analysis (tags, description)
   - Face Detection
   - Text Extraction (OCR)
   - Content Moderation
4. Click "Analyze"
5. The analysis results will appear in the media details

## Monitoring and Maintenance

### 1. Viewing Function Logs

```bash
az functionapp log tail --name media-processing-functions --resource-group media-processing-rg
```

### 2. Checking Queue Status

```bash
az storage queue show --name media-processing-queue --account-name mediaprocessingstorage1 --account-key $STORAGE_KEY
```

### 3. Viewing Storage Metrics

Monitor storage usage through the Azure Portal or:

```bash
az storage account show-usage --name mediaprocessingstorage1 --resource-group media-processing-rg
```

## Security Considerations

1. **Storage Access Keys**: Stored securely in application settings
2. **AI Service Keys**: Managed through application settings
3. **CORS Configuration**: Set to allow only specific origins
4. **Authentication**: Could be enhanced with Azure AD integration

## Future Enhancements

1. **User Authentication**: Add Azure AD B2C for user management
2. **Advanced AI Features**: Integrate more Azure AI services
3. **Mobile App**: Develop a companion mobile application
4. **Batch Processing**: Add support for processing multiple files
5. **Custom Processing Workflows**: Allow users to define processing pipelines

## Conclusion

The Media Processing Application demonstrates a modern cloud-native architecture using Azure services. By leveraging Azure Functions for asynchronous processing and Azure Storage for scalable data management, the application provides a robust platform for media processing and analysis.

The queue-based design ensures that the web application remains responsive while heavy processing tasks are handled in the background. This architecture can scale to handle large volumes of media files and processing requests.
