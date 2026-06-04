"""
Script to add video documents to Firebase Firestore

Usage:
1. Install: pip install firebase-admin
2. Go to Firebase Console > Project Settings > Service Accounts
3. Click "Generate new private key" and save the JSON file
4. Rename it to "serviceAccountKey.json" and place it in this scripts folder
5. Run: python add_videos.py
"""

import firebase_admin
from firebase_admin import credentials, firestore
import time
import os

# Get the directory where this script is located
script_dir = os.path.dirname(os.path.abspath(__file__))
service_account_path = os.path.join(script_dir, 'serviceAccountKey.json')

# Initialize Firebase Admin
cred = credentials.Certificate(service_account_path)
firebase_admin.initialize_app(cred)

db = firestore.client()

# Videos to add
videos_to_add = [
    {
        "title": "HP | Ferrari - Lewis Hamilton",
        "videoUrl": "https://drive.google.com/uc?export=view&id=1IW-JEQ57T7CUu4SlFHMoj_9ezDVW1b33"
    },
    {
        "title": "HP Envy x360",
        "videoUrl": "https://drive.google.com/uc?export=view&id=1ibjTmuvKg1Uw1J1RkVuknv7M7lQCAJhv"
    },
    {
        "title": "HP Pavilion",
        "videoUrl": "https://drive.google.com/uc?export=view&id=19DahvhTDdkMsdLxgLQjx22M01qB3X_MU"
    },
    {
        "title": "HP Pavilion 15 (2024)",
        "videoUrl": "https://drive.google.com/uc?export=view&id=1QxEUQHCJf-3Y95p45M0uOV2KK8ZLp1MM"
    },
    {
        "title": "HP Spectre",
        "videoUrl": "https://drive.google.com/uc?export=view&id=1gUgFAt1goR2wEa0WNt4mT7jyrMz2cGSg"
    }
]

def create_video_document(video):
    """Create a full video document from basic video info"""
    current_timestamp = int(time.time())
    
    return {
        "addedAt": current_timestamp,
        "categoryIds": ["demos"],
        "description": f"Watch the {video['title']} video to learn about HP's latest technology and innovations. This video showcases the features, design, and capabilities of HP products for retail professionals and technology enthusiasts.",
        "durationSec": 0,
        "language": "en",
        "published": True,
        "relatedIds": [""],
        "resources": [],
        "tags": ["training", "demos"],
        "thumbnailUrl": "",
        "title": video["title"],
        "videoUrl": video["videoUrl"],
        "viewCount": 0
    }

def title_to_doc_id(title):
    """Convert title to a valid Firestore document ID"""
    # Replace spaces with underscores, remove special characters
    doc_id = title.replace(" ", "_").replace("|", "").replace("(", "").replace(")", "")
    # Remove multiple underscores
    while "__" in doc_id:
        doc_id = doc_id.replace("__", "_")
    # Remove leading/trailing underscores
    doc_id = doc_id.strip("_")
    return doc_id

def add_videos():
    print("Starting to add videos to Firestore...\n")
    
    videos_collection = db.collection('videos')
    batch = db.batch()
    
    for video in videos_to_add:
        doc_id = title_to_doc_id(video["title"])
        doc_ref = videos_collection.document(doc_id)  # Use title as ID
        video_doc = create_video_document(video)
        
        batch.set(doc_ref, video_doc)
        print(f"Prepared: {video['title']} (ID: {doc_id})")
    
    try:
        batch.commit()
        print(f"\n✅ Successfully added all videos to Firestore!")
        print(f"Total videos added: {len(videos_to_add)}")
    except Exception as e:
        print(f"❌ Error adding videos: {e}")

if __name__ == "__main__":
    add_videos()
