# Firebase Video Upload Scripts

Scripts to bulk add video documents to Firebase Firestore.

## Setup

### 1. Get Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** (gear icon) > **Service Accounts**
4. Click **"Generate new private key"**
5. Save the downloaded JSON file as `serviceAccountKey.json` in this `scripts` folder

### 2. Run the Script

#### Option A: Python (Recommended)

```bash
# Install firebase-admin
pip install firebase-admin

# Run the script
cd scripts
python add_videos.py
```

#### Option B: Node.js

```bash
# Install dependencies
cd scripts
npm install

# Run the script
node add_videos.js
```

## What the Script Does

- Adds 5 new video documents to the `videos` collection
- Each document includes:
  - `title` - Video title
  - `videoUrl` - Google Drive stream URL
  - `published` - Set to `true` so videos appear in the app
  - `categoryIds` - Set to `["demos"]`
  - `tags` - Set to `["training", "demos"]`
  - `addedAt` - Current timestamp
  - Other required fields with default values

## Videos Being Added

| Title | Stream URL |
|-------|------------|
| HP \| Ferrari - Lewis Hamilton | https://drive.google.com/uc?export=view&id=1IW-JEQ57T7CUu4SlFHMoj_9ezDVW1b33 |
| HP Envy x360 | https://drive.google.com/uc?export=view&id=1ibjTmuvKg1Uw1J1RkVuknv7M7lQCAJhv |
| HP Pavilion | https://drive.google.com/uc?export=view&id=19DahvhTDdkMsdLxgLQjx22M01qB3X_MU |
| HP Pavilion 15 (2024) | https://drive.google.com/uc?export=view&id=1QxEUQHCJf-3Y95p45M0uOV2KK8ZLp1MM |
| HP Spectre | https://drive.google.com/uc?export=view&id=1gUgFAt1goR2wEa0WNt4mT7jyrMz2cGSg |

## After Running

1. Open your app and pull to refresh
2. The new videos should appear in the Home and Categories screens
3. You can update thumbnails and other fields directly in Firebase Console
