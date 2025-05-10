# Media Processing Application

A comprehensive media processing application built with Java 17, Spring Boot, and Azure Functions. This application allows users to upload, process, and analyze media files (images and videos) using Azure AI services.

## Project Structure

The project is organized into three main modules:

1. **media-common**: Common library shared between web and functions modules
2. **media-functions**: Azure Functions for media processing
3. **media-web**: Spring Boot web application with Thymeleaf frontend

## Features

### Image Processing
- Upload and store images
- Generate thumbnails automatically
- Add watermarks to images
- Resize images to different dimensions
- Apply basic filters (grayscale, blur, sepia)
- Convert images between formats

### Video Processing
- Upload and store videos
- Extract thumbnails from videos
- Add watermarks to videos
- Compress videos to reduce size
- Extract audio from videos
- Create short preview clips

### User Interface
- Responsive and intuitive interface built with Bootstrap
- Display media in a gallery view
- Provide previews for images and videos
- Allow batch uploads of multiple files
- Enable downloading of processed media
- Include search and filter options

### Azure Functions
- Blob triggers for automatic processing
- Queue triggers for batch tasks
- HTTP triggers for on-demand processing
- Timer triggers for scheduled tasks

### Azure AI Integration
- Image analysis (tags, descriptions)
- Face detection
- Object recognition
- Text extraction (OCR)
- Content moderation

## Prerequisites

- Java 17
- Maven
- Azure Functions Core Tools
- Azure CLI
- Azure Storage Emulator or Azure Storage Account
- Azure Cognitive Services (for AI features)

## Getting Started

### Local Development Setup

1. Clone the repository
2. Configure Azure Storage connection string in:
   - `media-functions/src/main/resources/local.settings.json`
   - `media-web/src/main/resources/application.properties`
3. Configure Azure AI services in:
   - `media-functions/src/main/resources/local.settings.json`
   - `media-web/src/main/resources/application.properties`
4. Build the project in the correct order:
   ```
   # On Windows
   build-all.bat
   
   # OR build modules individually
   build-common.bat
   build-functions.bat
   
   # On Linux/macOS
   ./build.sh
   ```
5. Run the web application:
   ```
   cd media-web
   mvn spring-boot:run
   ```
6. Run the Azure Functions:
   ```
   cd media-functions
   mvn azure-functions:run
   ```

### Deployment to Azure

See the User Manual for detailed deployment instructions.

## Architecture

The application follows a modular architecture:

- **Web Application**: Handles user interface, file uploads, and API endpoints
- **Azure Functions**: Processes media files asynchronously
- **Azure Storage**: Stores original and processed media files
- **Azure Queue Storage**: Manages processing requests
- **Azure Cognitive Services**: Provides AI capabilities

## License

This project is licensed under the MIT License - see the LICENSE file for details.
