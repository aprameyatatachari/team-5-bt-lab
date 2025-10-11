import React, { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { authAPI } from '../services/api';
import type { User, LoginRequest, RegisterRequest } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isLoggingOut: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (userData: RegisterRequest) => Promise<void>;
  logout: (callback?: () => void) => Promise<void>;
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
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  // Check if user is authenticated on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        // Check for logout or session expired flags from URL
        const urlParams = new URLSearchParams(window.location.search);
        const loggedOut = urlParams.get('loggedOut') === '1';
        const sessionExpired = urlParams.get('session') === 'expired';
        
        if (loggedOut || sessionExpired) {
          // Clear everything and don't attempt to authenticate
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          setUser(null);
          setToken(null);
          
          // Clean URL
          const newUrl = window.location.origin + window.location.pathname;
          window.history.replaceState({}, '', newUrl);
          
          setIsLoading(false);
          return;
        }
        
        // First check URL parameters for tokens (from login module redirect)
        const urlToken = urlParams.get('token');
        const urlRefresh = urlParams.get('refresh');
        const urlUser = urlParams.get('user');
        
        if (urlToken && urlRefresh && urlUser) {
          // Store tokens from URL parameters
          localStorage.setItem('accessToken', urlToken);
          localStorage.setItem('refreshToken', urlRefresh);
          setToken(urlToken);
          
          try {
            const userData = JSON.parse(urlUser);
            setUser(userData);
            
            // Clean URL by removing the parameters
            const newUrl = window.location.origin + window.location.pathname;
            window.history.replaceState({}, '', newUrl);
            
            setIsLoading(false); // CRITICAL: Set loading false before return
            return; // Exit early, we have the user data
          } catch (e) {
            console.error('Failed to parse user data from URL:', e);
          }
        }
        
        // If no URL tokens, check localStorage
        const accessToken = localStorage.getItem('accessToken');
        if (accessToken) {
          setToken(accessToken);

          // Pre-populate user with basic info decoded from JWT (best-effort fallback)
          let decodedUserData: any = null;
          try {
            const payload = JSON.parse(atob(accessToken.split('.')[1]));
            if (payload) {
              // Build minimal user from JWT claims
              decodedUserData = {
                userId: payload.userId || payload.sub,
                email: payload.sub || payload.email,
                firstName: payload.firstName || '',
                lastName: payload.lastName || '',
                userType: payload.userType || 'CUSTOMER',
                status: 'ACTIVE',
                phoneNumber: '',
                roles: payload.roles || []
              };
            }
          } catch (jwtError) {
            console.warn('Could not decode JWT token:', jwtError);
          }
          
          // Set the decoded user as a fallback so UI doesn't think user is logged out
          if (decodedUserData) {
            setUser(decodedUserData as User);
          }

          // Try to resolve the full user via auth service (enhances the fallback)
          try {
            const response = await authAPI.getCurrentUser();
            if (response.success) {
              setUser(response.data);
            } else {
              console.warn('getCurrentUser returned success=false; keeping decoded user from token');
            }
          } catch (err: any) {
            // Only clear tokens on definitive auth failure (401 means refresh also failed)
            const status = err?.response?.status;
            if (status === 401) {
              console.warn('getCurrentUser returned 401 after refresh; clearing tokens and redirecting');
              localStorage.removeItem('accessToken');
              localStorage.removeItem('refreshToken');
              setToken(null);
              setUser(null);
            } else {
              console.warn('getCurrentUser failed (network/server error); keeping decoded user:', err?.message || err);
              // Keep the decoded user from JWT so the app remains usable
            }
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

  const logout = async (callback?: () => void) => {
    console.log('Starting logout process...');
    setIsLoggingOut(true);
    
    try {
      // Try to call server logout while we still have tokens (needs auth header)
      await authAPI.logout();
      console.log('Server-side logout successful.');
    } catch (error) {
      console.error('Server-side logout failed:', error);
      // Continue with logout even if server call fails
    }
    
    // Clear all local storage and state immediately
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
    setToken(null);
    setIsLoggingOut(false);
    
    console.log('Client-side logout complete.');

    // Execute the callback immediately if provided
    if (callback) {
      callback();
    }
  };

  const logoutAllDevices = async () => {
    console.log('Starting logout all devices process...');
    
    try {
      // Call server logout-all while we still have tokens
      await authAPI.logoutAllDevices();
      console.log('Logout all devices API call successful');
    } catch (error) {
      console.error('Logout all devices API call failed:', error);
      // Continue with local logout even if API fails
    } finally {
      // Always clear local storage and state
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      setUser(null);
      setToken(null);
      
      console.log('User logged out from all devices, authentication state cleared');
      
      // Redirect to login module with a flag
      window.location.href = 'http://localhost:5173?loggedOut=1';
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
    isLoggingOut,
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
