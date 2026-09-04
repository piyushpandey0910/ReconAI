import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Sparkles, Database, MessageSquare } from 'lucide-react';

interface NavbarProps {
  onOpenUpload: () => void;
  onToggleChat: () => void;
  isChatOpen: boolean;
  unreadCount?: number;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenUpload, onToggleChat, isChatOpen }) => {
  const { user, logout, isAdmin, isAnalyst, canReconcile } = useAuth();

  const getRoleBadge = () => {
    if (isAdmin) return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-900/60 text-purple-300 border border-purple-500/40">ADMIN</span>;
    if (isAnalyst) return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-900/60 text-blue-300 border border-blue-500/40">ANALYST</span>;
    return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-800 text-slate-300 border border-slate-700">VIEWER (Read-Only)</span>;
  };

  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
            <Sparkles className="h-5 w-5 text-white" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-lg font-bold bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
                ReconAI
              </h1>
              <span className="text-[10px] uppercase tracking-wider font-mono font-bold px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-400 border border-blue-500/30">
                v1.0
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">Multi-Source Reconciliation Engine</p>
          </div>
        </div>

        {/* Actions & User */}
        <div className="flex items-center space-x-3">
          {canReconcile && (
            <button
              onClick={onOpenUpload}
              className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition shadow-md shadow-blue-600/20 active:scale-95"
            >
              <Database className="h-4 w-4" />
              <span className="hidden sm:inline">Ingest Datasets</span>
            </button>
          )}

          {/* AI Chat Drawer Button */}
          <button
            onClick={onToggleChat}
            className={`inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-lg text-sm font-medium transition border ${
              isChatOpen
                ? 'bg-purple-600 text-white border-purple-500 shadow-md shadow-purple-600/30'
                : 'bg-slate-800 hover:bg-slate-700 text-purple-300 border-purple-500/30'
            }`}
          >
            <MessageSquare className="h-4 w-4 text-purple-400" />
            <span className="hidden sm:inline">Ask AI Agent</span>
          </button>

          {/* User & Role Badge */}
          <div className="flex items-center space-x-2 pl-2 border-l border-slate-800">
            {getRoleBadge()}
            <div className="flex items-center space-x-2 px-2 py-1 rounded-lg bg-slate-800/60 border border-slate-700/60 text-xs text-slate-300">
              <User className="h-3.5 w-3.5 text-slate-400" />
              <span className="font-medium hidden md:inline">{user?.username}</span>
            </div>
            <button
              onClick={logout}
              title="Sign Out"
              className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-red-400 transition"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
