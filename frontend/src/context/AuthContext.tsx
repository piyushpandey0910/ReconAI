import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Role } from '../types';
import { authApi, setTokens } from '../api/client';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (u: string, p: string) => Promise<void>;
  signup: (u: string, e: string, p: string, r: string) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
  isAnalyst: boolean;
  isViewer: boolean;
  canReconcile: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    // Check if session can be restored from memory/storage
    const storedUser = sessionStorage.getItem('finance_user');
    const storedToken = sessionStorage.getItem('finance_token');
    const storedRefresh = sessionStorage.getItem('finance_refresh');

    if (storedUser && storedToken) {
      try {
        const parsed = JSON.parse(storedUser);
        setUser(parsed);
        setTokens(storedToken, storedRefresh);
      } catch (e) {
        sessionStorage.clear();
      }
    }
    setIsLoading(false);

    const handleLogout = () => {
      logout();
    };
    window.addEventListener('auth:logout', handleLogout);
    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  const login = async (usernameOrEmail: string, password: string) => {
    const res = await authApi.login(usernameOrEmail, password);
    const loggedInUser: User = {
      id: 0,
      username: res.username,
      email: res.email,
      role: res.role as Role,
    };
    setUser(loggedInUser);
    sessionStorage.setItem('finance_user', JSON.stringify(loggedInUser));
    sessionStorage.setItem('finance_token', res.accessToken);
    sessionStorage.setItem('finance_refresh', res.refreshToken);
  };

  const signup = async (username: string, email: string, password: string, role: string) => {
    const res = await authApi.signup(username, email, password, role);
    const newUser: User = {
      id: 0,
      username: res.username,
      email: res.email,
      role: res.role as Role,
    };
    setUser(newUser);
    sessionStorage.setItem('finance_user', JSON.stringify(newUser));
    sessionStorage.setItem('finance_token', res.accessToken);
    sessionStorage.setItem('finance_refresh', res.refreshToken);
  };

  const logout = () => {
    authApi.logout();
    setUser(null);
    sessionStorage.clear();
  };

  const role = user?.role;
  const isAdmin = role === 'ROLE_ADMIN';
  const isAnalyst = role === 'ROLE_ANALYST';
  const isViewer = role === 'ROLE_VIEWER';
  const canReconcile = isAdmin || isAnalyst;

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        signup,
        logout,
        isAdmin,
        isAnalyst,
        isViewer,
        canReconcile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
