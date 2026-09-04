import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { Loader2 } from 'lucide-react';

const AppContent: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const [authView, setAuthView] = useState<'login' | 'register'>('login');

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center text-blue-500">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return authView === 'login' ? (
      <LoginPage onNavigateToRegister={() => setAuthView('register')} />
    ) : (
      <RegisterPage onNavigateToLogin={() => setAuthView('login')} />
    );
  }

  return <DashboardPage />;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;
