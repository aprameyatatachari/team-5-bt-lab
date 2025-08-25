import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import BankingHeader from '../components/layout/BankingHeader';
import AdminStats from '../components/admin/AdminStats';
import UserManagement from '../components/admin/UserManagement';

const AdminDashboard: React.FC = () => {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <BankingHeader />
      
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Section */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Admin Dashboard
          </h1>
          <p className="text-gray-600">
            Welcome back, {user?.firstName}! Manage bank operations and user accounts.
          </p>
        </div>

        {/* Admin Stats Overview */}
        <div className="mb-8">
          <AdminStats />
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 gap-8">
          {/* User Management */}
          <div>
            <UserManagement />
          </div>

          {/* System Overview */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h2 className="text-xl font-semibold mb-4">System Overview</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-blue-50 p-4 rounded-lg">
                <h3 className="font-medium text-blue-900">User Management</h3>
                <p className="text-sm text-blue-700 mt-1">Create, edit, and manage user accounts</p>
              </div>
              <div className="bg-green-50 p-4 rounded-lg">
                <h3 className="font-medium text-green-900">System Monitoring</h3>
                <p className="text-sm text-green-700 mt-1">Monitor system health and performance</p>
              </div>
              <div className="bg-purple-50 p-4 rounded-lg">
                <h3 className="font-medium text-purple-900">Analytics & Reports</h3>
                <p className="text-sm text-purple-700 mt-1">View detailed system analytics</p>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default AdminDashboard;
