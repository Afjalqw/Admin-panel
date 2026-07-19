import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

// Automatically configured based on your google-services.json values!
const firebaseConfig = {
  apiKey: "AIzaSyDb73_QSHK3O9ImnULOQP0z_pMj7k6Ns8c",
  authDomain: "pdf-a89ed.firebaseapp.com",
  projectId: "pdf-a89ed",
  storageBucket: "pdf-a89ed.firebasestorage.app",
  messagingSenderId: "439781802533",
  appId: "1:439781802533:android:93e5c610ee610c3c9cf5cc"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);

export default app;
