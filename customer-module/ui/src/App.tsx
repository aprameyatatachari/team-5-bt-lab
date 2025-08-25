import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import DashboardPage from './pages/DashboardPage';
import AdminDashboard from './pages/AdminDashboard';
import CustomerDashboard from './pages/CustomerDashboard';
import TransferPage from './pages/TransferPage';
import ProtectedRoute from './components/ProtectedRoute';
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

  // If not authenticated, redirect to login service
  if (!isAuthenticated) {
    window.location.href = 'http://localhost:5173'; // Login module port
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Redirecting to login...</p>
        </div>
      </div>
    );
  }

  // Custom dashboard routing component
  const DashboardRouter: React.FC = () => {
    if (!user) return <Navigate to="/" replace />;
    
    // Route to appropriate dashboard based on user role
    switch (user.userType) {
      case 'ADMIN':
        return <AdminDashboard />;
      case 'EMPLOYEE':
        return <AdminDashboard />; // For now, employees use admin dashboard
      case 'CUSTOMER':
      default:
        return <CustomerDashboard />;
    }
  };

  return (
    <Routes>
      {/* Protected routes */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardRouter />
          </ProtectedRoute>
        }
      />
      
      {/* Legacy route - keep the old dashboard for backwards compatibility */}
      <Route
        path="/old-dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/transfers"
        element={
          <ProtectedRoute>
            <TransferPage />
          </ProtectedRoute>
        }
      />
      
      {/* Banking features */}
      <Route
        path="/accounts"
        element={
          <ProtectedRoute>
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
              <div className="text-center">
                <h1 className="text-2xl font-bold text-gray-900 mb-4">Account Management</h1>
                <p className="text-gray-600">Account details and management features coming soon...</p>
              </div>
            </div>
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/services"
        element={
          <ProtectedRoute>
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
              <div className="text-center">
                <h1 className="text-2xl font-bold text-gray-900 mb-4">Banking Services</h1>
                <p className="text-gray-600">Additional banking services coming soon...</p>
              </div>
            </div>
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/support"
        element={
          <ProtectedRoute>
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
              <div className="text-center">
                <h1 className="text-2xl font-bold text-gray-900 mb-4">Customer Support</h1>
                <p className="text-gray-600">Support features coming soon...</p>
              </div>
            </div>
          </ProtectedRoute>
        }
      />
      
      {/* Admin routes */}
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      
      {/* Employee routes */}
      <Route
        path="/employee/*"
        element={
          <ProtectedRoute requiredRole="EMPLOYEE">
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      
      {/* Customer routes */}
      <Route
        path="/customer/*"
        element={
          <ProtectedRoute requiredRole="CUSTOMER">
            <CustomerDashboard />
          </ProtectedRoute>
        }
      />
      
      {/* Default redirect */}
      <Route
        path="/"
        element={<Navigate to="/dashboard" replace />}
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
