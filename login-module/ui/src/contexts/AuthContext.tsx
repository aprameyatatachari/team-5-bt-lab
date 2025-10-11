import React, { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { authAPI } from '../services/api';
import type { User, LoginRequest, RegisterRequest } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (userData: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  logoutAllDevices: () => Promise<void>;
  hasRole: (role: string) => boolean;
  isAdmin: () => boolean;
  isCustomer: () => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('accessToken'));
  const [isLoading, setIsLoading] = useState(true);

  // Check if user is authenticated on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        // Honor logout/session flags from customer app and force-clear
        const params = new URLSearchParams(window.location.search);
        if (params.get('loggedOut') === '1' || params.get('session') === 'expired') {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          setUser(null);
          setToken(null);
          const cleanUrl = window.location.origin + window.location.pathname;
          window.history.replaceState({}, '', cleanUrl);
        }

        const accessToken = localStorage.getItem('accessToken');
        if (accessToken) {
          setToken(accessToken);
          
          // Try to decode JWT token to get role information
          try {
            const payload = JSON.parse(atob(accessToken.split('.')[1]));
            if (payload.roles) {
              // If we have role info in token, create enhanced user object
              const enhancedUser = {
                ...user,
                roles: payload.roles || []
              };
              setUser(enhancedUser as User);
            }
          } catch (jwtError) {
            console.warn('Could not decode JWT token for roles:', jwtError);
          }
          
          const response = await authAPI.getCurrentUser();
          if (response.success) {
            setUser(response.data);
          } else {
            // Token is invalid, clear it
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            setToken(null);
          }
        }
      } catch (error) {
        console.error('Auth check failed:', error);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        setToken(null);
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  // Watch for localStorage changes (when redirectToDashboard clears tokens)
  useEffect(() => {
    const handleStorageChange = () => {
      const accessToken = localStorage.getItem('accessToken');
      if (!accessToken && token) {
        // Tokens were cleared, reset auth state
        setUser(null);
        setToken(null);
      }
    };

    // Check immediately
    handleStorageChange();

    // Set up interval to check periodically (since same-tab localStorage changes don't fire events)
    const interval = setInterval(handleStorageChange, 100);

    return () => clearInterval(interval);
  }, [token]);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authAPI.login(credentials);
      if (response.success) {
        const { accessToken, refreshToken, user } = response.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        setToken(accessToken);
        setUser(user);
      } else {
        throw new Error(response.message);
      }
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const register = async (userData: RegisterRequest) => {
    try {
      const response = await authAPI.register(userData);
      if (response.success) {
        const { accessToken, refreshToken, user } = response.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        setToken(accessToken);
        setUser(user);
      } else {
        throw new Error(response.message);
      }
    } catch (error) {
      console.error('Registration failed:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      console.error('Logout API call failed:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setToken(null);
      setUser(null);
    }
  };

  const logoutAllDevices = async () => {
    try {
      await authAPI.logoutAllDevices();
    } catch (error) {
      console.error('Logout all devices failed:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setToken(null);
      setUser(null);
    }
  };

  const hasRole = (role: string): boolean => {
    if (!user) return false;
    // Check if user has roles property (enhanced version)
    if ('roles' in user && Array.isArray(user.roles)) {
      return user.roles.includes(role);
    }
    // Fallback: check basic role based on userType
    if (role === 'ADMIN_FULL_ACCESS' && user.userType === 'ADMIN') return true;
    if (role === 'CUSTOMER_VIEW' && user.userType === 'CUSTOMER') return true;
    return false;
  };

  const isAdmin = (): boolean => {
    return hasRole('ADMIN_FULL_ACCESS') || 
           hasRole('ADMIN_USER_MANAGEMENT') || 
           hasRole('ADMIN_SYSTEM_CONFIG') ||
           hasRole('ADMIN_REPORTS') ||
           user?.userType === 'ADMIN';
  };

  const isCustomer = (): boolean => {
    return user?.userType === 'CUSTOMER' || hasRole('CUSTOMER_VIEW');
  };

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: !!user,
    isLoading,
    login,
    register,
    logout,
    logoutAllDevices,
    hasRole,
    isAdmin,
    isCustomer,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
