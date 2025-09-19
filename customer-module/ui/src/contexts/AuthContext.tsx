import React, { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { authAPI } from '../services/api';
import type { User, LoginRequest, RegisterRequest } from '../services/api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (userData: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  logoutAllDevices: () => Promise<void>;
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
  const [isLoading, setIsLoading] = useState(true);

  // Check if user is authenticated on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        // First check URL parameters for tokens (from login module redirect)
        const urlParams = new URLSearchParams(window.location.search);
        const urlToken = urlParams.get('token');
        const urlRefresh = urlParams.get('refresh');
        const urlUser = urlParams.get('user');
        
        if (urlToken && urlRefresh && urlUser) {
          // Store tokens from URL parameters
          localStorage.setItem('accessToken', urlToken);
          localStorage.setItem('refreshToken', urlRefresh);
          
          try {
            const userData = JSON.parse(urlUser);
            setUser(userData);
            
            // Clean URL by removing the parameters
            const newUrl = window.location.origin + window.location.pathname;
            window.history.replaceState({}, '', newUrl);
            
            return; // Exit early, we have the user data
          } catch (e) {
            console.error('Failed to parse user data from URL:', e);
          }
        }
        
        // If no URL tokens, check localStorage
        const token = localStorage.getItem('accessToken');
        if (token) {
          const response = await authAPI.getCurrentUser();
          if (response.success) {
            setUser(response.data);
          } else {
            // Token is invalid, clear it
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
          }
        }
      } catch (error) {
        console.error('Auth check failed:', error);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authAPI.login(credentials);
      if (response.success) {
        const { accessToken, refreshToken, user } = response.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
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
      // Get the token before making the API call
      const token = localStorage.getItem('accessToken');
      
      if (token) {
        try {
          await authAPI.logout();
          console.log('Logout API call successful');
        } catch (error) {
          console.error('Logout API call failed:', error);
          // Continue with local logout even if API fails
        }
      }
    } catch (error) {
      console.error('Logout process failed:', error);
    } finally {
      // Always clear local storage and state
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      setUser(null);
      
      // Force a page refresh to ensure clean state
      console.log('User logged out, clearing authentication state');
    }
  };

  const logoutAllDevices = async () => {
    try {
      const token = localStorage.getItem('accessToken');
      
      if (token) {
        try {
          await authAPI.logoutAllDevices();
          console.log('Logout all devices API call successful');
        } catch (error) {
          console.error('Logout all devices API call failed:', error);
          // Continue with local logout even if API fails
        }
      }
    } catch (error) {
      console.error('Logout all devices process failed:', error);
    } finally {
      // Always clear local storage and state
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      setUser(null);
      
      // Force a page refresh to ensure clean state
      console.log('User logged out from all devices, clearing authentication state');
    }
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    register,
    logout,
    logoutAllDevices,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
