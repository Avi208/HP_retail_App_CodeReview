# Omni Pad - Firebase Schema Reference

This document describes the Firestore database schema used by Omni Pad.

---

## Collections Overview

| Collection | Description | Access |
|------------|-------------|--------|
| `videos` | Normal video library | Read: Public, Write: Admin |
| `heroes` | Hero carousel videos | Read: Public, Write: Admin |
| `support_tickets` | User support requests | Create: Public, Read/Update/Delete: Admin |

---

## Collection: `videos`

### Document Structure

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | Yes | Video title (also used as document ID) |
| `description` | string | No | Video description |
| `videoUrl` | string | Yes | Direct video URL (MP4 or HLS) |
| `thumbnailUrl` | string | Yes | Thumbnail image URL |
| `durationSec` | number | Yes | Video duration in seconds |
| `viewCount` | number | No | Number of views (default: 0) |
| `published` | boolean | Yes | Whether video is visible (default: true) |
| `categoryIds` | array[string] | Yes | Category slugs this video belongs to |
| `language` | string | No | Primary language code (default: "en") |
| `tags` | array[string] | No | Searchable tags |

### Example Document

```json
{
  "title": "HP Spectre x360 Overview",
  "description": "Discover the premium features of HP Spectre x360 convertible laptop.",
  "videoUrl": "https://storage.googleapis.com/videos/spectre-overview.mp4",
  "thumbnailUrl": "https://storage.googleapis.com/thumbnails/spectre.jpg",
  "durationSec": 245,
  "viewCount": 15420,
  "published": true,
  "categoryIds": ["laptops", "premium", "2-in-1"],
  "language": "en",
  "tags": ["spectre", "x360", "convertible", "premium"]
}
```

### Category IDs

Standard category slugs used in the app:

| Slug | Display Name |
|------|--------------|
| `all` | All Videos |
| `laptops` | Laptops |
| `desktops` | Desktops |
| `printers` | Printers |
| `monitors` | Monitors |
| `accessories` | Accessories |
| `gaming` | Gaming |
| `business` | Business |

### Indexes

Recommended Firestore indexes:

```
Collection: videos
Fields: published (Ascending), categoryIds (Array Contains)

Collection: videos
Fields: published (Ascending), viewCount (Descending)
```

---

## Collection: `heroes`

### Document Structure

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | Yes | Hero video title |
| `videoUrl` | string | Yes | Direct video URL |
| `thumbnailUrl` | string | Yes | Thumbnail for "Up Next" section |
| `active` | boolean | Yes | Whether hero is displayed (default: true) |
| `order` | number | Yes | Display order (lower = first) |

### Example Document

```json
{
  "title": "HP OmniPad Introduction",
  "videoUrl": "https://storage.googleapis.com/videos/hero-intro.mp4",
  "thumbnailUrl": "https://storage.googleapis.com/thumbnails/hero-intro.jpg",
  "active": true,
  "order": 1
}
```

### Display Logic

1. Only `active: true` heroes are shown
2. Sorted by `order` ascending
3. Auto-advances to next video on completion
4. Loops back to first when all played

---

## Collection: `support_tickets`

### Document Structure

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | User's full name |
| `email` | string | Yes | User's email address |
| `subject` | string | Yes | Ticket subject line |
| `message` | string | Yes | Detailed description |
| `category` | string | Yes | Ticket category (see below) |
| `priority` | string | Yes | Priority level (see below) |
| `status` | string | Yes | Current status (see below) |
| `createdAt` | number | Yes | Unix timestamp (milliseconds) |
| `createdAtFormatted` | string | Yes | Human-readable date |

### Example Document

```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "subject": "Video playback stuttering on WiFi",
  "message": "When watching videos on WiFi, the playback frequently stutters and buffers. This happens on all videos. My internet speed is 50Mbps. Device: Samsung Galaxy S23.",
  "category": "PLAYBACK",
  "priority": "MEDIUM",
  "status": "OPEN",
  "createdAt": 1710345600000,
  "createdAtFormatted": "Mar 13, 2026 15:20"
}
```

### Category Values

| Value | Description |
|-------|-------------|
| `GENERAL` | General inquiry |
| `BUG` | Bug report |
| `FEATURE` | Feature request |
| `ACCOUNT` | Account-related issue |
| `PLAYBACK` | Video playback issue |
| `OTHER` | Other |

### Priority Values

| Value | Description | Color |
|-------|-------------|-------|
| `LOW` | Not urgent | Green |
| `MEDIUM` | Normal priority | Orange |
| `HIGH` | Urgent issue | Red |

### Status Values

| Value | Description | Color |
|-------|-------------|-------|
| `OPEN` | New ticket, not yet addressed | Blue |
| `IN_PROGRESS` | Being worked on | Orange |
| `RESOLVED` | Issue has been fixed | Green |
| `CLOSED` | Ticket closed | Gray |

---

## Queries Used by App

### Get Published Videos

```javascript
db.collection("videos")
  .where("published", "==", true)
  .get()
```

### Get Videos by Category

```javascript
db.collection("videos")
  .where("published", "==", true)
  .where("categoryIds", "array-contains", "laptops")
  .get()
```

### Get Active Heroes (Ordered)

```javascript
db.collection("heroes")
  .where("active", "==", true)
  .orderBy("order")
  .get()
```

### Get Support Tickets (Recent First)

```javascript
db.collection("support_tickets")
  .orderBy("createdAt", "desc")
  .get()
```

### Increment View Count

```javascript
db.collection("videos")
  .doc(videoId)
  .update({
    viewCount: firebase.firestore.FieldValue.increment(1)
  })
```

### Update Ticket Status

```javascript
db.collection("support_tickets")
  .doc(ticketId)
  .update({ status: "RESOLVED" })
```

---

## Real-time Listeners

### Videos Snapshot Listener

Used for real-time sync of video library:

```javascript
db.collection("videos")
  .where("published", "==", true)
  .onSnapshot((snapshot) => {
    snapshot.docChanges().forEach((change) => {
      if (change.type === "added") {
        // New video added - download if offline mode enabled
      }
      if (change.type === "removed") {
        // Video removed - delete local copy
      }
      if (change.type === "modified") {
        // Video updated - refresh metadata
      }
    });
  });
```

### Heroes Snapshot Listener

```javascript
db.collection("heroes")
  .where("active", "==", true)
  .orderBy("order")
  .onSnapshot((snapshot) => {
    // Update hero carousel
  });
```

---

## Security Rules

### Recommended Production Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Videos - read only for all users
    match /videos/{videoId} {
      allow read: if true;
      allow write: if request.auth != null && 
                      request.auth.token.admin == true;
    }
    
    // Heroes - read only for all users
    match /heroes/{heroId} {
      allow read: if true;
      allow write: if request.auth != null && 
                      request.auth.token.admin == true;
    }
    
    // Support tickets
    match /support_tickets/{ticketId} {
      // Anyone can create a ticket
      allow create: if request.resource.data.keys().hasAll(
        ['name', 'email', 'subject', 'message', 'category', 'priority', 'status', 'createdAt']
      );
      
      // Only admin can read, update, delete
      allow read, update, delete: if request.auth != null && 
                                     request.auth.token.admin == true;
    }
  }
}
```

---

## Data Migration Scripts

### Add New Video via Node.js

```javascript
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

async function addVideo(videoData) {
  const docId = videoData.title; // Use title as document ID
  await db.collection('videos').doc(docId).set({
    ...videoData,
    viewCount: 0,
    published: true
  });
  console.log(`Added video: ${docId}`);
}

// Usage
addVideo({
  title: "HP Pavilion Gaming Overview",
  description: "Explore the HP Pavilion Gaming laptop series.",
  videoUrl: "https://...",
  thumbnailUrl: "https://...",
  durationSec: 180,
  categoryIds: ["laptops", "gaming"],
  language: "en",
  tags: ["pavilion", "gaming", "laptop"]
});
```

### Bulk Update Published Status

```javascript
async function unpublishByCategory(categoryId) {
  const snapshot = await db.collection('videos')
    .where('categoryIds', 'array-contains', categoryId)
    .get();
  
  const batch = db.batch();
  snapshot.docs.forEach(doc => {
    batch.update(doc.ref, { published: false });
  });
  
  await batch.commit();
  console.log(`Unpublished ${snapshot.size} videos`);
}
```

---

## Backup Recommendations

1. **Enable Firestore Export** in Google Cloud Console
2. **Schedule daily exports** to Cloud Storage
3. **Retain backups** for at least 30 days
4. **Test restore process** quarterly

### Export Command

```bash
gcloud firestore export gs://your-bucket/backups/$(date +%Y%m%d)
```

---

## Monitoring

### Recommended Alerts

1. **High read rate** on `videos` collection
2. **Write failures** on any collection
3. **Security rule denials**
4. **Large document sizes** (>1MB warning)

### Firebase Console Metrics

- Document reads/writes per day
- Active connections
- Rule evaluation failures
- Storage usage
