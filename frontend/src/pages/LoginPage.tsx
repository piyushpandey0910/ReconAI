import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Sparkles, AlertCircle, Loader2 } from 'lucide-react';

interface LoginPageProps {
  onNavigateToRegister: () => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onNavigateToRegister }) => {
  const { login } = useAuth();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) return;
    setLoading(true);
    setError(null);
    try {
      await login(username, password);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid credentials or connection error');
    } finally {
      setLoading(false);
    }
  };

  const fillQuickRole = (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center space-y-3">
        <div className="h-12 w-12 mx-auto rounded-2xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 flex items-center justify-center shadow-xl shadow-blue-500/20">
          <Sparkles className="h-6 w-6 text-white" />
        </div>
        <h2 className="text-2xl font-extrabold text-white tracking-tight">
          ReconAI
        </h2>
        <p className="text-xs text-slate-400">
          Multi-Source Reconciliation Platform with Two-Pass AI Engine
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-slate-900/90 border border-slate-800 py-8 px-6 shadow-2xl rounded-2xl sm:px-10 space-y-6">
          {error && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-300">Username or Email</label>
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="mt-1 w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300">Password</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs transition shadow-lg shadow-blue-600/20 active:scale-95 flex items-center justify-center space-x-2"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>Sign In to Controller</span>}
            </button>
          </form>

          {/* Quick Login Role Pills for immediate testing */}
          <div className="pt-2 border-t border-slate-800 space-y-2">
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block text-center">
              One-Click Demo Roles
            </span>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => fillQuickRole('admin', 'admin123')}
                className="px-2.5 py-1.5 rounded-lg bg-purple-900/30 hover:bg-purple-900/50 text-purple-300 border border-purple-500/30 text-[11px] font-medium transition text-center"
              >
                ADMIN
              </button>
              <button
                type="button"
                onClick={() => fillQuickRole('analyst', 'analyst123')}
                className="px-2.5 py-1.5 rounded-lg bg-blue-900/30 hover:bg-blue-900/50 text-blue-300 border border-blue-500/30 text-[11px] font-medium transition text-center"
              >
                ANALYST
              </button>
              <button
                type="button"
                onClick={() => fillQuickRole('viewer', 'viewer123')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 text-[11px] font-medium transition text-center"
              >
                VIEWER
              </button>
            </div>
          </div>

          <div className="text-center text-xs text-slate-400">
            Don't have an account?{' '}
            <button
              onClick={onNavigateToRegister}
              className="text-blue-400 hover:underline font-semibold"
            >
              Create Account
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
