import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Shield, Bell, Globe, Save, Lock, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { EmailAuthProvider, reauthenticateWithCredential, updatePassword } from 'firebase/auth';
import { useAuth } from '../contexts/AuthContext';
import { auth } from '../config/firebase';

interface SettingsSection {
  id: string;
  label: string;
  icon: React.ElementType;
}

const SECTIONS: SettingsSection[] = [
  { id: 'profile', label: 'Profile', icon: User },
  { id: 'security', label: 'Security', icon: Shield },
  { id: 'notifications', label: 'Notifications', icon: Bell },
  { id: 'system', label: 'System', icon: Globe },
];

export default function SettingsPage() {
  const { mealshareUser, signOut } = useAuth();
  const navigate = useNavigate();
  const [activeSection, setActiveSection] = useState('profile');
  const [saved, setSaved] = useState(false);

  // Security / Password change states
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdLoading, setPwdLoading] = useState(false);
  const [pwdError, setPwdError] = useState<string | null>(null);
  const [pwdSuccess, setPwdSuccess] = useState<string | null>(null);

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setPwdError(null);
    setPwdSuccess(null);

    if (!currentPassword || !newPassword || !confirmPassword) {
      setPwdError('Please complete all password fields.');
      return;
    }

    if (newPassword.length < 6) {
      setPwdError('New password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPwdError('New password and confirmation do not match.');
      return;
    }

    if (newPassword === currentPassword) {
      setPwdError('New password must be different from current password.');
      return;
    }

    const user = auth.currentUser;
    if (!user || !user.email) {
      setPwdError('No active authenticated session found. Please sign in again.');
      return;
    }

    setPwdLoading(true);

    try {
      // Re-authenticate current user with current password using Firebase Web SDK
      const credential = EmailAuthProvider.credential(user.email, currentPassword);
      await reauthenticateWithCredential(user, credential);

      // Update password via Firebase Auth SDK
      await updatePassword(user, newPassword);

      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPwdSuccess('Password changed successfully! You will be signed out and redirected to login in 3 seconds.');

      setTimeout(async () => {
        try {
          await signOut();
        } finally {
          navigate('/login', { replace: true });
        }
      }, 3000);
    } catch (err: any) {
      const code = err?.code || '';
      if (code === 'auth/wrong-password' || code === 'auth/invalid-credential') {
        setPwdError('The current password you entered is incorrect.');
      } else if (code === 'auth/weak-password') {
        setPwdError('The new password is too weak. Please choose a stronger password (at least 6 characters).');
      } else if (code === 'auth/requires-recent-login') {
        setPwdError('Security requirement: Please sign out and sign in again before changing your password.');
      } else if (code === 'auth/too-many-requests') {
        setPwdError('Too many failed attempts. Please try again later.');
      } else {
        setPwdError(err.message || 'Failed to change password. Please try again.');
      }
    } finally {
      setPwdLoading(false);
    }
  };

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Settings</h1>
          <p className="text-sm text-gray-500">Manage your admin preferences</p>
        </div>
        <button onClick={handleSave} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition">
          <Save className="w-4 h-4" /> {saved ? 'Saved!' : 'Save Changes'}
        </button>
      </div>

      <div className="flex flex-col lg:flex-row gap-6">
        {/* Section Nav */}
        <div className="w-full lg:w-56 flex lg:flex-col gap-1 overflow-x-auto">
          {SECTIONS.map((s) => (
            <button key={s.id} onClick={() => setActiveSection(s.id)} className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm font-medium whitespace-nowrap transition ${activeSection === s.id ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-50'}`}>
              <s.icon className={`w-4 h-4 ${activeSection === s.id ? 'text-primary-600' : 'text-gray-400'}`} />{s.label}
            </button>
          ))}
        </div>

        {/* Content */}
        <div className="flex-1">
          {activeSection === 'profile' && (
            <div className="bg-white rounded-xl border border-gray-100 p-6 space-y-5">
              <h3 className="text-sm font-semibold text-gray-900">Profile Information</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1">Full Name</label>
                  <input
                    type="text"
                    defaultValue={mealshareUser?.name || 'Platform Administrator'}
                    className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1">Email</label>
                  <input
                    type="email"
                    readOnly
                    value={mealshareUser?.email || 'admin@mealshare.org'}
                    className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg bg-gray-50 text-gray-700"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1">Account Role</label>
                  <input
                    type="text"
                    value={mealshareUser?.role ? `ROLE_${mealshareUser.role}` : 'ROLE_ADMIN'}
                    readOnly
                    className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg bg-gray-50 text-gray-700 font-mono text-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1">Account Status</label>
                  <input
                    type="text"
                    value={mealshareUser?.status || 'ACTIVE'}
                    readOnly
                    className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg bg-gray-50 text-emerald-700 font-semibold text-xs"
                  />
                </div>
              </div>
            </div>
          )}

          {activeSection === 'security' && (
            <div className="bg-white rounded-xl border border-gray-100 p-6 space-y-5">
              <div>
                <h3 className="text-sm font-semibold text-gray-900">Security Settings</h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Update your administrator account password via Firebase Authentication
                </p>
              </div>

              {pwdError && (
                <div className="flex items-center gap-2 p-3 text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg">
                  <AlertTriangle className="w-4 h-4 shrink-0 text-red-600" />
                  <span>{pwdError}</span>
                </div>
              )}

              {pwdSuccess && (
                <div className="flex items-center gap-2 p-3 text-xs text-emerald-800 bg-emerald-50 border border-emerald-200 rounded-lg">
                  <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
                  <span>{pwdSuccess}</span>
                </div>
              )}

              <form onSubmit={handlePasswordChange} className="space-y-4 max-w-md">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Current Password
                  </label>
                  <input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Enter current password"
                    disabled={pwdLoading}
                    className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20 disabled:bg-gray-50"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    New Password
                  </label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Enter new password (min. 6 characters)"
                    disabled={pwdLoading}
                    className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20 disabled:bg-gray-50"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Confirm New Password
                  </label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Confirm new password"
                    disabled={pwdLoading}
                    className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20 disabled:bg-gray-50"
                  />
                </div>

                <button
                  type="submit"
                  disabled={pwdLoading}
                  className="flex items-center justify-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 disabled:opacity-50 transition"
                >
                  {pwdLoading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Updating Password...
                    </>
                  ) : (
                    <>
                      <Lock className="w-4 h-4" />
                      Change Password
                    </>
                  )}
                </button>
              </form>

              <div className="pt-4 border-t border-gray-100">
                <h4 className="text-sm font-medium text-gray-700 mb-2">Sessions</h4>
                <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div>
                    <p className="text-sm font-medium text-gray-800">Current Session</p>
                    <p className="text-xs text-gray-500">Firebase Token Authenticated • {mealshareUser?.email}</p>
                  </div>
                  <span className="text-xs bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-full font-medium">Active</span>
                </div>
              </div>
            </div>
          )}

          {activeSection === 'notifications' && (
            <div className="bg-white rounded-xl border border-gray-100 p-6 space-y-5">
              <h3 className="text-sm font-semibold text-gray-900">Notification Preferences</h3>
              {[
                { label: 'New donation created', desc: 'Notify when a donor adds a new food donation', default: true },
                { label: 'New request received', desc: 'Notify when a receiver organization requests food', default: true },
                { label: 'Request approved/rejected', desc: 'Notify on request status changes', default: true },
                { label: 'Pickup scheduled', desc: 'Notify when a pickup is scheduled for fulfillment', default: true },
                { label: 'Pickup completed', desc: 'Notify when a pickup is delivered', default: false },
                { label: 'Verification submitted', desc: 'Notify when an org submits verification', default: true },
                { label: 'System updates', desc: 'Platform updates and maintenance notices', default: false },
              ].map((pref) => (
                <div key={pref.label} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <div><p className="text-sm font-medium text-gray-800">{pref.label}</p><p className="text-xs text-gray-500">{pref.desc}</p></div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input type="checkbox" defaultChecked={pref.default} className="sr-only peer" />
                    <div className="w-9 h-5 bg-gray-200 rounded-full peer peer-checked:bg-primary-500 peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all"></div>
                  </label>
                </div>
              ))}
            </div>
          )}

          {activeSection === 'system' && (
            <div className="bg-white rounded-xl border border-gray-100 p-6 space-y-5">
              <h3 className="text-sm font-semibold text-gray-900">System Preferences</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Timezone</label>
                  <select defaultValue="IST" className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg"><option value="IST">Asia/Kolkata (IST)</option><option value="UTC">UTC</option></select>
                </div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Date Format</label>
                  <select defaultValue="dd/mm/yyyy" className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg"><option value="dd/mm/yyyy">DD/MM/YYYY</option><option value="mm/dd/yyyy">MM/DD/YYYY</option><option value="yyyy-mm-dd">YYYY-MM-DD</option></select>
                </div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Language</label>
                  <select defaultValue="en" className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg"><option value="en">English</option><option value="hi">Hindi</option><option value="kn">Kannada</option></select>
                </div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Items per page</label>
                  <select defaultValue="25" className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg"><option value="10">10</option><option value="25">25</option><option value="50">50</option><option value="100">100</option></select>
                </div>
              </div>
              <div className="pt-4 border-t border-gray-100">
                <h4 className="text-sm font-medium text-gray-700 mb-2">About</h4>
                <div className="text-sm text-gray-500 space-y-1">
                  <p>MealShare Admin v1.0.0 (Production Core)</p>
                  <p>Connected to Spring Boot 3.4.3 &amp; PostgreSQL 16.8 + PostGIS 3.4.3</p>
                  <p>© 2026 MealShare Surplus Redistribution Platform.</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
