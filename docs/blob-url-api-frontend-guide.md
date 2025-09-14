# Blob URL API - Frontend Developer Guide

## Overview

The Blob URL feature provides temporary download URLs for files, enabling better download progress tracking and reduced server load. Instead of streaming files directly through the API, you request a temporary URL and download from that.

## Key Benefits

- **Instant response**: URL creation is sub-second (uses hard links)
- **Progress tracking**: Full HTTP range request support for download progress
- **Resume capability**: Interrupted downloads can be resumed
- **Better UX**: Non-blocking API calls, proper download headers
- **Security**: Temporary URLs with cryptographic tokens that auto-expire

## API Endpoints

### 1. Create Blob URL

**POST** `/api/blob-urls/create`

Creates a temporary download URL for a file.

```json
// Request
{
  "filePath": "/path/to/your/file.pdf"
}

// Response (200 OK)
{
  "token": "abc123def456...",
  "downloadUrl": "https://your-domain.com/api/blob-urls/downloads/abc123def456...",
  "filename": "file.pdf",
  "fileSize": 1048576,
  "contentType": "application/pdf",
  "expiresAt": "2025-09-14T15:30:00Z",
  "createdAt": "2025-09-14T14:30:00Z"
}
```

**Error Responses:**
- `400`: Invalid file path or cross-filesystem error
- `404`: File not found
- `500`: Hard link creation failed

### 2. Check URL Status

**GET** `/api/blob-urls/{token}/status`

Check if a blob URL is still valid and get metadata.

```json
// Response (200 OK)
{
  "token": "abc123def456...",
  "downloadUrl": "https://your-domain.com/api/blob-urls/downloads/abc123def456...",
  "filename": "file.pdf",
  "fileSize": 1048576,
  "contentType": "application/pdf",
  "expiresAt": "2025-09-14T15:30:00Z",
  "createdAt": "2025-09-14T14:30:00Z"
}
```

**Error Responses:**
- `404`: Token not found or expired

### 3. Download File

**GET** `/api/blob-urls/downloads/{token}`

Download the file using the temporary URL. **No authentication required** for this endpoint.

**Headers Provided:**
- `Content-Type`: Proper MIME type
- `Content-Length`: File size
- `Content-Disposition`: Attachment with filename
- `Accept-Ranges: bytes`: Range request support

**Range Request Support:**
```http
GET /api/blob-urls/downloads/abc123def456...
Range: bytes=0-1023

HTTP/1.1 206 Partial Content
Content-Range: bytes 0-1023/1048576
Content-Length: 1024
```

## Frontend Implementation Examples

### Basic Download Flow

```javascript
// 1. Create blob URL
const createResponse = await fetch('/api/blob-urls/create', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ filePath: '/documents/report.pdf' })
});

const blobData = await createResponse.json();

// 2. Use the download URL (no auth needed)
window.location.href = blobData.downloadUrl;
// OR for programmatic download:
const downloadResponse = await fetch(blobData.downloadUrl);
const blob = await downloadResponse.blob();
```

### Download with Progress Tracking

```javascript
async function downloadWithProgress(blobUrl, onProgress) {
  const response = await fetch(blobUrl.downloadUrl);
  const contentLength = parseInt(response.headers.get('Content-Length'));
  
  const reader = response.body.getReader();
  let receivedLength = 0;
  const chunks = [];
  
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    chunks.push(value);
    receivedLength += value.length;
    
    // Report progress
    onProgress(receivedLength / contentLength);
  }
  
  return new Blob(chunks);
}
```

### Resume Interrupted Download

```javascript
async function resumeDownload(blobUrl, existingData) {
  const startByte = existingData.length;
  
  const response = await fetch(blobUrl.downloadUrl, {
    headers: {
      'Range': `bytes=${startByte}-`
    }
  });
  
  if (response.status === 206) {
    // Partial content - append to existing data
    const remainingData = await response.arrayBuffer();
    return new Blob([existingData, remainingData]);
  }
}
```

### Check URL Validity Before Download

```javascript
async function checkAndDownload(token) {
  try {
    const statusResponse = await fetch(`/api/blob-urls/${token}/status`, {
      headers: { 'Authorization': 'Bearer ' + authToken }
    });
    
    if (statusResponse.ok) {
      const blobData = await statusResponse.json();
      // URL is valid, proceed with download
      window.location.href = blobData.downloadUrl;
    } else {
      // URL expired or invalid
      console.log('Download URL has expired');
    }
  } catch (error) {
    console.error('Failed to check URL status:', error);
  }
}
```

## Important Notes

1. **Authentication**: Only blob URL creation and status checking require authentication. The actual download URL works without authentication.

2. **Expiration**: URLs expire after 1 hour by default. Always check status before attempting downloads if the URL might be old.

3. **Error Handling**: The download endpoint returns `404` for expired/invalid tokens. Handle this gracefully in your UI.

4. **File Size**: Works with files of any size. Large files benefit most from this approach.

5. **Concurrent Downloads**: Multiple clients can download from the same URL simultaneously.

6. **Security**: Tokens are cryptographically secure and not guessable. Don't log or expose them unnecessarily.

## Migration from Direct Downloads

If you're currently using `/api/files/download/**`, consider migrating to blob URLs for better performance:

```javascript
// Old approach (deprecated)
const downloadUrl = `/api/files/download/${encodeURIComponent(filePath)}`;

// New approach (recommended)
const blobResponse = await fetch('/api/blob-urls/create', {
  method: 'POST',
  headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
  body: JSON.stringify({ filePath })
});
const blobData = await blobResponse.json();
const downloadUrl = blobData.downloadUrl;
```

The blob URL approach provides better user experience with progress tracking and doesn't block your API connections during large file downloads.