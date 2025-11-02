# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Building the Application
```bash
./gradlew build -x test    # Build without tests
./gradlew build           # Build with tests
```

### Running the Application
```bash
./gradlew bootRun         # Run in foreground
./gradlew bootRun > app.log 2>&1 &  # Run in background with logging
```

### Testing and Development
```bash
./gradlew test            # Run all tests
pkill -f "gradle.*bootRun"  # Stop background application
```

## Architecture Overview

This is a Spring Boot application for missing person case management with web crawling capabilities.

### Core Domain Structure

**Missing Case Management**
- `MissingCase` - Main entity for missing person cases with details like person info, location, physical description
- `CaseFile` - Images and files associated with cases (supports S3 storage)
- `CaseContact` - Police contact information for cases

**Blog Crawling System**
- `BlogPost` - Stores crawled blog post metadata (title, URL, logNo)
- Selenium-based crawling for dynamic content from Naver blogs
- Image extraction and S3 upload pipeline for missing person photos

### Key Service Layers

**Crawling Services**
- `SeleniumBlogCrawlingService` - Handles pagination and dynamic content loading
- `BlogImageExtractorService` - Extracts images from individual blog posts using Selenium
- `BlogImageProcessingService` - Orchestrates image extraction and S3 upload

**Storage Services**
- `S3ImageUploadService` - Downloads images and uploads to AWS S3
- `BlogPostService` - Manages blog post data persistence

### Database Configuration

The application uses MySQL with JPA/Hibernate. Key configuration:
- Database URL: `jdbc:mysql://k13a706.p.ssafy.io:3306/topoom-db`
- Schema auto-update enabled (`ddl-auto: update`)
- Connection pool: HikariCP

### AWS Integration

AWS S3 is configured for image storage:
- Bucket: `topoom-missing-cases`
- Region: `ap-northeast-2`
- Images stored under: `missing-cases/{caseId}/images/`

### Key API Endpoints

Blog crawling operations:
- `POST /api/blog-crawl/safe182-missing-selenium/save` - Crawl and save missing person blog posts
- `POST /api/blog-crawl/extract-images?postUrl={url}` - Extract images from specific blog post
- `POST /api/blog-crawl/extract-and-upload-images?postUrl={url}&caseId={id}` - Extract and upload to S3

### Important Technical Details

**Selenium Configuration**
- Chrome headless mode enabled
- Custom User-Agent for Naver blog compatibility
- Handles dynamic content loading and pagination
- WebDriverManager for automatic driver management

**Image Processing**
- Supports JPEG, PNG, GIF, BMP, WebP formats
- Content type detection via byte signature analysis
- SHA-256 checksums for integrity verification
- Dimension extraction and metadata storage

**CaseFile Entity Structure**
- `caseId` can be null (decoupled from MissingCase for flexible usage)
- Enum types: `IoRole` (INPUT/OUTPUT), `Purpose` (BEFORE/APPEARANCE/FACE/etc), `ContentKind` (IMAGE/JSON)
- Stores S3 metadata: bucket, key, content type, size, dimensions, checksum

### Common Development Patterns

When adding new crawling features:
1. Extend Selenium services for content extraction
2. Create appropriate DTOs for data transfer
3. Add S3 upload integration if files are involved
4. Include comprehensive logging for debugging

When working with entities:
- CaseFile can exist without MissingCase (use caseId field)
- Always include proper error handling for external services
- Use Builder pattern for entity creation