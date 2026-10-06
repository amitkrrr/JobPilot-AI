import React, { useState } from 'react';
import { useAuthStore } from '../stores/authStore';
import api from '../services/api';
import { Mail, Eye, EyeOff, User, ArrowRight, ShieldAlert } from 'lucide-react';

interface AuthPageProps {
  initialView?: 'login' | 'register';
  onNavigateBack?: () => void;
}

export default function AuthPage({ initialView = 'login', onNavigateBack }: AuthPageProps) {
  const [isLogin, setIsLogin] = useState(initialView === 'login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loginStore = useAuthStore((state) => state.login);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    if (!isLogin && password !== confirmPassword) {
      setError('Passwords do not match');
      setLoading(false);
      return;
    }

    try {
      if (isLogin) {
        // Handle Login
        const response = await api.post('/auth/login', { email, password });
        const data = response.data;
        loginStore(data.token, email, data.name || email);
      } else {
        // Handle Registration
        await api.post('/auth/register', { name, email, password });
        // Automatically login after successful registration
        const loginResponse = await api.post('/auth/login', { email, password });
        const data = loginResponse.data;
        loginStore(data.token, email, name);
      }
    } catch (err: any) {
      console.error(err);
      setError(
        err.response?.data?.message || 
        err.response?.data?.error || 
        'An unexpected error occurred. Please check your credentials.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-12 bg-slate-50 relative overflow-hidden font-sans">
      {/* Decorative Wave Accents at bottom corners matching mockup! */}
      <div className="absolute left-0 bottom-0 w-48 h-48 bg-blue-100 rounded-tr-[100%] opacity-40 -z-10"></div>
      <div className="absolute right-0 bottom-0 w-72 h-36 bg-blue-200/50 rounded-tl-[100%] opacity-50 -z-10"></div>

      {/* Auth Card container */}
      <div className="w-full max-w-md space-y-8 bg-white px-8 py-10 rounded-3xl shadow-xl border border-slate-100 relative">
        
        {/* Back Button (if landed from main homepage) */}
        {onNavigateBack && (
          <button 
            onClick={onNavigateBack}
            className="absolute left-6 top-6 text-xs font-bold text-slate-400 hover:text-slate-600"
          >
            ← Back
          </button>
        )}

        {/* Logo Header */}
        <div className="text-center pt-2">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-xl bg-blue-600 text-white shadow-lg shadow-blue-200 transform -rotate-12">
            <svg className="h-7 w-7 fill-current" viewBox="0 0 24 24">
              <path d="M2 21l21-9L2 3v7l15 2-15 2v7z" />
            </svg>
          </div>
          <h2 className="mt-4 text-3xl font-extrabold tracking-tight text-[#0f172a]">
            Job<span className="text-blue-600">Pilot</span>
          </h2>
          
          <h3 className="mt-6 text-2xl font-extrabold text-slate-900 leading-tight">
            {isLogin ? 'Welcome back 👋' : 'Create your account'}
          </h3>
          <p className="mt-2 text-sm text-slate-500">
            {isLogin 
              ? 'Login to your account to continue your job search journey' 
              : 'Join JobPilot and take the next step in your career'}
          </p>
        </div>

        {error && (
          <div className="flex items-start gap-3 rounded-xl bg-rose-50 p-4 text-xs font-medium text-rose-800 border border-rose-100">
            <ShieldAlert className="h-4 w-4 flex-shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form className="space-y-4" onSubmit={handleSubmit}>
          {/* Create Account Fields */}
          {!isLogin && (
            <div>
              <label className="text-xs font-bold text-slate-600 tracking-wide uppercase">
                Full Name
              </label>
              <div className="relative mt-1.5">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                  <User className="h-5 w-5" />
                </div>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Amit Kumar"
                  className="block w-full rounded-xl border border-slate-200 py-3 pl-10 pr-3 text-slate-900 placeholder-slate-400 bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
                />
              </div>
            </div>
          )}

          {/* Email field */}
          <div>
            <label className="text-xs font-bold text-slate-600 tracking-wide uppercase">
              Email
            </label>
            <div className="relative mt-1.5">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                <Mail className="h-5 w-5" />
              </div>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="block w-full rounded-xl border border-slate-200 py-3 pl-10 pr-3 text-slate-900 placeholder-slate-400 bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
              />
            </div>
          </div>

          {/* Password Field */}
          <div>
            <label className="text-xs font-bold text-slate-600 tracking-wide uppercase">
              Password
            </label>
            <div className="relative mt-1.5">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                <svg className="h-5 w-5 stroke-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </div>
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="block w-full rounded-xl border border-slate-200 py-3 pl-10 pr-10 text-slate-900 placeholder-slate-400 bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 hover:text-slate-600"
              >
                {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
          </div>

          {/* Confirm Password Field (Register Only) */}
          {!isLogin && (
            <div>
              <label className="text-xs font-bold text-slate-600 tracking-wide uppercase">
                Confirm Password
              </label>
              <div className="relative mt-1.5">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                  <svg className="h-5 w-5 stroke-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  className="block w-full rounded-xl border border-slate-200 py-3 pl-10 pr-10 text-slate-900 placeholder-slate-400 bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 hover:text-slate-600"
                >
                  {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>
          )}

          {/* Remember Me / Forgot Password Layout (Login Only) */}
          {isLogin && (
            <div className="flex items-center justify-between text-xs pt-1">
              <label className="flex items-center gap-2 cursor-pointer text-slate-600 font-semibold select-none">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="h-4.5 w-4.5 rounded border-slate-300 text-blue-600 focus:ring-blue-500 focus:ring-offset-0 cursor-pointer"
                />
                Remember me
              </label>
              <button
                type="button"
                onClick={() => setError('Password recovery is coming soon! Please register a new account if you forgot your credentials.')}
                className="font-bold text-blue-600 hover:text-blue-700"
              >
                Forgot password?
              </button>
            </div>
          )}

          {/* Submit Action Button */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={loading}
              className="group relative flex w-full justify-center items-center gap-2 rounded-xl bg-blue-600 py-3.5 px-4 text-base font-bold text-white hover:bg-blue-700 shadow-md shadow-blue-200 focus:outline-none transition-colors disabled:opacity-50"
            >
              {loading ? (
                <span className="flex items-center gap-2">
                  <svg className="h-4 w-4 animate-spin text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  Processing...
                </span>
              ) : isLogin ? (
                <span className="flex items-center gap-2">
                  Login <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
                </span>
              ) : (
                <span className="flex items-center gap-2">
                  Create Account <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
                </span>
              )}
            </button>
          </div>
        </form>

        {/* OR Divider matching mockup */}
        <div className="relative my-6 flex py-1 items-center justify-center">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-100"></div>
          </div>
          <span className="relative bg-white px-4 text-xs font-bold text-slate-400 uppercase tracking-widest">
            OR
          </span>
        </div>

        {/* Google Oauth Button */}
        <div>
          <button
            type="button"
            onClick={() => setError('Google Sign-In is coming soon! Please register/login using your email and password above.')}
            className="flex w-full justify-center items-center gap-3 rounded-xl border border-slate-200/80 bg-white py-3 px-4 text-sm font-bold text-slate-700 hover:bg-slate-50 transition-colors shadow-sm"
          >
            {/* Custom Google SVG Icon */}
            <svg className="h-5 w-5 flex-shrink-0" viewBox="0 0 24 24" fill="none">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" fill="#EA4335" />
            </svg>
            Continue with Google
          </button>
        </div>

        {/* Footer navigation links */}
        <div className="text-center pt-2">
          {isLogin ? (
            <p className="text-sm text-slate-500 font-semibold">
              Don't have an account?{' '}
              <button
                onClick={() => { setIsLogin(false); setError(''); }}
                className="font-bold text-blue-600 hover:text-blue-700 ml-1 select-none"
              >
                Sign up
              </button>
            </p>
          ) : (
            <p className="text-sm text-slate-500 font-semibold">
              Already have an account?{' '}
              <button
                onClick={() => { setIsLogin(true); setError(''); }}
                className="font-bold text-blue-600 hover:text-blue-700 ml-1 select-none"
              >
                Login
              </button>
            </p>
          )}
        </div>

      </div>
    </div>
  );
}
