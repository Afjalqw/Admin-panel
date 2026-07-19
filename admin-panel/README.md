# AI PDF Studio Admin Control Panel 🚀

A highly responsive React + Tailwind CSS + Vite administration panel designed to remotely manage and sync settings, monetization campaigns (AdMob), and generative AI model states for your Android application instantly.

---

## 🎨 Visual Identity & Core Mechanics
- **Design System:** Material 3 Inspired slate-indigo, featuring live charts and dynamic state cards.
- **Dynamic Configuration Sync:** Firestore Real-time Snapshot listeners propagate settings instantaneously to active Android devices.
- **Secure Authentication:** Integrates with Firebase Auth to lock panel features.
- **Dynamic Schema Provisioning:** Features a single-button "Provision Firestore Collections" utility that auto-generates all tables and defaults in your Firestore backend on click.

---

## 🛠️ Step-by-Step Installation

### Prerequisite
Make sure you have [Node.js (LTS)](https://nodejs.org) installed on your system.

### 1. Install Dependencies
Navigate to the `/admin-panel` directory in your terminal and install packages:
```bash
cd admin-panel
npm install
```

### 2. Start Locally in Development Mode
To run the server locally with Vite (hot reloading enabled):
```bash
npm run dev
```
Open your browser and navigate to `http://localhost:3000`.

---

## 🌐 Deploy to Firebase Hosting (Production-Ready)

To host this panel globally on Firebase's lightning-fast CDN servers for free:

### 1. Install Firebase CLI Tool
If not already installed, run:
```bash
npm install -g firebase-tools
```

### 2. Log In to Firebase Console
```bash
firebase login
```

### 3. Initialize Firebase Hosting in the Folder
In the `/admin-panel` root directory, execute:
```bash
firebase init hosting
```
- Select **Use an existing project** and choose your project: `pdf-a89ed`.
- When asked: *"What do you want to use as your public directory?"*, type: **`dist`** (Vite builds into `dist`).
- When asked: *"Configure as a single-page app (rewrite all urls to /index.html)?"*, select: **`Yes`**.
- When asked: *"Set up automatic builds and deploys with GitHub?"*, select: **`No`**.

### 4. Build and Deploy
Execute the build script and deploy using:
```bash
npm run build
firebase deploy --only hosting
```

Your Admin Web Panel will be live globally! Feel free to customize security rules or login credentials directly inside the Firebase Auth console.

---

## 🔒 Security Configuration
By default, standard Firebase security rules block public database write requests. Update your Firestore Rules inside the Firebase Console to restrict writes to authenticated admins:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```
