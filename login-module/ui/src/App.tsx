import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import { useAuth } from './contexts/AuthContext';

const App: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // If authenticated, redirect to customer portal
  const redirectToDashboard = () => {
    if (!user) return;
    
    // Get current tokens
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');
    
    if (accessToken && refreshToken) {
      // Create URL with tokens as parameters
      const params = new URLSearchParams({
        token: accessToken,
        refresh: refreshToken,
        user: JSON.stringify(user)
      });
      
      // Redirect to appropriate customer module based on role
      const customerModuleUrl = `http://localhost:5174?${params.toString()}`;
      window.location.href = customerModuleUrl;
    }
  };

  React.useEffect(() => {
    if (isAuthenticated && user) {
      redirectToDashboard();
    }
  }, [isAuthenticated, user]);

  return (
    <Routes>
      {/* Public routes */}
      <Route 
        path="/login" 
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} 
      />
      <Route 
        path="/register" 
        element={isAuthenticated ? <Navigate to="/" replace /> : <RegisterPage />} 
      />
      
      {/* Default redirect */}
      <Route
        path="/"
        element={
          isAuthenticated ? (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <p className="text-gray-600">Redirecting to dashboard...</p>
              </div>
            </div>
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
      
      {/* 404 route */}
      <Route
        path="*"
        element={
          <div className="min-h-screen bg-gray-50 flex items-center justify-center">
            <div className="text-center">
              <h1 className="text-4xl font-bold text-gray-900 mb-4">404</h1>
              <p className="text-gray-600 mb-4">Page not found</p>
              <a href="/" className="text-blue-600 hover:underline">
                Go back home
              </a>
            </div>
          </div>
        }
      />
    </Routes>
  );
};

export default App;
