import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import { useAuth } from './contexts/AuthContext';

const App: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();

  // Check for logout/session flags that should suppress auto-redirect
  const params = new URLSearchParams(window.location.search);
  const suppressRedirect = params.get('loggedOut') === '1' || params.get('session') === 'expired';

  // Move useEffect to top level to avoid hooks order issues
  React.useEffect(() => {
    if (!suppressRedirect && isAuthenticated && user) {
      // Delay to show welcome message and ensure auth state is stable
      const timer = setTimeout(() => {
        redirectToDashboard();
      }, 1500); // Increased from 100ms to 1.5s to show welcome message
      
      return () => clearTimeout(timer);
    }
  }, [isAuthenticated, user, suppressRedirect]);

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
      
      // Clear the login module's auth state before redirecting
      // This ensures when user comes back, login module shows login page
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      
      // Redirect to appropriate customer module based on role - use replace to clear this page
      const customerModuleUrl = `http://localhost:5174?${params.toString()}`;
      window.location.replace(customerModuleUrl);
    }
  };

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
            // Show welcome screen while redirecting
            <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
              <div className="text-center bg-white rounded-lg shadow-lg p-8 max-w-md">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">
                  Welcome back, {user?.firstName || 'User'}!
                </h2>
                <p className="text-gray-600">
                  Taking you to your dashboard...
                </p>
              </div>
            </div>
          ) : (
            // Check for logout/session flags to show appropriate message
            (() => {
              const loggedOut = params.get('loggedOut') === '1';
              const sessionExpired = params.get('session') === 'expired';
              
              if (loggedOut || sessionExpired) {
                return (
                  <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                    <div className="text-center max-w-md">
                      <h1 className="text-2xl font-bold text-gray-900 mb-4">
                        {loggedOut ? 'Logged Out Successfully' : 'Session Expired'}
                      </h1>
                      <p className="text-gray-600 mb-6">
                        {loggedOut 
                          ? 'You have been successfully logged out from all services.' 
                          : 'Your session has expired. Please log in again to continue.'}
                      </p>
                      <button
                        onClick={() => {
                          // Clean URL and navigate to login
                          window.history.replaceState({}, '', '/login');
                          window.location.pathname = '/login';
                        }}
                        className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors"
                      >
                        Continue to Login
                      </button>
                    </div>
                  </div>
                );
              }
              
              return <Navigate to="/login" replace />;
            })()
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
