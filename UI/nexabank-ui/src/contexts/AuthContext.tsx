import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';

interface User {
  userId: string;
  email: string;
  userType: string;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => void;
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
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));

  useEffect(() => {
    if (token) {
      // Decode JWT token to get user information
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({
          userId: payload.userId,
          email: payload.email,
          userType: payload.userType,
          roles: payload.roles || []
        });
      } catch (error) {
        console.error('Error decoding token:', error);
        logout();
      }
    }
  }, [token]);

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      if (response.ok) {
        const data = await response.json();
        const accessToken = data.data.accessToken;
        
        localStorage.setItem('token', accessToken);
        setToken(accessToken);
        
        // Decode the token to get user info
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        setUser({
          userId: payload.userId,
          email: payload.email,
          userType: payload.userType,
          roles: payload.roles || []
        });
        
        return true;
      } else {
        return false;
      }
    } catch (error) {
      console.error('Login error:', error);
      return false;
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role: string): boolean => {
    return user?.roles?.includes(role) || false;
  };

  const isAdmin = (): boolean => {
    return hasRole('ADMIN_FULL_ACCESS') || 
           hasRole('ADMIN_USER_MANAGEMENT') || 
           hasRole('ADMIN_SYSTEM_CONFIG') ||
           hasRole('ADMIN_REPORTS');
  };

  const isCustomer = (): boolean => {
    return user?.userType === 'CUSTOMER' || hasRole('CUSTOMER_VIEW');
  };

  const value: AuthContextType = {
    user,
    token,
    login,
    logout,
    hasRole,
    isAdmin,
    isCustomer,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};