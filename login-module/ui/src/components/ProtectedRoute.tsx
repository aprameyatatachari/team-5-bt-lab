import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'ADMIN' | 'EMPLOYEE' | 'CUSTOMER';
  requiredPermissions?: string[];
  adminOnly?: boolean;
  customerOnly?: boolean;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requiredRole, 
  requiredPermissions,
  adminOnly = false,
  customerOnly = false
}) => {
  const { isAuthenticated, user, isLoading, hasRole, isAdmin, isCustomer } = useAuth();
  const location = useLocation();

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

  if (!isAuthenticated) {
    // Redirect to login page with return url
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check admin-only access
  if (adminOnly && !isAdmin()) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Admin Access Required</h1>
          <p className="text-gray-600 mb-4">
            This page requires administrator privileges.
          </p>
          <p className="text-sm text-gray-500">
            Your role: {user?.userType}
          </p>
        </div>
      </div>
    );
  }

  // Check customer-only access
  if (customerOnly && !isCustomer()) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Customer Access Required</h1>
          <p className="text-gray-600 mb-4">
            This page is only available to customers.
          </p>
          <p className="text-sm text-gray-500">
            Your role: {user?.userType}
          </p>
        </div>
      </div>
    );
  }

  // Check specific role requirements (legacy support)
  if (requiredRole && user?.userType !== requiredRole) {
    // User doesn't have required role
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Access Denied</h1>
          <p className="text-gray-600 mb-4">
            You don't have permission to access this page.
          </p>
          <p className="text-sm text-gray-500">
            Required role: {requiredRole}, Your role: {user?.userType}
          </p>
        </div>
      </div>
    );
  }

  // Check specific permission requirements
  if (requiredPermissions && requiredPermissions.length > 0) {
    const hasAllPermissions = requiredPermissions.every(permission => hasRole(permission));
    
    if (!hasAllPermissions) {
      const missingPermissions = requiredPermissions.filter(permission => !hasRole(permission));
      
      return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-2xl font-bold text-gray-900 mb-4">Insufficient Permissions</h1>
            <p className="text-gray-600 mb-4">
              You don't have the required permissions to access this page.
            </p>
            <p className="text-sm text-gray-500">
              Missing permissions: {missingPermissions.join(', ')}
            </p>
          </div>
        </div>
      );
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;
