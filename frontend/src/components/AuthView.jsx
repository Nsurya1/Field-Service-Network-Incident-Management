import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { Activity, Lock, User, Mail, AlertCircle, CheckCircle, KeyRound, ArrowLeft } from 'lucide-react';

export default function AuthView() {
  const { login, register } = useAuth();
  const [tab, setTab] = useState('signin'); // 'signin' | 'register' | 'forgot'
  
  // Sign In / Register state
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
  });

  // Forgot password state
  const [forgotInput, setForgotInput] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [forgotStep, setForgotStep] = useState(1); // 1 = request token, 2 = enter token + new password

  // Feedback state
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const resetMessages = () => {
    setError('');
    setSuccess('');
  };

  const handleSignInOrRegister = async (e) => {
    e.preventDefault();
    resetMessages();
    setLoading(true);

    try {
      if (tab === 'register') {
        await register(formData.username, formData.email, formData.password);
      } else {
        await login(formData.username, formData.password);
      }
    } catch (err) {
      if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
        setError('Server is waking up (Render free tier cold start). Please wait 30 seconds and try again.');
      } else if (err.message?.includes('Network Error')) {
        setError('Cannot connect to backend. Please verify your Render service is Live and VITE_API_BASE_URL is set in Vercel.');
      } else {
        setError(
          err.response?.data?.message ||
          err.message ||
          (tab === 'register' ? 'Registration failed. Try another username/email.' : 'Invalid credentials. Please verify username and password.')
        );
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRequestResetCode = async (e) => {
    e.preventDefault();
    resetMessages();
    setLoading(true);

    try {
      const data = await authService.forgotPassword(forgotInput);
      setSuccess(data.message || 'Reset code generated successfully.');
      if (data.resetToken) {
        setResetToken(data.resetToken);
      }
      setForgotStep(2);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to request password reset code.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    resetMessages();
    setLoading(true);

    try {
      const data = await authService.resetPassword(resetToken, newPassword);
      setSuccess(data.message || 'Password reset successfully! Please sign in with your new password.');
      setTab('signin');
      setForgotStep(1);
      setResetToken('');
      setNewPassword('');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reset password. Please check your token.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col justify-center items-center p-4">
      <div className="max-w-md w-full">
        {/* App Branding */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center p-3 bg-indigo-600 text-white rounded-2xl shadow-md mb-3">
            <Activity className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">FieldService</h1>
          <p className="text-xs text-slate-500 mt-1">Network Incident & Dispatch Management System</p>
        </div>

        {/* Auth Card */}
        <div className="bg-white rounded-2xl shadow-xl border border-slate-200 p-8">
          {/* Tab switches */}
          <div className="flex border-b border-slate-200 mb-6">
            <button
              onClick={() => { setTab('signin'); resetMessages(); }}
              className={`flex-1 pb-3 text-xs font-bold uppercase tracking-wider transition border-b-2 cursor-pointer ${
                tab === 'signin'
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}
            >
              Sign In
            </button>
            <button
              onClick={() => { setTab('register'); resetMessages(); }}
              className={`flex-1 pb-3 text-xs font-bold uppercase tracking-wider transition border-b-2 cursor-pointer ${
                tab === 'register'
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}
            >
              Create Account
            </button>
            <button
              onClick={() => { setTab('forgot'); resetMessages(); }}
              className={`flex-1 pb-3 text-xs font-bold uppercase tracking-wider transition border-b-2 cursor-pointer ${
                tab === 'forgot'
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}
            >
              Reset Password
            </button>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-start">
              <AlertCircle className="w-4 h-4 mr-2 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-xs flex items-start">
              <CheckCircle className="w-4 h-4 mr-2 shrink-0 mt-0.5" />
              <span>{success}</span>
            </div>
          )}

          {/* SIGN IN / REGISTER FORM */}
          {tab !== 'forgot' && (
            <form onSubmit={handleSignInOrRegister} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Username</label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    type="text"
                    required
                    placeholder="e.g. john_doe"
                    value={formData.username}
                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                    className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  />
                </div>
              </div>

              {tab === 'register' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Email Address</label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                    <input
                      type="email"
                      required
                      placeholder="e.g. john@example.com"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                    />
                  </div>
                </div>
              )}

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="block text-xs font-semibold text-slate-700">Password</label>
                  {tab === 'signin' && (
                    <button
                      type="button"
                      onClick={() => { setTab('forgot'); resetMessages(); }}
                      className="text-xs text-indigo-600 hover:text-indigo-800 font-medium cursor-pointer"
                    >
                      Forgot password?
                    </button>
                  )}
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md disabled:opacity-50 transition cursor-pointer"
              >
                {loading ? 'Processing...' : tab === 'register' ? 'Register & Sign In' : 'Sign In'}
              </button>
            </form>
          )}

          {/* FORGOT / RESET PASSWORD FORM */}
          {tab === 'forgot' && (
            <div className="space-y-4">
              {forgotStep === 1 ? (
                <form onSubmit={handleRequestResetCode} className="space-y-4">
                  <p className="text-xs text-slate-500 leading-relaxed">
                    Enter your registered username or email address. We will generate a secure reset token for your account.
                  </p>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Username or Email</label>
                    <div className="relative">
                      <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                      <input
                        type="text"
                        required
                        placeholder="e.g. nitesh or user@example.com"
                        value={forgotInput}
                        onChange={(e) => setForgotInput(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md disabled:opacity-50 transition cursor-pointer"
                  >
                    {loading ? 'Generating Code...' : 'Get Password Reset Code'}
                  </button>

                  <div className="text-center pt-2">
                    <button
                      type="button"
                      onClick={() => setForgotStep(2)}
                      className="text-xs text-indigo-600 hover:underline cursor-pointer"
                    >
                      Already have a reset code? Set new password →
                    </button>
                  </div>
                </form>
              ) : (
                <form onSubmit={handleResetPasswordSubmit} className="space-y-4">
                  <p className="text-xs text-slate-500 leading-relaxed">
                    Enter your reset code and set a new password (min. 6 characters).
                  </p>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Reset Code</label>
                    <div className="relative">
                      <KeyRound className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                      <input
                        type="text"
                        required
                        placeholder="e.g. 8A1B2C3D"
                        value={resetToken}
                        onChange={(e) => setResetToken(e.target.value.toUpperCase())}
                        className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-sm font-mono tracking-wider focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition uppercase"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">New Password</label>
                    <div className="relative">
                      <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                      <input
                        type="password"
                        required
                        minLength={6}
                        placeholder="Min. 6 characters"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md disabled:opacity-50 transition cursor-pointer"
                  >
                    {loading ? 'Updating Password...' : 'Save New Password'}
                  </button>

                  <div className="flex justify-between items-center pt-2">
                    <button
                      type="button"
                      onClick={() => setForgotStep(1)}
                      className="text-xs text-slate-500 hover:text-slate-700 inline-flex items-center cursor-pointer"
                    >
                      <ArrowLeft className="w-3.5 h-3.5 mr-1" />
                      Request code again
                    </button>
                    <button
                      type="button"
                      onClick={() => { setTab('signin'); resetMessages(); }}
                      className="text-xs text-indigo-600 hover:underline cursor-pointer"
                    >
                      Back to Sign In
                    </button>
                  </div>
                </form>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
