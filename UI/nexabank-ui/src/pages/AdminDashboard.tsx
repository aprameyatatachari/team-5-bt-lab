import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import BankingHeader from '../components/layout/BankingHeader';
import AdminStats from '../components/admin/AdminStats';
import UserManagement from '../components/admin/UserManagement';
import AccountManagement from '../components/admin/AccountManagement';

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
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
          {/* User Management */}
          <div>
            <UserManagement />
          </div>

          {/* Account Management */}
          <div>
            <AccountManagement />
          </div>
        </div>
      </main>
    </div>
  );
};

export default AdminDashboard;
