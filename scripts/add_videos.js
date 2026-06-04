/**
 * Script to add video documents to Firebase Firestore
 * 
 * Usage:
 * 1. Go to Firebase Console > Project Settings > Service Accounts
 * 2. Click "Generate new private key" and save the JSON file
 * 3. Rename it to "serviceAccountKey.json" and place it in this scripts folder
 * 4. Run: node add_videos.js
 */

const admin = require('firebase-admin');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Videos to add
const videosToAdd = [
  {
    title: "HP | Ferrari - Lewis Hamilton",
    videoUrl: "https://drive.google.com/uc?export=view&id=1IW-JEQ57T7CUu4SlFHMoj_9ezDVW1b33"
  },
  {
    title: "HP Envy x360",
    videoUrl: "https://drive.google.com/uc?export=view&id=1ibjTmuvKg1Uw1J1RkVuknv7M7lQCAJhv"
  },
  {
    title: "HP Pavilion",
    videoUrl: "https://drive.google.com/uc?export=view&id=19DahvhTDdkMsdLxgLQjx22M01qB3X_MU"
  },
  {
    title: "HP Pavilion 15 (2024)",
    videoUrl: "https://drive.google.com/uc?export=view&id=1QxEUQHCJf-3Y95p45M0uOV2KK8ZLp1MM"
  },
  {
    title: "HP Spectre",
    videoUrl: "https://drive.google.com/uc?export=view&id=1gUgFAt1goR2wEa0WNt4mT7jyrMz2cGSg"
  }
];

// Generate thumbnail URL from video URL (extract file ID and create thumbnail URL)
function generateThumbnailUrl(videoUrl) {
  const match = videoUrl.match(/id=([a-zA-Z0-9_-]+)/);
  if (match) {
    // For Google Drive videos, we can try to use a thumbnail
    // But since these are videos, thumbnail might not be available
    // Return empty string - you can update these later in Firebase Console
    return "";
  }
  return "";
}

// Create full document from video info
function createVideoDocument(video) {
  const currentTimestamp = Math.floor(Date.now() / 1000);
  
  return {
    addedAt: currentTimestamp,
    categoryIds: ["demos"],
    description: `Watch the ${video.title} video to learn about HP's latest technology and innovations. This video showcases the features, design, and capabilities of HP products for retail professionals and technology enthusiasts.`,
    durationSec: 0,
    language: "en",
    published: true,
    relatedIds: [""],
    resources: [],
    tags: ["training", "demos"],
    thumbnailUrl: generateThumbnailUrl(video.videoUrl),
    title: video.title,
    videoUrl: video.videoUrl,
    viewCount: 0
  };
}

// Convert title to a valid Firestore document ID
function titleToDocId(title) {
  // Replace spaces with underscores, remove special characters
  let docId = title.replace(/ /g, '_').replace(/\|/g, '').replace(/\(/g, '').replace(/\)/g, '');
  // Remove multiple underscores
  while (docId.includes('__')) {
    docId = docId.replace(/__/g, '_');
  }
  // Remove leading/trailing underscores
  docId = docId.replace(/^_+|_+$/g, '');
  return docId;
}

async function addVideos() {
  console.log('Starting to add videos to Firestore...\n');
  
  const batch = db.batch();
  const videosCollection = db.collection('videos');
  
  for (const video of videosToAdd) {
    const docId = titleToDocId(video.title);
    const docRef = videosCollection.doc(docId); // Use title as ID
    const videoDoc = createVideoDocument(video);
    
    batch.set(docRef, videoDoc);
    console.log(`Prepared: ${video.title} (ID: ${docId})`);
  }
  
  try {
    await batch.commit();
    console.log('\n✅ Successfully added all videos to Firestore!');
    console.log(`Total videos added: ${videosToAdd.length}`);
  } catch (error) {
    console.error('❌ Error adding videos:', error);
  }
  
  process.exit(0);
}

addVideos();
