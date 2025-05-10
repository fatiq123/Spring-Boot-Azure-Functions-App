# Media Processing Application - User Manual

This user manual provides detailed instructions for setting up, configuring, and deploying the Media Processing Application.

## Table of Contents

1. [Local Development Setup](#local-development-setup)
2. [Azure Resources Setup](#azure-resources-setup)
3. [Configuration](#configuration)
4. [Deployment](#deployment)
5. [Usage Guide](#usage-guide)
6. [Troubleshooting](#troubleshooting)

## Local Development Setup

### Prerequisites

- Java 17 JDK
- Maven 3.8+
- Azure Functions Core Tools v4
- Azure CLI
- Azure Storage Emulator (Azurite) or Azure Storage Account
- Git

### Installation Steps

1. **Install Java 17**:
   - Download and install from [Oracle](https://www.oracle.com/java/technologies/downloads/#java17) or use OpenJDK

2. **Install Maven**:
   - Download from [Maven website](https://maven.apache.org/download.cgi)
   - Add to PATH environment variable

3. **Install Azure Functions Core Tools**:
   ```bash
   npm install -g azure-functions-core-tools@4 --unsafe-perm true
   ```

4. **Install Azure CLI**:
   ```bash
   # For Windows (PowerShell)
   Invoke-WebRequest -Uri https://aka.ms/installazurecliwindows -OutFile .\AzureCLI.msi; Start-Process msiexec.exe -Wait -ArgumentList '/I AzureCLI.msi /quiet'; rm .\AzureCLI.msi

   # For macOS
   brew update && brew install azure-cli

   # For Linux
   curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
   ```

5. **Install Azurite (for local development)**:
   ```bash
   npm install -g azurite
   ```

6. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd media-processing-app
   ```

7. **Build the project**:
   ```bash
   mvn clean install
   ```

## Azure Resources Setup

### Required Azure Resources

1. **Resource Group**
2. **Storage Account**
3. **App Service Plan**
4. **Function App**
5. **Computer Vision Service**
6. **Web App**

### Azure CLI Commands

1. **Login to Azure**:
   ```bash
   az login
   ```

2. **Set Subscription**:
   ```bash
   az account set --subscription "<subscription-id>"
   ```

3. **Create Resource Group**:
   ```bash
   az group create --name media-processing-rg --location eastus
   ```

4. **Create Storage Account**:
   ```bash
   az storage account create --name mediaprocessingstorage --resource-group media-processing-rg --location centralus --sku Standard_LRS
   ```

5. **Create Storage Containers**:
   ```bash
   # Get storage account key
#   mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA==
   STORAGE_KEY=$(az storage account keys list --resource-group media-processing-rg --account-name mediaprocessingstorage --query "[0].value" -o tsv)

   # Create containers
   az storage container create --name media --account-name mediaprocessingstorage --account-key $STORAGE_KEY
   az storage container create --name thumbnails --account-name mediaprocessingstorage --account-key $STORAGE_KEY
   az storage container create --name processed --account-name mediaprocessingstorage --account-key $STORAGE_KEY
   ```

6. **Create Storage Queue**:
   ```bash
   az storage queue create --name media-processing-queue --account-name mediaprocessingstorage --account-key $STORAGE_KEY
   ```

7. **Create Computer Vision Service**:
   ```bash
   az cognitiveservices account create --name media-processing-vision --resource-group media-processing-rg --kind ComputerVision --sku S1 --location centralus --yes
   ```

8. **Get Computer Vision Keys**:
   ```bash
   az cognitiveservices account keys list --name media-processing-vision --resource-group media-processing-rg
   ```
[//]: # ({)
[//]: # ("key1": "73d300b4d9f84c2c8b830311007df536",)
[//]: # ("key2": "dd864c98761948efa8a9a810ef66242a")
[//]: # (})



9. **Create App Service Plan**:
   ```bash
   az appservice plan create --name media-processing-plan --resource-group media-processing-rg --sku B1 --is-linux
   ```

10. **Create Function App**:
    ```bash
    az functionapp create --name media-processing-functions --storage-account mediaprocessingstorage --resource-group media-processing-rg --plan media-processing-plan --runtime java --runtime-version 17 --functions-version 4
    ```

11. **Create Web App**:
    ```bash
    az webapp create --name media-processing-web --resource-group media-processing-rg --plan media-processing-plan --runtime "JAVA:17-java17"
    ```

## Configuration

### Local Configuration

1. **Configure Azure Storage Emulator**:
   ```bash
   # Start Azurite
   azurite --silent --location <path-to-data-folder> --debug <path-to-debug.log>
   ```

2. **Configure local.settings.json**:
   Edit `media-functions/src/main/resources/local.settings.json`:
   ```json
   {
     "IsEncrypted": false,
     "Values": {
       "AzureWebJobsStorage": "UseDevelopmentStorage=true",
       "FUNCTIONS_WORKER_RUNTIME": "java",
       "ComputerVisionEndpoint": "https://your-computer-vision-resource.cognitiveservices.azure.com/",
       "ComputerVisionKey": "your-computer-vision-key",
       "TextAnalyticsEndpoint": "https://your-text-analytics-resource.cognitiveservices.azure.com/",
       "TextAnalyticsKey": "your-text-analytics-key",
       "FormRecognizerEndpoint": "https://your-form-recognizer-resource.cognitiveservices.azure.com/",
       "FormRecognizerKey": "your-form-recognizer-key"
     },
     "Host": {
       "CORS": "*",
       "CORSCredentials": false
     }
   }
   ```

3. **Configure application.properties**:
   Edit `media-web/src/main/resources/application.properties`:
   ```properties
   # Azure Storage configuration
   azure.storage.connection-string=UseDevelopmentStorage=true
   azure.storage.container-name=media
   azure.storage.thumbnail-container-name=thumbnails
   azure.storage.processed-container-name=processed

   # Azure Functions configuration
   azure.functions.base-url=http://localhost:7071

   # Azure AI configuration
   azure.ai.computer-vision.endpoint=https://your-computer-vision-resource.cognitiveservices.azure.com/
   azure.ai.computer-vision.key=your-computer-vision-key
   azure.ai.text-analytics.endpoint=https://your-text-analytics-resource.cognitiveservices.azure.com/
   azure.ai.text-analytics.key=your-text-analytics-key
   azure.ai.form-recognizer.endpoint=https://your-form-recognizer-resource.cognitiveservices.azure.com/
   azure.ai.form-recognizer.key=your-form-recognizer-key
   ```

### Azure Configuration

1. **Configure Function App Settings**:
   ```bash
   # Get storage connection string
   STORAGE_CONN=$(az storage account show-connection-string --name mediaprocessingstorage --resource-group media-processing-rg --query connectionString -o tsv)

   # Get Computer Vision key and endpoint
   73d300b4d9f84c2c8b830311007df536
   CV_KEY=$(az cognitiveservices account keys list --name media-processing-vision --resource-group media-processing-rg --query key1 -o tsv)
   
   CV_ENDPOINT=$(az cognitiveservices account show --name media-processing-vision --resource-group media-processing-rg --query properties.endpoint -o tsv)

   # Set Function App settings
   az functionapp config appsettings set --name media-processing-functions --resource-group media-processing-rg --settings \
     AzureWebJobsStorage=mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA== \
     ComputerVisionEndpoint=https://centralus.api.cognitive.microsoft.com/ \
     ComputerVisionKey=73d300b4d9f84c2c8b830311007df536
   ```

   
[//]: # (   az functionapp config appsettings set `)

[//]: # (--name media-processing-functions `)

[//]: # (--resource-group media-processing-rg `)

[//]: # (--settings `)

[//]: # ("AzureWebJobsStorage=mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA==" `)

[//]: # ("ComputerVisionEndpoint=https://centralus.api.cognitive.microsoft.com/" `)

[//]: # ("ComputerVisionKey=73d300b4d9f84c2c8b830311007df536")
   


2. **Configure Web App Settings**:
   ```bash
   # Set Web App settings
   az webapp config appsettings set --name media-processing-web --resource-group media-processing-rg --settings \
     AZURE_STORAGE_CONNECTION_STRING=$STORAGE_CONN \
     AZURE_STORAGE_CONTAINER_NAME=media \
     AZURE_STORAGE_THUMBNAIL_CONTAINER_NAME=thumbnails \
     AZURE_STORAGE_PROCESSED_CONTAINER_NAME=processed \
     AZURE_FUNCTIONS_BASE_URL=https://media-processing-functions.azurewebsites.net \
     AZURE_AI_COMPUTER_VISION_ENDPOINT=$CV_ENDPOINT \
     AZURE_AI_COMPUTER_VISION_KEY=$CV_KEY
   ```


[//]: # (az webapp config appsettings set `)

[//]: # (--name media-processing-web `)

[//]: # (--resource-group media-processing-rg `)

[//]: # (--settings `)

[//]: # ("AZURE_STORAGE_CONNECTION_STRING=mBoA/i/XrOLmqkVMihia0ZaYXE6XWy5Lps8I4EVXTxNLRpJsiOJvWKINf9GF2/rdZOslqRerZm1N+AStPisTRA==" `)

[//]: # ("AZURE_STORAGE_CONTAINER_NAME=media" `)

[//]: # ("AZURE_STORAGE_THUMBNAIL_CONTAINER_NAME=thumbnails" `)

[//]: # ("AZURE_STORAGE_PROCESSED_CONTAINER_NAME=processed" `)

[//]: # ("AZURE_FUNCTIONS_BASE_URL=https://media-processing-functions.azurewebsites.net" `)

[//]: # ("AZURE_AI_COMPUTER_VISION_ENDPOINT=https://centralus.api.cognitive.microsoft.com/" `)

[//]: # ("AZURE_AI_COMPUTER_VISION_KEY=73d300b4d9f84c2c8b830311007df536")


## Deployment

### Deploy Azure Functions

1. **Package the Functions**:
   ```bash
   cd media-functions
   mvn clean package
   ```

2. **Deploy to Azure**:
   ```bash
   mvn azure-functions:deploy
   ```

### Deploy Web Application

1. **Package the Web App**:
   ```bash
   cd media-web
   mvn clean package
   ```

2. **Deploy to Azure**:
   ```bash
   az webapp deploy --resource-group media-processing-rg --name media-processing-web --src-path target/media-web-0.0.1-SNAPSHOT.jar --type jar
   ```

## Usage Guide

### Web Application

1. **Access the Web Application**:
   - Local: http://localhost:8080
   - Azure: https://media-processing-web.azurewebsites.net

2. **Upload Media**:
   - Click on "Upload" in the navigation bar
   - Select a file to upload
   - Click "Upload" button

3. **View Media**:
   - Click on a media item in the gallery to view details
   - Use the processing options on the right side to process the media

4. **Process Media**:
   - For images: Generate thumbnails, add watermarks, resize, apply filters, convert formats
   - For videos: Extract thumbnails, add watermarks, compress, extract audio, create previews
   - For AI processing: Analyze images, detect faces, extract text, moderate content

### API Endpoints

1. **Upload Media**:
   ```
   POST /api/media/upload
   Content-Type: multipart/form-data
   Body: file=@path/to/file
   ```

2. **Get All Media**:
   ```
   GET /api/media
   ```

3. **Get Media by ID**:
   ```
   GET /api/media/{id}
   ```

4. **Process Media**:
   ```
   POST /api/media/{id}/process?type=PROCESSING_TYPE
   Content-Type: application/json
   Body: {"param1": "value1", "param2": "value2"}
   ```

## Troubleshooting

### Common Issues

1. **Azure Functions not starting**:
   - Check Java version: `java -version`
   - Verify Azure Functions Core Tools version: `func --version`
   - Check local.settings.json configuration

2. **Storage connection issues**:
   - Verify Azurite is running for local development
   - Check storage connection string in configuration
   - Verify storage account exists and is accessible

3. **AI services not working**:
   - Verify Computer Vision keys and endpoints
   - Check quota and rate limits for the service
   - Verify network connectivity to the service

4. **Deployment failures**:
   - Check Maven build output for errors
   - Verify Azure CLI is logged in with correct subscription
   - Check resource group and resource existence

### Logs

1. **View Function App logs**:
   ```bash
   az functionapp log tail --name media-processing-functions --resource-group media-processing-rg
   ```

2. **View Web App logs**:
   ```bash
   az webapp log tail --name media-processing-web --resource-group media-processing-rg
   ```

3. **Enable Application Insights**:
   ```bash
   # Create Application Insights
   az monitor app-insights component create --app media-processing-insights --location eastus --resource-group media-processing-rg --application-type web

   # Get Instrumentation Key
   APPINSIGHTS_KEY=$(az monitor app-insights component show --app media-processing-insights --resource-group media-processing-rg --query instrumentationKey -o tsv)

   # Configure Function App
   az functionapp config appsettings set --name media-processing-functions --resource-group media-processing-rg --settings APPINSIGHTS_INSTRUMENTATIONKEY=$APPINSIGHTS_KEY

   # Configure Web App
   az webapp config appsettings set --name media-processing-web --resource-group media-processing-rg --settings APPINSIGHTS_INSTRUMENTATIONKEY=$APPINSIGHTS_KEY
   ```
