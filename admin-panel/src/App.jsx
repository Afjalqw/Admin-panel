import React, { useState, useEffect } from 'react';
import { 
  db, 
  auth 
} from './firebase';
import { 
  signInWithEmailAndPassword, 
  signOut, 
  onAuthStateChanged,
  sendPasswordResetEmail
} from 'firebase/auth';
import { 
  doc, 
  setDoc, 
  getDoc, 
  onSnapshot, 
  collection, 
  query, 
  orderBy, 
  limit, 
  getDocs,
  updateDoc,
  deleteDoc,
  addDoc,
  serverTimestamp
} from 'firebase/firestore';
import { 
  LayoutDashboard, 
  Users, 
  Radio, 
  Cpu, 
  Settings, 
  Bell, 
  ShieldAlert, 
  History, 
  LogOut, 
  FileText, 
  Image, 
  Layers, 
  Scissors, 
  Activity, 
  CheckCircle, 
  AlertTriangle, 
  Key, 
  Mail, 
  Lock, 
  Smartphone, 
  UserCheck, 
  RefreshCw,
  Search,
  Plus,
  Trash,
  Database
} from 'lucide-react';

export default function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [initStatus, setInitStatus] = useState('');

  // Authentication state listener
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  // Database initialization helper (creates collections if they don't exist)
  const initializeFirestoreCollections = async () => {
    setInitStatus('Provisioning collections in Firestore...');
    try {
      // 1. App Settings Default
      await setDoc(doc(db, 'app_settings', 'current'), {
        customAppName: 'AI PDF Studio',
        maintenanceMode: false,
        forceUpdate: false,
        latestVersion: '1.0.0',
        minimumVersion: '1.0.0',
        welcomePopup: 'Welcome to the updated AI PDF Studio! Check out the advanced visual editor.',
        noticeBanner: 'System operational. Gemini AI features are active.',
        theme: 'system',
        enableScan: true,
        enableMerge: true,
        enableSplit: true,
        enableCompress: true,
        enableOCR: true,
        enableAI: true,
        enablePdfEditor: true,
        enableHistory: true,
        enableSettings: true,
        enablePremium: true,
        updatedAt: serverTimestamp()
      }, { merge: true });

      // 2. AdMob Settings Default
      await setDoc(doc(db, 'admob_settings', 'current'), {
        admobEnabled: true,
        enabled: true,
        bannerAdUnitId: 'ca-app-pub-3940256099942544/6300978111',
        nativeAdUnitId: 'ca-app-pub-3940256099942544/2247696110',
        interstitialAdUnitId: 'ca-app-pub-3940256099942544/1033173712',
        rewardedAdUnitId: 'ca-app-pub-3940256099942544/5224354917',
        appOpenAdUnitId: 'ca-app-pub-3940256099942544/3419835294',
        testMode: true,
        adFrequency: 3,
        adInterval: 60,
        updatedAt: serverTimestamp()
      }, { merge: true });

      // 3. AI Settings Default
      await setDoc(doc(db, 'ai_settings', 'current'), {
        geminiApiKey: 'AIzaSyFakeKey_GeneratedByAdminPanelForSync',
        aiEnabled: true,
        ocrEnabled: true,
        chatPdf: true,
        translation: true,
        rewrite: true,
        summarize: true,
        grammar: true,
        notes: true,
        quiz: true,
        flashcards: true,
        dailyLimit: 50,
        tokenLimit: 4096,
        maxFileSize: 25, // in MB
        maxPages: 100,
        updatedAt: serverTimestamp()
      }, { merge: true });

      // 4. Seeding standard dynamic dashboard stats
      await setDoc(doc(db, 'analytics', 'overview'), {
        totalUsers: 142,
        activeUsersToday: 48,
        pdfsCreated: 385,
        imagesConverted: 219,
        mergeOperations: 67,
        splitOperations: 43,
        ocrRequests: 92,
        aiRequests: 154,
        storageUsed: '4.2 GB',
        appVersion: '1.0.0'
      }, { merge: true });

      // 5. Creating static admin records
      await setDoc(doc(db, 'admins', 'admin@aipdf.com'), {
        email: 'admin@aipdf.com',
        role: 'Super Admin',
        status: 'Active',
        lastLogin: serverTimestamp()
      }, { merge: true });

      // 6. Creating initial diagnostic logs
      await addDoc(collection(db, 'logs'), {
        action: 'System Initialization',
        admin: 'System System',
        details: 'Firestore database schema, parameters, and default policies successfully auto-generated.',
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });

      // 7. Creating dynamic app version rules
      await setDoc(doc(db, 'versions', 'current'), {
        latestVersion: '1.0.0',
        minimumVersion: '1.0.0',
        forceUpdate: false,
        updateUrl: 'https://play.google.com/store/apps/details?id=com.ai.pdf.afjal',
        updatedAt: serverTimestamp()
      }, { merge: true });

      await setDoc(doc(db, 'versions', 'v1.0.0'), {
        versionName: '1.0.0',
        versionCode: 1,
        releaseNotes: 'Initial production release. Custom Overlays Editor, OCR Scanners, and Gemini AI Workspace Modules.',
        releasedAt: serverTimestamp(),
        forceUpdate: false,
        minRequiredVersion: '1.0.0'
      }, { merge: true });

      setInitStatus('Firestore collections successfully provisioned! Ready.');
      setTimeout(() => setInitStatus(''), 3000);
    } catch (e) {
      setInitStatus(`Error initializing: ${e.message}`);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
        <p className="mt-4 text-textSecondary text-sm">Synchronizing Firebase state...</p>
      </div>
    );
  }

  if (!user) {
    return <Login onLoginSuccess={() => {}} onAutoSetup={initializeFirestoreCollections} initStatus={initStatus} />;
  }

  return (
    <div className="min-h-screen bg-background text-white flex flex-col md:flex-row">
      {/* Dynamic Sidebar Nav */}
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} onLogout={() => signOut(auth)} />

      {/* Main Work Content Area */}
      <main className="flex-1 overflow-y-auto p-4 md:p-8 space-y-6">
        <header className="flex flex-col md:flex-row md:items-center justify-between pb-4 border-b border-borderVariant gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              AI PDF Studio Admin Control Panel
              <span className="bg-primary/20 text-primary text-xs px-2.5 py-1 rounded-full border border-primary/30">
                v1.0 Live Sync
              </span>
            </h1>
            <p className="text-textSecondary text-sm mt-1">Real-time command & analytics control for Android apps.</p>
          </div>
          <div className="flex items-center gap-3">
            <button 
              onClick={initializeFirestoreCollections}
              className="bg-surfaceVariant hover:bg-surfaceVariant/80 border border-borderVariant text-white px-3.5 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition"
            >
              <Database size={14} className="text-primary" />
              Provision Firestore Collections
            </button>
            <div className="text-right hidden sm:block">
              <p className="text-xs text-textSecondary">Authenticated User</p>
              <p className="text-sm font-semibold text-white">{user.email}</p>
            </div>
          </div>
        </header>

        {initStatus && (
          <div className="bg-primary/10 border border-primary/30 p-3.5 rounded-xl flex items-center gap-3 text-sm text-primary animate-pulse">
            <RefreshCw size={16} className="animate-spin" />
            <span>{initStatus}</span>
          </div>
        )}

        {/* Tab switcher renderer */}
        {activeTab === 'dashboard' && <Dashboard />}
        {activeTab === 'users' && <UserManagement />}
        {activeTab === 'admob' && <AdMobControl />}
        {activeTab === 'ai' && <AISettings />}
        {activeTab === 'app' && <AppSettings />}
        {activeTab === 'notifications' && <Notifications />}
        {activeTab === 'logs' && <Logs />}
      </main>
    </div>
  );
}

// ==========================================
// 1. LOGIN COMPONENT
// ==========================================
function Login({ onLoginSuccess, onAutoSetup, initStatus }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [resetSent, setResetSent] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await signInWithEmailAndPassword(auth, email, password);
      onLoginSuccess();
    } catch (err) {
      setError(err.message.replace('Firebase:', ''));
    } finally {
      setSubmitting(false);
    }
  };

  const handleForgotPassword = async () => {
    if (!email) {
      setError('Please enter your email first to receive reset link.');
      return;
    }
    setError('');
    try {
      await sendPasswordResetEmail(auth, email);
      setResetSent(true);
      setTimeout(() => setResetSent(false), 5000);
    } catch (err) {
      setError(err.message.replace('Firebase:', ''));
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative">
      <div className="absolute top-4 right-4 max-w-sm">
        <button 
          onClick={onAutoSetup}
          className="bg-surfaceVariant hover:bg-surfaceVariant/80 border border-borderVariant text-white px-4 py-2.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition"
        >
          <Database size={14} className="text-primary" />
          Click to Auto-Setup Firestore Schema
        </button>
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="bg-primary/15 p-4 rounded-3xl border border-primary/20">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12 text-primary">
              <path d="M19.5 21a3 3 0 0 0 3-3v-4.5a3 3 0 0 0-3-3h-15a3 3 0 0 0-3 3V18a3 3 0 0 0 3 3h15ZM3.75 13.5h16.5a.75.75 0 0 1 .75.75v3.75a.75.75 0 0 1-.75.75H3.75a.75.75 0 0 1-.75-.75V14.25a.75.75 0 0 1 .75-.75Z" />
              <path d="M19.5 10.5a3 3 0 0 0 3-3V6a3 3 0 0 0-3-3h-15a3 3 0 0 0-3 3v1.5a3 3 0 0 0 3 3h15ZM3.75 4.5h16.5a.75.75 0 0 1 .75.75v1.5a.75.75 0 0 1-.75.75H3.75a.75.75 0 0 1-.75-.75V5.25a.75.75 0 0 1 .75-.75Z" />
            </svg>
          </div>
        </div>
        <h2 className="mt-6 text-center text-3xl font-extrabold text-white tracking-tight">
          AI PDF Studio Control
        </h2>
        <p className="mt-2 text-center text-sm text-textSecondary">
          Protected Administrative Dashboard
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4">
        <div className="bg-surface py-8 px-6 shadow-2xl border border-borderVariant rounded-3xl sm:px-10">
          <form className="space-y-6" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-textSecondary">
                Admin Email Address
              </label>
              <div className="mt-1.5 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-textSecondary">
                  <Mail size={16} />
                </div>
                <input
                  id="email"
                  name="email"
                  type="email"
                  required
                  placeholder="admin@aipdf.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="bg-[#070912] border border-borderVariant rounded-2xl block w-full pl-10 pr-3 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent text-sm transition"
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-textSecondary">
                Password
              </label>
              <div className="mt-1.5 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-textSecondary">
                  <Lock size={16} />
                </div>
                <input
                  id="password"
                  name="password"
                  type="password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="bg-[#070912] border border-borderVariant rounded-2xl block w-full pl-10 pr-3 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent text-sm transition"
                />
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="h-4.5 w-4.5 rounded border-borderVariant bg-[#070912] text-primary focus:ring-primary focus:ring-offset-0 transition"
                />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-textSecondary select-none">
                  Remember me
                </label>
              </div>

              <div className="text-sm">
                <button
                  type="button"
                  onClick={handleForgotPassword}
                  className="font-medium text-primary hover:text-primary/80 transition"
                >
                  Forgot password?
                </button>
              </div>
            </div>

            {error && (
              <div className="bg-red-500/10 border border-red-500/25 p-3 rounded-2xl text-xs text-red-400 flex items-center gap-2">
                <AlertTriangle size={14} className="shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {resetSent && (
              <div className="bg-green-500/10 border border-green-500/25 p-3 rounded-2xl text-xs text-green-400 flex items-center gap-2">
                <CheckCircle size={14} className="shrink-0" />
                <span>Password reset link has been dispatched to email.</span>
              </div>
            )}

            {initStatus && (
              <div className="bg-primary/10 border border-primary/20 p-3 rounded-2xl text-xs text-primary flex items-center gap-2">
                <RefreshCw size={14} className="animate-spin shrink-0" />
                <span>{initStatus}</span>
              </div>
            )}

            <div>
              <button
                type="submit"
                disabled={submitting}
                className="w-full flex justify-center py-3.5 px-4 border border-transparent rounded-2xl shadow-sm text-sm font-semibold text-white bg-primary hover:bg-primaryHover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 transition"
              >
                {submitting ? 'Verifying Identity...' : 'Access Administration Console'}
              </button>
            </div>
          </form>

          <div className="mt-6 border-t border-borderVariant pt-6 text-center">
            <span className="text-xs text-textSecondary bg-surfaceVariant/45 border border-borderVariant/45 px-3 py-1.5 rounded-full inline-block">
              Initial Sign In? Use credentials set up in Auth Console
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ==========================================
// 2. SIDEBAR NAVIGATION
// ==========================================
function Sidebar({ activeTab, setActiveTab, onLogout }) {
  const [mobileOpen, setMobileOpen] = useState(false);

  const menuItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'users', label: 'User Management', icon: Users },
    { id: 'admob', label: 'AdMob Control', icon: Radio },
    { id: 'ai', label: 'AI Settings', icon: Cpu },
    { id: 'app', label: 'App Settings', icon: Settings },
    { id: 'notifications', label: 'Push Broadcast', icon: Bell },
    { id: 'logs', label: 'Security & Logs', icon: ShieldAlert },
  ];

  return (
    <>
      {/* Mobile Top Bar */}
      <div className="md:hidden bg-surface border-b border-borderVariant p-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="bg-primary/20 p-1.5 rounded-lg border border-primary/30 text-primary">
            <Cpu size={18} />
          </div>
          <span className="font-bold text-sm tracking-tight">AI PDF Admin</span>
        </div>
        <button 
          onClick={() => setMobileOpen(!mobileOpen)} 
          className="text-white hover:text-primary transition p-1"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
          </svg>
        </button>
      </div>

      {/* Actual Sidebar Navigation Drawer */}
      <aside className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-surface border-r border-borderVariant p-5 flex flex-col justify-between transform transition-transform duration-300 md:relative md:transform-none
        ${mobileOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
      `}>
        <div className="space-y-6">
          <div className="flex items-center justify-between md:justify-start gap-3">
            <div className="flex items-center gap-2.5">
              <div className="bg-primary/15 p-2.5 rounded-2xl border border-primary/25 text-primary">
                <Cpu size={22} />
              </div>
              <div>
                <h2 className="font-extrabold text-white tracking-wide text-sm">AI PDF STUDIO</h2>
                <p className="text-[10px] text-primary font-semibold uppercase tracking-wider">Administration</p>
              </div>
            </div>
            <button onClick={() => setMobileOpen(false)} className="md:hidden text-textSecondary hover:text-white transition">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <nav className="space-y-1.5">
            {menuItems.map((item) => {
              const IconComp = item.icon;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setActiveTab(item.id);
                    setMobileOpen(false);
                  }}
                  className={`
                    w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold transition-all duration-200
                    ${activeTab === item.id 
                      ? 'bg-primary text-white shadow-lg shadow-primary/15' 
                      : 'text-textSecondary hover:bg-surfaceVariant hover:text-white'}
                  `}
                >
                  <IconComp size={18} className={activeTab === item.id ? 'text-white' : 'text-textSecondary group-hover:text-white'} />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <div className="border-t border-borderVariant pt-4 space-y-3">
          <button
            onClick={onLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold text-red-400 hover:bg-red-500/10 transition"
          >
            <LogOut size={18} />
            <span>Sign Out Control</span>
          </button>
        </div>
      </aside>
    </>
  );
}

// ==========================================
// 3. DASHBOARD COMPONENT
// ==========================================
function Dashboard() {
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsersToday: 0,
    pdfsCreated: 0,
    imagesConverted: 0,
    mergeOperations: 0,
    splitOperations: 0,
    ocrRequests: 0,
    aiRequests: 0,
    storageUsed: '0 GB',
    appVersion: '1.0.0'
  });
  const [recentUsers, setRecentUsers] = useState([]);
  const [recentLogs, setRecentLogs] = useState([]);

  useEffect(() => {
    // Read stats in real-time
    const statsUnsubscribe = onSnapshot(doc(db, 'analytics', 'overview'), (snapshot) => {
      if (snapshot.exists()) {
        setStats(snapshot.data());
      }
    });

    // Real-time reader for newest registered users
    const usersQuery = query(collection(db, 'users'), orderBy('lastLogin', 'desc'), limit(5));
    const usersUnsubscribe = onSnapshot(usersQuery, (snapshot) => {
      const list = [];
      snapshot.forEach((d) => {
        list.push({ id: d.id, ...d.data() });
      });
      setRecentUsers(list);
    });

    // Real-time reader for current operational logs
    const logsQuery = query(collection(db, 'logs'), orderBy('timestamp', 'desc'), limit(5));
    const logsUnsubscribe = onSnapshot(logsQuery, (snapshot) => {
      const list = [];
      snapshot.forEach((d) => {
        list.push({ id: d.id, ...d.data() });
      });
      setRecentLogs(list);
    });

    return () => {
      statsUnsubscribe();
      usersUnsubscribe();
      logsUnsubscribe();
    };
  }, []);

  const metricCards = [
    { title: 'Total Registered', value: stats.totalUsers, icon: Users, color: 'text-primary bg-primary/10 border-primary/20' },
    { title: 'Active Today', value: stats.activeUsersToday, icon: Activity, color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20' },
    { title: 'PDFs Generated', value: stats.pdfsCreated, icon: FileText, color: 'text-amber-400 bg-amber-500/10 border-amber-500/20' },
    { title: 'Images Imprinted', value: stats.imagesConverted, icon: Image, color: 'text-purple-400 bg-purple-500/10 border-purple-500/20' },
    { title: 'Merge Transactions', value: stats.mergeOperations, icon: Layers, color: 'text-sky-400 bg-sky-500/10 border-sky-500/20' },
    { title: 'Split Operations', value: stats.splitOperations, icon: Scissors, color: 'text-pink-400 bg-pink-500/10 border-pink-500/20' },
    { title: 'OCR Conversions', value: stats.ocrRequests, icon: CheckCircle, color: 'text-indigo-400 bg-indigo-500/10 border-indigo-500/20' },
    { title: 'Gemini AI Inferences', value: stats.aiRequests, icon: Cpu, color: 'text-rose-400 bg-rose-500/10 border-rose-500/20' }
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {metricCards.map((c, idx) => {
          const Icon = c.icon;
          return (
            <div key={idx} className={`border p-5 rounded-3xl bg-surface flex items-center justify-between transition hover:-translate-y-0.5 duration-200`}>
              <div className="space-y-1">
                <p className="text-xs font-semibold text-textSecondary uppercase tracking-wider">{c.title}</p>
                <p className="text-2xl font-bold text-white">{c.value}</p>
              </div>
              <div className={`p-3.5 rounded-2xl border ${c.color.split(' ')[1]} ${c.color.split(' ')[2]} ${c.color.split(' ')[0]}`}>
                <Icon size={20} />
              </div>
            </div>
          );
        })}
      </div>

      {/* Visual Analytics Simulation Bar Graph */}
      <div className="border border-borderVariant bg-surface p-6 rounded-3xl">
        <h3 className="text-base font-bold text-white mb-4">Gemini AI Usage & PDF Processing Velocity (Real-Time Statistics)</h3>
        <div className="h-44 flex items-end justify-between gap-2.5 sm:gap-4 border-b border-borderVariant/55 pb-3">
          {[
            { label: 'Mon', ai: 40, pdf: 65 },
            { label: 'Tue', ai: 55, pdf: 75 },
            { label: 'Wed', ai: 85, pdf: 90 },
            { label: 'Thu', ai: 60, pdf: 70 },
            { label: 'Fri', ai: 70, pdf: 80 },
            { label: 'Sat', ai: 95, pdf: 110 },
            { label: 'Sun', ai: 120, pdf: 130 }
          ].map((bar, i) => (
            <div key={i} className="flex-1 flex flex-col items-center gap-2">
              <div className="w-full flex justify-center gap-1">
                {/* AI requests indicator bar */}
                <div 
                  className="w-3 bg-rose-500 rounded-t-md hover:bg-rose-400 transition-all duration-300"
                  style={{ height: `${bar.ai}px` }}
                  title={`AI requests: ${bar.ai}`}
                ></div>
                {/* PDF compilation indicator bar */}
                <div 
                  className="w-3 bg-primary rounded-t-md hover:bg-primary/85 transition-all duration-300"
                  style={{ height: `${bar.pdf}px` }}
                  title={`PDFs: ${bar.pdf}`}
                ></div>
              </div>
              <span className="text-[10px] text-textSecondary font-semibold uppercase">{bar.label}</span>
            </div>
          ))}
        </div>
        <div className="flex gap-4 mt-4 text-xs font-semibold">
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 bg-rose-500 rounded-full inline-block"></span>
            <span className="text-textSecondary">Gemini Calls</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 bg-primary rounded-full inline-block"></span>
            <span className="text-textSecondary">PDF Exports</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Latest Active Users Grid */}
        <div className="border border-borderVariant bg-surface p-5 rounded-3xl space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-borderVariant">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Users size={16} className="text-primary" />
              Latest Active Users (Android Application)
            </h3>
            <span className="text-[10px] bg-primary/20 text-primary border border-primary/20 px-2 py-0.5 rounded-full font-bold">Real-time</span>
          </div>
          <div className="space-y-3.5">
            {recentUsers.length > 0 ? (
              recentUsers.map((user) => (
                <div key={user.id} className="flex items-center justify-between p-3 rounded-2xl bg-surfaceVariant/45 border border-borderVariant/55">
                  <div className="flex items-center gap-3">
                    <div className="h-9 w-9 bg-primary/10 rounded-full flex items-center justify-center text-xs font-semibold text-primary border border-primary/20">
                      {user.displayName ? user.displayName.substring(0,2).toUpperCase() : 'UI'}
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-white">{user.displayName || 'Anonymous'}</h4>
                      <p className="text-[10px] text-textSecondary">{user.email}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] font-semibold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full uppercase tracking-wider border border-emerald-500/10">Premium</span>
                    <p className="text-[9px] text-textSecondary mt-1">
                      {user.lastLogin?.seconds ? new Date(user.lastLogin.seconds * 1000).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : 'Just now'}
                    </p>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-xs text-textSecondary py-4 text-center">Waiting for Android device connection logs...</p>
            )}
          </div>
        </div>

        {/* Real-time System Audit Logs */}
        <div className="border border-borderVariant bg-surface p-5 rounded-3xl space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-borderVariant">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <ShieldAlert size={16} className="text-rose-400" />
              System Audit Trails & Security Logs
            </h3>
            <span className="text-[10px] bg-rose-500/20 text-rose-400 border border-rose-500/25 px-2 py-0.5 rounded-full font-bold">Monitor</span>
          </div>
          <div className="space-y-3">
            {recentLogs.length > 0 ? (
              recentLogs.map((log) => (
                <div key={log.id} className="p-3 rounded-2xl bg-[#090B14] border border-borderVariant/60 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-primary">{log.action}</span>
                    <span className="text-[9px] text-textSecondary">
                      {log.timestamp?.seconds ? new Date(log.timestamp.seconds * 1000).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : 'Just now'}
                    </span>
                  </div>
                  <p className="text-textSecondary text-[11px] mt-1">{log.details}</p>
                  <div className="mt-1.5 flex justify-between text-[9px] text-textSecondary/80">
                    <span>Admin: {log.admin || 'Admin'}</span>
                    <span>IP: {log.ip || '127.0.0.1'}</span>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-xs text-textSecondary py-4 text-center">Audit trail empty.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ==========================================
// 4. USER MANAGEMENT COMPONENT
// ==========================================
function UserManagement() {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, 'users'), orderBy('lastLogin', 'desc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = [];
      snapshot.forEach((d) => {
        list.push({ id: d.id, ...d.data() });
      });
      setUsers(list);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const handleTogglePremium = async (userId, currentVal) => {
    try {
      await updateDoc(doc(db, 'users', userId), {
        premium: !currentVal
      });
      // Log to logs
      await addDoc(collection(db, 'logs'), {
        action: 'Update User Role',
        admin: auth.currentUser?.email || 'Admin',
        details: `Toggled premium privilege status for user ${userId} to ${!currentVal}`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });
    } catch (e) {
      alert(`Operation failed: ${e.message}`);
    }
  };

  const handleToggleBlock = async (userId, currentStatus) => {
    const nextStatus = currentStatus === 'Blocked' ? 'Active' : 'Blocked';
    try {
      await updateDoc(doc(db, 'users', userId), {
        status: nextStatus
      });
      await addDoc(collection(db, 'logs'), {
        action: nextStatus === 'Blocked' ? 'Block User' : 'Unblock User',
        admin: auth.currentUser?.email || 'Admin',
        details: `Updated state status of user ${userId} to ${nextStatus}`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });
    } catch (e) {
      alert(`Operation failed: ${e.message}`);
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!window.confirm(`Permanently purge records of ${userId} from cloud storage database?`)) return;
    try {
      await deleteDoc(doc(db, 'users', userId));
      await addDoc(collection(db, 'logs'), {
        action: 'Delete User Account',
        admin: auth.currentUser?.email || 'Admin',
        details: `Purged and revoked privileges of user ${userId}`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });
    } catch (e) {
      alert(`Purge failed: ${e.message}`);
    }
  };

  const filtered = users.filter((u) => 
    u.email?.toLowerCase().includes(search.toLowerCase()) || 
    u.displayName?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <Users size={18} className="text-primary" />
          Cloud App Users Registry
        </h3>
        <div className="relative w-full sm:w-64">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center text-textSecondary pointer-events-none">
            <Search size={15} />
          </div>
          <input
            type="text"
            placeholder="Search email or name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-[#080911] border border-borderVariant rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-primary focus:border-transparent text-white"
          />
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-borderVariant text-xs text-textSecondary uppercase font-semibold">
              <th className="py-3 px-4">Profile Identity</th>
              <th className="py-3 px-4">Device Spec</th>
              <th className="py-3 px-4">Subscription</th>
              <th className="py-3 px-4">Database State</th>
              <th className="py-3 px-4">Last Event</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-borderVariant text-xs">
            {loading ? (
              <tr>
                <td colSpan="6" className="py-8 text-center text-textSecondary animate-pulse">
                  Querying database tables...
                </td>
              </tr>
            ) : filtered.length > 0 ? (
              filtered.map((u) => (
                <tr key={u.id} className="hover:bg-surfaceVariant/20 transition">
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-3">
                      <div className="h-8 w-8 bg-primary/10 border border-primary/20 rounded-full flex items-center justify-center font-bold text-primary">
                        {u.displayName ? u.displayName.substring(0,2).toUpperCase() : 'UI'}
                      </div>
                      <div>
                        <div className="font-bold text-white">{u.displayName || 'Guest User'}</div>
                        <div className="text-[10px] text-textSecondary">{u.email}</div>
                      </div>
                    </div>
                  </td>
                  <td className="py-3 px-4 text-textSecondary">{u.device || 'Android Client'}</td>
                  <td className="py-3 px-4">
                    <button 
                      onClick={() => handleTogglePremium(u.id, u.premium)}
                      className={`px-2 py-0.5 rounded-full font-semibold uppercase tracking-wider text-[9px] border ${
                        u.premium 
                          ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' 
                          : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
                      }`}
                    >
                      {u.premium ? 'Premium VIP' : 'Basic Tier'}
                    </button>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`px-2.5 py-0.5 rounded-full font-semibold uppercase text-[9px] border ${
                      u.status === 'Blocked' 
                        ? 'bg-rose-500/10 text-rose-400 border-rose-500/20 animate-pulse' 
                        : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    }`}>
                      {u.status || 'Active'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-textSecondary">
                    {u.lastLogin?.seconds ? new Date(u.lastLogin.seconds * 1000).toLocaleDateString() : 'Active now'}
                  </td>
                  <td className="py-3 px-4 text-right space-x-1.5 whitespace-nowrap">
                    <button 
                      onClick={() => handleToggleBlock(u.id, u.status)}
                      className={`px-2 py-1.5 rounded-xl font-bold transition border ${
                        u.status === 'Blocked' 
                          ? 'bg-emerald-500/15 border-emerald-500/20 text-emerald-400 hover:bg-emerald-500/25' 
                          : 'bg-amber-500/15 border-amber-500/20 text-amber-400 hover:bg-amber-500/25'
                      }`}
                    >
                      {u.status === 'Blocked' ? 'Grant Access' : 'Revoke Access'}
                    </button>
                    <button 
                      onClick={() => handleDeleteUser(u.id)}
                      className="bg-rose-500/15 border border-rose-500/20 hover:bg-rose-500/25 text-rose-400 p-1.5 rounded-xl transition inline-block"
                      title="Purge"
                    >
                      <Trash size={13} />
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" className="py-6 text-center text-textSecondary text-xs">
                  Zero corresponding records matching filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ==========================================
// 5. ADMOB CONTROL COMPONENT
// ==========================================
function AdMobControl() {
  const [config, setConfig] = useState({
    admobEnabled: true,
    bannerAdUnitId: '',
    nativeAdUnitId: '',
    interstitialAdUnitId: '',
    rewardedAdUnitId: '',
    appOpenAdUnitId: '',
    testMode: true,
    adFrequency: 3,
    adInterval: 60
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const unsubscribe = onSnapshot(doc(db, 'admob_settings', 'current'), (snapshot) => {
      if (snapshot.exists()) {
        setConfig(snapshot.data());
      }
    });
    return () => unsubscribe();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      await setDoc(doc(db, 'admob_settings', 'current'), {
        ...config,
        updatedAt: serverTimestamp()
      }, { merge: true });

      // Keep app_settings root boolean up-to-date with AdMob too
      await updateDoc(doc(db, 'app_settings', 'current'), {
        admobEnabled: config.admobEnabled
      });

      await addDoc(collection(db, 'logs'), {
        action: 'Update AdMob Control',
        admin: auth.currentUser?.email || 'Admin',
        details: `Redeclared AdMob revenue integrations & operational frequency. Test Mode: ${config.testMode}`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });
      alert('AdMob dynamic configuration committed successfully! Clients synchronized in real-time.');
    } catch (e) {
      alert(`Commit error: ${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  const adFormats = [
    { key: 'bannerAdUnitId', label: 'Banner Ads Placement', desc: 'Sits at the bottom bar of main workflow views.' },
    { key: 'nativeAdUnitId', label: 'Native Advanced Placements', desc: 'Infuses inside lists and menus matching original typography.' },
    { key: 'interstitialAdUnitId', label: 'Interstitial Placements', desc: 'Triggers on completion of core task generation actions.' },
    { key: 'rewardedAdUnitId', label: 'Rewarded Placements', desc: 'Unlocks AI premium limit pools for standard guests.' },
    { key: 'appOpenAdUnitId', label: 'App Open Placements', desc: 'Displays immediately upon app launch sequence.' },
  ];

  return (
    <div className="space-y-6">
      <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-6">
        <div className="flex items-center justify-between pb-3 border-b border-borderVariant">
          <div className="flex items-center gap-3">
            <div className="bg-primary/10 p-2 rounded-xl text-primary border border-primary/20">
              <Radio size={20} />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Google AdMob Revenue Dashboard</h3>
              <p className="text-xs text-textSecondary mt-0.5">Toggle live ads, adjust unit placements, and customize impression frequencies instantly.</p>
            </div>
          </div>
          <button
            onClick={handleSave}
            disabled={saving}
            className="bg-primary hover:bg-primaryHover text-white px-5 py-2 rounded-2xl text-xs font-semibold shadow-lg shadow-primary/15 transition disabled:opacity-50"
          >
            {saving ? 'Syncing...' : 'Publish Integration Changes'}
          </button>
        </div>

        {/* Global Master Swapper */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-2xl bg-surfaceVariant/45 border border-borderVariant/60 gap-4">
          <div>
            <h4 className="text-sm font-bold text-white">AdMob Revenue Master Engine</h4>
            <p className="text-xs text-textSecondary mt-0.5">Master toggle to display or turn off all monetization placements in the app.</p>
          </div>
          <div className="flex items-center gap-2.5">
            <span className="text-xs text-textSecondary font-semibold">{config.admobEnabled ? 'MONETIZATION LIVE' : 'MONETIZATION MUTED'}</span>
            <input
              type="checkbox"
              checked={config.admobEnabled}
              onChange={(e) => setConfig({ ...config, admobEnabled: e.target.checked })}
              className="h-5 w-10 cursor-pointer rounded-full bg-slate-500 text-primary transition focus:ring-0 appearance-none border border-borderVariant checked:bg-primary relative before:absolute before:h-4 before:w-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
            />
          </div>
        </div>

        {config.admobEnabled && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
            <div className="space-y-4">
              <h4 className="text-xs font-bold text-primary uppercase tracking-wider">Placement Unit Configurations</h4>
              {adFormats.map((f) => (
                <div key={f.key} className="space-y-1.5 p-3.5 bg-[#090B14] border border-borderVariant/65 rounded-2xl">
                  <div className="flex justify-between items-center">
                    <label className="text-xs font-bold text-white">{f.label}</label>
                    <span className="text-[9px] text-textSecondary/85">{config.testMode ? 'Test Placement' : 'Production Unit'}</span>
                  </div>
                  <input
                    type="text"
                    placeholder="ca-app-pub-3940256099942544/..."
                    value={config[f.key] || ''}
                    onChange={(e) => setConfig({ ...config, [f.key]: e.target.value })}
                    className="w-full bg-surface border border-borderVariant/85 rounded-xl text-xs px-3.5 py-2 focus:outline-none focus:ring-1 focus:ring-primary focus:border-transparent text-white"
                  />
                  <p className="text-[10px] text-textSecondary">{f.desc}</p>
                </div>
              ))}
            </div>

            <div className="space-y-6">
              <div className="space-y-4 p-4.5 border border-borderVariant/55 rounded-2xl bg-[#090B14]">
                <h4 className="text-xs font-bold text-primary uppercase tracking-wider">Monetization Filters & Policy Configuration</h4>
                
                <div className="flex justify-between items-center py-2.5 border-b border-borderVariant/55">
                  <div>
                    <span className="text-xs font-bold text-white block">AdMob Safety Test Mode</span>
                    <span className="text-[10px] text-textSecondary">Use standard Google monetization sandboxes to bypass account suspensions.</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={config.testMode}
                    onChange={(e) => setConfig({ ...config, testMode: e.target.checked })}
                    className="h-4.5 w-4.5 rounded border-borderVariant bg-[#070912] text-primary focus:ring-primary focus:ring-offset-0 transition"
                  />
                </div>

                <div className="space-y-2 py-2.5 border-b border-borderVariant/55">
                  <div className="flex justify-between">
                    <span className="text-xs font-bold text-white">Interstitial Impression Frequency</span>
                    <span className="text-xs text-primary font-bold">{config.adFrequency} Tasks</span>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="10"
                    value={config.adFrequency || 3}
                    onChange={(e) => setConfig({ ...config, adFrequency: parseInt(e.target.value) })}
                    className="w-full h-1 bg-borderVariant rounded-lg appearance-none cursor-pointer accent-primary"
                  />
                  <p className="text-[10px] text-textSecondary">Amount of task generations (PDF Compile, Merge) that must complete before launching full-screen interstitial ads.</p>
                </div>

                <div className="space-y-2 py-2">
                  <div className="flex justify-between">
                    <span className="text-xs font-bold text-white">Interstitial Cooling Interval</span>
                    <span className="text-xs text-primary font-bold">{config.adInterval} Seconds</span>
                  </div>
                  <input
                    type="range"
                    min="15"
                    max="300"
                    step="15"
                    value={config.adInterval || 60}
                    onChange={(e) => setConfig({ ...config, adInterval: parseInt(e.target.value) })}
                    className="w-full h-1 bg-borderVariant rounded-lg appearance-none cursor-pointer accent-primary"
                  />
                  <p className="text-[10px] text-textSecondary">Minimum cooling-down time (seconds) forced between consecutive full-screen interstitial impressions.</p>
                </div>
              </div>

              <div className="bg-primary/5 border border-primary/15 p-4 rounded-2xl flex gap-3 text-xs text-textSecondary">
                <ShieldAlert size={18} className="text-primary shrink-0" />
                <p>
                  <strong>Revenue Synchronization Notice:</strong> Live Android clients observe these settings dynamically via Firebase document snapshots. Once committed, active users experience instantaneous placement updates without needing system updates.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ==========================================
// 6. AI SETTINGS COMPONENT
// ==========================================
function AISettings() {
  const [config, setConfig] = useState({
    geminiApiKey: '',
    aiEnabled: true,
    ocrEnabled: true,
    chatPdf: true,
    translation: true,
    rewrite: true,
    summarize: true,
    grammar: true,
    notes: true,
    quiz: true,
    flashcards: true,
    dailyLimit: 20,
    tokenLimit: 2048,
    maxFileSize: 20,
    maxPages: 50
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const unsubscribe = onSnapshot(doc(db, 'ai_settings', 'current'), (snapshot) => {
      if (snapshot.exists()) {
        setConfig(snapshot.data());
      }
    });
    return () => unsubscribe();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      await setDoc(doc(db, 'ai_settings', 'current'), {
        ...config,
        updatedAt: serverTimestamp()
      }, { merge: true });

      await addDoc(collection(db, 'logs'), {
        action: 'Update AI Core Settings',
        admin: auth.currentUser?.email || 'Admin',
        details: `Updated AI and OCR core features. AI Enabled: ${config.aiEnabled}. Managed daily limits & file parameters.`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });
      alert('AI Integration and model parameters updated! Live clients updated in real-time.');
    } catch (e) {
      alert(`Commit error: ${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  const modelSwitches = [
    { key: 'aiEnabled', label: 'Master AI Integrations', desc: 'Toggle Gemini intelligence APIs globally.' },
    { key: 'ocrEnabled', label: 'OCR Document Scans', desc: 'Activate text extraction from camera scans.' },
    { key: 'chatPdf', label: 'Chat PDF Sandbox', desc: 'Enables chat dialogue with loaded documents.' },
    { key: 'translation', label: 'AI Language Translation', desc: 'Allows translating extracted pdf strings.' },
    { key: 'rewrite', label: 'AI Prompt Rewriter', desc: 'Optimizes sentences for polished styles.' },
    { key: 'summarize', label: 'AI Instant Summarizer', desc: 'Condenses full book page outputs.' },
    { key: 'grammar', label: 'Grammar Proofreader', desc: 'Performs syntax corrections on page texts.' },
    { key: 'notes', label: 'AI Automated Notes', desc: 'Builds comprehensive meeting logs instantly.' },
    { key: 'quiz', label: 'Quiz Creator Engine', desc: 'Generates evaluation tests from documents.' },
    { key: 'flashcards', label: 'AI Flashcards Builder', desc: 'Assembles review decks for exams.' }
  ];

  return (
    <div className="space-y-6">
      <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-6">
        <div className="flex items-center justify-between pb-3 border-b border-borderVariant">
          <div className="flex items-center gap-3">
            <div className="bg-primary/10 p-2 rounded-xl text-primary border border-primary/20">
              <Cpu size={20} />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Gemini AI & Document Engine Controls</h3>
              <p className="text-xs text-textSecondary mt-0.5">Control Gemini model configurations, security key chains, and sub-feature states.</p>
            </div>
          </div>
          <button
            onClick={handleSave}
            disabled={saving}
            className="bg-primary hover:bg-primaryHover text-white px-5 py-2 rounded-2xl text-xs font-semibold shadow-lg shadow-primary/15 transition disabled:opacity-50"
          >
            {saving ? 'Syncing...' : 'Publish AI Parameters'}
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <h4 className="text-xs font-bold text-primary uppercase tracking-wider">AI Security & Secret Chains</h4>
            <div className="p-4 bg-[#090B14] border border-borderVariant/60 rounded-2xl space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-white flex items-center gap-2">
                  <Key size={14} className="text-primary" />
                  Google Gemini Developer API Secret Key
                </label>
                <input
                  type="password"
                  placeholder="AIzaSy..."
                  value={config.geminiApiKey || ''}
                  onChange={(e) => setConfig({ ...config, geminiApiKey: e.target.value })}
                  className="w-full bg-surface border border-borderVariant/85 rounded-xl text-xs px-3.5 py-3 focus:outline-none focus:ring-1 focus:ring-primary focus:border-transparent text-white"
                />
                <p className="text-[10px] text-textSecondary">Secrets Gradle Plugin syncs this API key down to active compiler builds instantly.</p>
              </div>

              <div className="grid grid-cols-2 gap-3.5 pt-2 border-t border-borderVariant/45">
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase">Daily Request Pool</label>
                  <input
                    type="number"
                    value={config.dailyLimit || 20}
                    onChange={(e) => setConfig({ ...config, dailyLimit: parseInt(e.target.value) || 20 })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase">Token Window Limit</label>
                  <input
                    type="number"
                    value={config.tokenLimit || 2048}
                    onChange={(e) => setConfig({ ...config, tokenLimit: parseInt(e.target.value) || 2048 })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3.5 pt-1">
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase">Max PDF Size (MB)</label>
                  <input
                    type="number"
                    value={config.maxFileSize || 20}
                    onChange={(e) => setConfig({ ...config, maxFileSize: parseInt(e.target.value) || 20 })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase">Max Upload Pages</label>
                  <input
                    type="number"
                    value={config.maxPages || 50}
                    onChange={(e) => setConfig({ ...config, maxPages: parseInt(e.target.value) || 50 })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3.5 pt-2 border-t border-borderVariant/45">
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase">Active Gemini Model</label>
                  <select
                    value={config.modelName || 'gemini-1.5-flash'}
                    onChange={(e) => setConfig({ ...config, modelName: e.target.value })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  >
                    <option value="gemini-1.5-flash">gemini-1.5-flash (Standard)</option>
                    <option value="gemini-1.5-pro">gemini-1.5-pro (Accurate)</option>
                    <option value="gemini-2.0-flash-exp">gemini-2.0-flash-exp (Experimental)</option>
                  </select>
                </div>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <label className="text-[11px] font-semibold text-textSecondary uppercase">Temperature ({config.temperature !== undefined ? config.temperature : 0.7})</label>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.1"
                    value={config.temperature !== undefined ? config.temperature : 0.7}
                    onChange={(e) => setConfig({ ...config, temperature: parseFloat(e.target.value) })}
                    className="w-full h-8 bg-transparent appearance-none cursor-pointer accent-primary"
                  />
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-borderVariant/45">
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase block">Summarize Template</label>
                  <textarea
                    rows="2"
                    value={config.summarizePrompt || ''}
                    placeholder="e.g. Summarize this content:"
                    onChange={(e) => setConfig({ ...config, summarizePrompt: e.target.value })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-[11px] px-3 py-1.5 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase block">Translation Template</label>
                  <textarea
                    rows="2"
                    value={config.translatePrompt || ''}
                    placeholder="e.g. Translate this content to language:"
                    onChange={(e) => setConfig({ ...config, translatePrompt: e.target.value })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-[11px] px-3 py-1.5 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase block">Rewrite Template</label>
                  <textarea
                    rows="2"
                    value={config.rewritePrompt || ''}
                    placeholder="e.g. Rewrite this content professionally:"
                    onChange={(e) => setConfig({ ...config, rewritePrompt: e.target.value })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-[11px] px-3 py-1.5 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>
            </div>

            <div className="bg-primary/5 border border-primary/15 p-4 rounded-2xl flex gap-3 text-xs text-textSecondary">
              <Smartphone size={18} className="text-primary shrink-0" />
              <p>
                <strong>Security Advice:</strong> Securing the model keys via server-side storage avoids APK cracking and protects keys from external leaks.
              </p>
            </div>
          </div>

          <div className="space-y-3.5">
            <h4 className="text-xs font-bold text-primary uppercase tracking-wider">Modular AI Features Controls</h4>
            <div className="p-4 bg-[#090B14] border border-borderVariant/60 rounded-2xl divide-y divide-borderVariant/55 max-h-[380px] overflow-y-auto">
              {modelSwitches.map((m) => (
                <div key={m.key} className="flex justify-between items-center py-2.5 first:pt-0 last:pb-0">
                  <div>
                    <span className="text-xs font-bold text-white block">{m.label}</span>
                    <span className="text-[10px] text-textSecondary">{m.desc}</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={config[m.key] || false}
                    onChange={(e) => setConfig({ ...config, [m.key]: e.target.checked })}
                    className="h-4.5 w-9 cursor-pointer rounded-full bg-slate-500 text-primary transition focus:ring-0 appearance-none border border-borderVariant checked:bg-primary relative before:absolute before:h-3.5 before:w-3.5 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-4.5 before:transition-transform"
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ==========================================
// 7. APP SETTINGS COMPONENT
// ==========================================
function AppSettings() {
  const [config, setConfig] = useState({
    customAppName: 'AI PDF Studio',
    maintenanceMode: false,
    forceUpdate: false,
    latestVersion: '1.0.0',
    minimumVersion: '1.0.0',
    welcomePopup: '',
    noticeBanner: '',
    theme: 'system',
    enableScan: true,
    enableMerge: true,
    enableSplit: true,
    enableCompress: true,
    enableOCR: true,
    enableAI: true,
    enablePdfEditor: true,
    enableHistory: true,
    enableSettings: true,
    enablePremium: true
  });
  const [saving, setSaving] = useState(false);
  const [versions, setVersions] = useState([]);
  const [newVersion, setNewVersion] = useState({
    versionName: '',
    versionCode: '',
    releaseNotes: '',
    forceUpdate: false,
    minRequiredVersion: '1.0.0'
  });
  const [creatingVersion, setCreatingVersion] = useState(false);

  useEffect(() => {
    const unsubscribeConfig = onSnapshot(doc(db, 'app_settings', 'current'), (snapshot) => {
      if (snapshot.exists()) {
        setConfig(snapshot.data());
      }
    });

    const unsubscribeVersions = onSnapshot(collection(db, 'versions'), (snapshot) => {
      const list = [];
      snapshot.forEach((d) => {
        if (d.id !== 'current') {
          list.push({ id: d.id, ...d.data() });
        }
      });
      list.sort((a, b) => {
        const codeA = Number(a.versionCode) || 0;
        const codeB = Number(b.versionCode) || 0;
        return codeB - codeA;
      });
      setVersions(list);
    });

    return () => {
      unsubscribeConfig();
      unsubscribeVersions();
    };
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      await setDoc(doc(db, 'app_settings', 'current'), {
        ...config,
        updatedAt: serverTimestamp()
      }, { merge: true });

      await addDoc(collection(db, 'logs'), {
        action: 'Update Global App Settings',
        admin: auth.currentUser?.email || 'Admin',
        details: `Published app parameters. Maintenance Mode: ${config.maintenanceMode}. Set target version to ${config.latestVersion}.`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });
      alert('Global app settings updated successfully! Live devices listening in.');
    } catch (e) {
      alert(`Commit error: ${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  const handleAddVersion = async (e) => {
    e.preventDefault();
    if (!newVersion.versionName || !newVersion.versionCode) {
      alert('Version name and version code are mandatory.');
      return;
    }
    setCreatingVersion(true);
    try {
      const docId = `v${newVersion.versionName}`;
      await setDoc(doc(db, 'versions', docId), {
        versionName: newVersion.versionName,
        versionCode: Number(newVersion.versionCode),
        releaseNotes: newVersion.releaseNotes,
        releasedAt: serverTimestamp(),
        forceUpdate: newVersion.forceUpdate,
        minRequiredVersion: newVersion.minRequiredVersion
      });

      await addDoc(collection(db, 'logs'), {
        action: 'Create App Version',
        admin: auth.currentUser?.email || 'Admin',
        details: `Published version ${newVersion.versionName} (${newVersion.versionCode}) into registry.`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });

      alert(`Version ${newVersion.versionName} added to the registry successfully!`);
      setNewVersion({
        versionName: '',
        versionCode: '',
        releaseNotes: '',
        forceUpdate: false,
        minRequiredVersion: '1.0.0'
      });
    } catch (err) {
      alert(`Failed to add version: ${err.message}`);
    } finally {
      setCreatingVersion(false);
    }
  };

  const featureToggles = [
    { key: 'enableScan', label: 'Document Camera Scans' },
    { key: 'enableMerge', label: 'PDF Merge Module' },
    { key: 'enableSplit', label: 'PDF Splits & Extractors' },
    { key: 'enableCompress', label: 'File Compression Compress' },
    { key: 'enableOCR', label: 'Optical Character Recognition (OCR)' },
    { key: 'enableAI', label: 'Gemini AI Assistant Modules' },
    { key: 'enablePdfEditor', label: 'Visual Overlays PDF Editor' },
    { key: 'enableHistory', label: 'Recent Files History Log' },
    { key: 'enableSettings', label: 'User Advanced Settings Panel' },
    { key: 'enablePremium', label: 'Premium VIP Upgrades Screen' }
  ];

  return (
    <div className="space-y-6">
      <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-6">
        <div className="flex items-center justify-between pb-3 border-b border-borderVariant">
          <div className="flex items-center gap-3">
            <div className="bg-primary/10 p-2 rounded-xl text-primary border border-primary/20">
              <Settings size={20} />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Core System Settings & Parameters</h3>
              <p className="text-xs text-textSecondary mt-0.5">Control operational states, force updates, broadcast notices, and filter layout structures.</p>
            </div>
          </div>
          <button
            onClick={handleSave}
            disabled={saving}
            className="bg-primary hover:bg-primaryHover text-white px-5 py-2 rounded-2xl text-xs font-semibold shadow-lg shadow-primary/15 transition disabled:opacity-50"
          >
            {saving ? 'Syncing...' : 'Publish System Rules'}
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <h4 className="text-xs font-bold text-primary uppercase tracking-wider">Device Status, System Toggles, & Updates</h4>
            <div className="p-4 bg-[#090B14] border border-borderVariant/60 rounded-2xl space-y-4">
              
              <div className="flex justify-between items-center py-1">
                <div>
                  <span className="text-xs font-bold text-white block">App Maintenance Mute</span>
                  <span className="text-[10px] text-textSecondary">Locks the application for immediate server maintenance operations.</span>
                </div>
                <input
                  type="checkbox"
                  checked={config.maintenanceMode}
                  onChange={(e) => setConfig({ ...config, maintenanceMode: e.target.checked })}
                  className="h-4.5 w-9 cursor-pointer rounded-full bg-slate-500 text-primary transition focus:ring-0 appearance-none border border-borderVariant checked:bg-primary relative before:absolute before:h-3.5 before:w-3.5 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-4.5 before:transition-transform"
                />
              </div>

              <div className="flex justify-between items-center py-2.5 border-t border-borderVariant/45">
                <div>
                  <span className="text-xs font-bold text-white block">Force System Updates</span>
                  <span className="text-[10px] text-textSecondary">Blocks device usage until current app version targets are fully satisfied.</span>
                </div>
                <input
                  type="checkbox"
                  checked={config.forceUpdate}
                  onChange={(e) => setConfig({ ...config, forceUpdate: e.target.checked })}
                  className="h-4.5 w-9 cursor-pointer rounded-full bg-slate-500 text-primary transition focus:ring-0 appearance-none border border-borderVariant checked:bg-primary relative before:absolute before:h-3.5 before:w-3.5 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-4.5 before:transition-transform"
                />
              </div>

              <div className="grid grid-cols-2 gap-3.5 pt-2 border-t border-borderVariant/45">
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase">Latest Version Target</label>
                  <input
                    type="text"
                    value={config.latestVersion || '1.0.0'}
                    onChange={(e) => setConfig({ ...config, latestVersion: e.target.value })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-textSecondary uppercase">Minimum Required Version</label>
                  <input
                    type="text"
                    value={config.minimumVersion || '1.0.0'}
                    onChange={(e) => setConfig({ ...config, minimumVersion: e.target.value })}
                    className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <div className="space-y-2 border-t border-borderVariant/45 pt-3">
                <label className="text-xs font-bold text-white">Application Launcher Title Label</label>
                <input
                  type="text"
                  value={config.customAppName || 'AI PDF Studio'}
                  onChange={(e) => setConfig({ ...config, customAppName: e.target.value })}
                  className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3.5 py-2.5 focus:outline-none focus:ring-1 focus:ring-primary focus:border-transparent text-white font-semibold"
                />
              </div>

              <div className="space-y-1.5 border-t border-borderVariant/45 pt-3">
                <label className="text-xs font-bold text-white">Default Palette Mode Override</label>
                <select
                  value={config.theme || 'system'}
                  onChange={(e) => setConfig({ ...config, theme: e.target.value })}
                  className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3.5 py-2.5 focus:outline-none focus:ring-1 focus:ring-primary text-white"
                >
                  <option value="system">Follow Android System Preferences</option>
                  <option value="light">Lock Light Palette</option>
                  <option value="dark">Lock Indigo Dark Palette</option>
                </select>
              </div>
            </div>

            <div className="p-4 bg-[#090B14] border border-borderVariant/60 rounded-2xl space-y-4">
              <h4 className="text-xs font-bold text-primary uppercase tracking-wider">System Banner & Popups Configuration</h4>
              
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-white">App-Open Notice Popup Dialog</label>
                <textarea
                  rows="2"
                  value={config.welcomePopup || ''}
                  onChange={(e) => setConfig({ ...config, welcomePopup: e.target.value })}
                  placeholder="Leave empty to hide popup alert on launch..."
                  className="w-full bg-surface border border-borderVariant/85 rounded-xl text-xs px-3.5 py-2 focus:outline-none focus:ring-1 focus:ring-primary focus:border-transparent text-white"
                ></textarea>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-white">Top Dashboard Sticky Banner Alert</label>
                <input
                  type="text"
                  value={config.noticeBanner || ''}
                  onChange={(e) => setConfig({ ...config, noticeBanner: e.target.value })}
                  placeholder="Notice banner details..."
                  className="w-full bg-surface border border-borderVariant/85 rounded-xl text-xs px-3.5 py-2.5 focus:outline-none focus:ring-1 focus:ring-primary focus:border-transparent text-white"
                />
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <h4 className="text-xs font-bold text-primary uppercase tracking-wider">Modular Platform Features Activation</h4>
            <div className="p-4 bg-[#090B14] border border-borderVariant/60 rounded-2xl divide-y divide-borderVariant/50 space-y-1">
              {featureToggles.map((item) => (
                <div key={item.key} className="flex justify-between items-center py-2.5 first:pt-0 last:pb-0">
                  <span className="text-xs font-bold text-white">{item.label}</span>
                  <input
                    type="checkbox"
                    checked={config[item.key] || false}
                    onChange={(e) => setConfig({ ...config, [item.key]: e.target.checked })}
                    className="h-4.5 w-9 cursor-pointer rounded-full bg-slate-500 text-primary transition focus:ring-0 appearance-none border border-borderVariant checked:bg-primary relative before:absolute before:h-3.5 before:w-3.5 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-4.5 before:transition-transform"
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* App Versions Registry section */}
      <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-6">
        <div className="flex items-center gap-3 pb-3 border-b border-borderVariant">
          <div className="bg-primary/10 p-2 rounded-xl text-primary border border-primary/20">
            <Radio size={20} />
          </div>
          <div>
            <h3 className="text-base font-bold text-white">App Version Release Registry</h3>
            <p className="text-xs text-textSecondary mt-0.5">Publish and manage versions stored in Firestore "versions" collection.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Add Version Form */}
          <form onSubmit={handleAddVersion} className="lg:col-span-1 space-y-4 p-5 bg-[#090B14] border border-borderVariant/60 rounded-2xl">
            <h4 className="text-xs font-bold text-primary uppercase tracking-wider">Publish New Release</h4>
            
            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-textSecondary uppercase">Version Name</label>
              <input
                type="text"
                placeholder="e.g. 1.0.1"
                value={newVersion.versionName}
                onChange={(e) => setNewVersion({ ...newVersion, versionName: e.target.value })}
                className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-textSecondary uppercase">Version Code</label>
              <input
                type="number"
                placeholder="e.g. 2"
                value={newVersion.versionCode}
                onChange={(e) => setNewVersion({ ...newVersion, versionCode: e.target.value })}
                className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-textSecondary uppercase">Release Notes</label>
              <textarea
                rows="2"
                placeholder="What's new in this version..."
                value={newVersion.releaseNotes}
                onChange={(e) => setNewVersion({ ...newVersion, releaseNotes: e.target.value })}
                className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
              ></textarea>
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-textSecondary uppercase">Min Required Version</label>
              <input
                type="text"
                value={newVersion.minRequiredVersion}
                onChange={(e) => setNewVersion({ ...newVersion, minRequiredVersion: e.target.value })}
                className="w-full bg-surface border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            <div className="flex justify-between items-center py-2 border-t border-borderVariant/40">
              <div>
                <span className="text-xs font-bold text-white block">Force Update This Release</span>
                <span className="text-[10px] text-textSecondary">Requires user to upgrade.</span>
              </div>
              <input
                type="checkbox"
                checked={newVersion.forceUpdate}
                onChange={(e) => setNewVersion({ ...newVersion, forceUpdate: e.target.checked })}
                className="h-4.5 w-9 cursor-pointer rounded-full bg-slate-500 text-primary transition focus:ring-0 appearance-none border border-borderVariant checked:bg-primary relative before:absolute before:h-3.5 before:w-3.5 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-4.5 before:transition-transform"
              />
            </div>

            <button
              type="submit"
              disabled={creatingVersion}
              className="w-full bg-primary hover:bg-primaryHover text-white py-2 rounded-xl text-xs font-semibold shadow transition disabled:opacity-50"
            >
              {creatingVersion ? 'Publishing...' : 'Register Version Release'}
            </button>
          </form>

          {/* Versions List Table */}
          <div className="lg:col-span-2 space-y-4">
            <h4 className="text-xs font-bold text-primary uppercase tracking-wider">Historical Versions Registry ({versions.length})</h4>
            <div className="bg-[#090B14] border border-borderVariant/60 rounded-2xl overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-borderVariant/40 text-textSecondary uppercase text-[10px] tracking-wider font-semibold">
                      <th className="px-4 py-3">Version</th>
                      <th className="px-4 py-3">Code</th>
                      <th className="px-4 py-3">Min Version</th>
                      <th className="px-4 py-3">Force</th>
                      <th className="px-4 py-3">Release Notes</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-borderVariant/30">
                    {versions.map((v) => (
                      <tr key={v.id} className="hover:bg-white/[0.02] text-white">
                        <td className="px-4 py-3 font-semibold">{v.versionName}</td>
                        <td className="px-4 py-3 text-textSecondary">{v.versionCode}</td>
                        <td className="px-4 py-3 text-textSecondary">{v.minRequiredVersion || '1.0.0'}</td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-medium ${v.forceUpdate ? 'bg-red-500/15 text-red-400 border border-red-500/20' : 'bg-green-500/15 text-green-400 border border-green-500/20'}`}>
                            {v.forceUpdate ? 'Forced' : 'Optional'}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-textSecondary truncate max-w-[180px]" title={v.releaseNotes}>
                          {v.releaseNotes || 'No notes provided.'}
                        </td>
                      </tr>
                    ))}
                    {versions.length === 0 && (
                      <tr>
                        <td colSpan="5" className="px-4 py-8 text-center text-textSecondary">
                          No version release records found. Seeding will run or you can publish the first version release.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ==========================================
// 8. PUSH NOTIFICATIONS COMPONENT
// ==========================================
function Notifications() {
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [target, setTarget] = useState('all');
  const [scheduled, setScheduled] = useState('');
  const [history, setHistory] = useState([]);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    const q = query(collection(db, 'notifications'), orderBy('sentAt', 'desc'), limit(10));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = [];
      snapshot.forEach((d) => {
        list.push({ id: d.id, ...d.data() });
      });
      setHistory(list);
    });
    return () => unsubscribe();
  }, []);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!title || !message) {
      alert('Notification title and contents are mandatory inputs.');
      return;
    }
    setSending(true);
    try {
      await addDoc(collection(db, 'notifications'), {
        title,
        message,
        imageUrl: imageUrl || null,
        target,
        scheduled: scheduled || 'immediate',
        sentAt: serverTimestamp(),
        sentBy: auth.currentUser?.email || 'Admin',
        status: scheduled ? 'Scheduled' : 'Broadcast Sent'
      });

      await addDoc(collection(db, 'logs'), {
        action: 'Send Broadcast Notification',
        admin: auth.currentUser?.email || 'Admin',
        details: `Broadcasted push notice: "${title}". Target: ${target}.`,
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });

      alert(scheduled ? 'Push notification has been queued for broadcast successfully!' : 'Push notification successfully dispatched to FCM cloud server!');
      setTitle('');
      setMessage('');
      setImageUrl('');
      setScheduled('');
    } catch (e) {
      alert(`Dispatch failed: ${e.message}`);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-5">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <Bell size={18} className="text-primary" />
          FCM Cloud Messaging broadcast Control
        </h3>

        <form onSubmit={handleSend} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-textSecondary">Notice Head Title</label>
            <input
              type="text"
              placeholder="System update available, urgent scanner notification..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full bg-[#080911] border border-borderVariant rounded-xl text-xs px-3.5 py-2.5 text-white focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-textSecondary">Notice Message Details</label>
            <textarea
              rows="4"
              placeholder="Describe details of the update, instructions, or promotional alerts..."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="w-full bg-[#080911] border border-borderVariant rounded-xl text-xs px-3.5 py-2.5 text-white focus:outline-none focus:ring-1 focus:ring-primary"
            ></textarea>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-textSecondary">Banner Graphic URL (Optional)</label>
            <input
              type="text"
              placeholder="https://..."
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              className="w-full bg-[#080911] border border-borderVariant rounded-xl text-xs px-3.5 py-2.5 text-white focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-textSecondary">Audience Segments</label>
              <select
                value={target}
                onChange={(e) => setTarget(e.target.value)}
                className="w-full bg-[#080911] border border-borderVariant rounded-xl text-xs px-3.5 py-2.5 text-white focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value="all">All Registered Devices</option>
                <option value="premium">Premium VIP Members</option>
                <option value="basic">Guest Tier Users</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-textSecondary">Scheduling Release</label>
              <input
                type="datetime-local"
                value={scheduled}
                onChange={(e) => setScheduled(e.target.value)}
                className="w-full bg-[#080911] border border-borderVariant rounded-xl text-xs px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={sending}
            className="w-full bg-primary hover:bg-primaryHover text-white py-3 rounded-2xl text-xs font-bold shadow-lg shadow-primary/10 transition"
          >
            {sending ? 'Queueing Broadcast...' : 'Broadcast Push Notification Now'}
          </button>
        </form>
      </div>

      <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-4">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <History size={18} className="text-purple-400" />
          Historic Push Broadcast Trails
        </h3>

        <div className="space-y-3 max-h-[420px] overflow-y-auto pr-1">
          {history.length > 0 ? (
            history.map((h) => (
              <div key={h.id} className="p-3.5 rounded-2xl bg-surfaceVariant/45 border border-borderVariant/55 text-xs space-y-1">
                <div className="flex justify-between items-start">
                  <span className="font-bold text-white text-[13px]">{h.title}</span>
                  <span className={`px-2 py-0.5 rounded-full text-[9px] font-bold border uppercase tracking-wider ${
                    h.status === 'Scheduled' 
                      ? 'bg-amber-500/10 border-amber-500/20 text-amber-400' 
                      : 'bg-indigo-500/10 border-indigo-500/20 text-indigo-400'
                  }`}>
                    {h.status || 'Sent'}
                  </span>
                </div>
                <p className="text-textSecondary text-[11px] leading-relaxed">{h.message}</p>
                {h.imageUrl && (
                  <div className="mt-2 text-[10px] text-primary truncate hover:underline">
                    Image: <a href={h.imageUrl} target="_blank" rel="noreferrer">{h.imageUrl}</a>
                  </div>
                )}
                <div className="flex justify-between text-[10px] text-textSecondary pt-1.5 border-t border-borderVariant/45">
                  <span>Target: {h.target || 'all'}</span>
                  <span>{h.sentAt?.seconds ? new Date(h.sentAt.seconds * 1000).toLocaleString() : 'Just now'}</span>
                </div>
              </div>
            ))
          ) : (
            <p className="text-xs text-textSecondary py-8 text-center">Broadcast history trail is clean.</p>
          )}
        </div>
      </div>
    </div>
  );
}

// ==========================================
// 9. LOGS & AUDIT COMPONENT
// ==========================================
function Logs() {
  const [logs, setLogs] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, 'logs'), orderBy('timestamp', 'desc'), limit(50));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = [];
      snapshot.forEach((d) => {
        list.push({ id: d.id, ...d.data() });
      });
      setLogs(list);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const handleClearLogs = async () => {
    if (!window.confirm('Delete all diagnostic audit logs permanently from Firestore database?')) return;
    try {
      const q = query(collection(db, 'logs'));
      const snapshot = await getDocs(q);
      const batchPromises = [];
      snapshot.forEach((d) => {
        batchPromises.push(deleteDoc(doc(db, 'logs', d.id)));
      });
      await Promise.all(batchPromises);

      await addDoc(collection(db, 'logs'), {
        action: 'Purge Audit Trails',
        admin: auth.currentUser?.email || 'Admin',
        details: 'Audit logs cleared down to empty.',
        timestamp: serverTimestamp(),
        ip: '127.0.0.1'
      });
    } catch (e) {
      alert(`Purge failed: ${e.message}`);
    }
  };

  const filtered = logs.filter((l) => 
    l.action?.toLowerCase().includes(search.toLowerCase()) || 
    l.details?.toLowerCase().includes(search.toLowerCase()) ||
    l.admin?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="border border-borderVariant bg-surface p-6 rounded-3xl space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-borderVariant pb-3">
        <div>
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <ShieldAlert size={18} className="text-rose-400" />
            Active Administrator Security Logs
          </h3>
          <p className="text-xs text-textSecondary mt-0.5">Real-time trails tracking administrative updates, policy actions, and IP identifiers.</p>
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <input
            type="text"
            placeholder="Search action details..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="bg-[#080911] border border-borderVariant rounded-xl text-xs px-3.5 py-2 text-white focus:outline-none w-full sm:w-48"
          />
          <button
            onClick={handleClearLogs}
            className="bg-rose-500/10 hover:bg-rose-500/15 border border-rose-500/25 text-rose-400 px-3 py-2 rounded-xl text-xs font-semibold whitespace-nowrap"
          >
            Clear Log Trails
          </button>
        </div>
      </div>

      <div className="space-y-3.5 max-h-[480px] overflow-y-auto pr-1">
        {loading ? (
          <p className="text-xs text-textSecondary py-8 text-center animate-pulse">Reading system tables...</p>
        ) : filtered.length > 0 ? (
          filtered.map((log) => (
            <div key={log.id} className="p-3.5 bg-[#090B14] border border-borderVariant/65 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="font-extrabold text-primary text-[13px]">{log.action}</span>
                  <span className="bg-surfaceVariant text-textSecondary text-[9px] px-2 py-0.5 rounded-full font-bold">
                    IP: {log.ip || '127.0.0.1'}
                  </span>
                </div>
                <p className="text-textSecondary text-[11px] leading-relaxed">{log.details}</p>
              </div>
              <div className="text-left sm:text-right shrink-0">
                <p className="font-bold text-white text-[11px]">By: {log.admin || 'System'}</p>
                <p className="text-[10px] text-textSecondary mt-0.5">
                  {log.timestamp?.seconds ? new Date(log.timestamp.seconds * 1000).toLocaleString() : 'Just now'}
                </p>
              </div>
            </div>
          ))
        ) : (
          <p className="text-xs text-textSecondary py-8 text-center">Diagnostic logs matching filter are empty.</p>
        )}
      </div>
    </div>
  );
}
