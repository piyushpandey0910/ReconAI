import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Sparkles, AlertCircle, Loader2 } from 'lucide-react';

interface RegisterPageProps {
  onNavigateToLogin: () => void;
}

export const RegisterPage: React.FC<RegisterPageProps> = ({ onNavigateToLogin }) => {
  const { signup } = useAuth();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('ROLE_ANALYST');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !email || !password) return;
    setLoading(true);
    setError(null);
    try {
      await signup(username, email, password, role);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center space-y-3">
        <div className="h-12 w-12 mx-auto rounded-2xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 flex items-center justify-center shadow-xl shadow-blue-500/20">
          <Sparkles className="h-6 w-6 text-white" />
        </div>
        <h2 className="text-2xl font-extrabold text-white tracking-tight">
          Create Controller Account
        </h2>
        <p className="text-xs text-slate-400">
          Sign up to manage financial reconciliation pipelines
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-slate-900/90 border border-slate-800 py-8 px-6 shadow-2xl rounded-2xl sm:px-10 space-y-5">
          {error && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-300">Username</label>
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. john_controller"
                className="mt-1 w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300">Email Address</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="john@finance.com"
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
                placeholder="••••••••"
                className="mt-1 w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300">Role Selection</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="mt-1 w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2.5 text-xs text-white focus:outline-none focus:border-blue-500"
              >
                <option value="ROLE_ADMIN">ADMIN (Full Pipeline & Upload Control)</option>
                <option value="ROLE_ANALYST">ANALYST (Reconciliation & Manual Review)</option>
                <option value="ROLE_VIEWER">VIEWER (Read-Only Dashboard & Chat)</option>
              </select>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs transition shadow-lg shadow-blue-600/20 active:scale-95 flex items-center justify-center space-x-2"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>Complete Registration</span>}
            </button>
          </form>

          <div className="text-center text-xs text-slate-400">
            Already have an account?{' '}
            <button
              onClick={onNavigateToLogin}
              className="text-blue-400 hover:underline font-semibold"
            >
              Sign In
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
