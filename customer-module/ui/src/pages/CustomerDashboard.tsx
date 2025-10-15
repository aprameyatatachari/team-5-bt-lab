import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import BankingHeader from '../components/layout/BankingHeader';
import CustomerServices from '../components/customer/CustomerServices';

const CustomerDashboard: React.FC = () => {
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
            Welcome back, {user?.firstName}!
          </h1>
          <p className="text-gray-600">
            Manage your customer profile and personal information.
          </p>
        </div>

        {/* Customer Profile Focus */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Profile Information */}
          <div className="lg:col-span-2 space-y-8">
            {/* Profile Overview */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">Profile Information</h3>
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">Full Name</label>
                    <p className="text-gray-900">{user?.firstName} {user?.lastName}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">Email</label>
                    <p className="text-gray-900">{user?.email}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">User ID</label>
                    <p className="text-gray-900">{user?.userId}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">Role</label>
                    <p className="text-gray-900">{user?.userType}</p>
                  </div>
                </div>
              </div>
            </div>

            {/* KYC Status */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">KYC & Verification Status</h3>
              <p className="text-gray-600 text-sm">
                Your Know Your Customer (KYC) verification helps ensure secure banking services.
              </p>
              <div className="mt-4 p-4 bg-yellow-50 rounded-lg">
                <p className="text-yellow-800 text-sm">
                  📄 Complete your profile information for enhanced services
                </p>
              </div>
            </div>
          </div>

          {/* Right Column - Customer Services */}
          <div className="space-y-6">
            <CustomerServices />
            
            {/* Customer Support */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">Customer Support</h3>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Profile Updates</span>
                  <span className="text-sm font-medium text-blue-600">Available</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Document Upload</span>
                  <span className="text-sm font-medium text-green-600">Ready</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Help & Support</span>
                  <span className="text-sm font-medium text-blue-600">24/7</span>
                </div>
              </div>
            </div>

            {/* Profile Completion */}
            <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-lg text-white p-6">
              <h3 className="text-lg font-semibold mb-2">Profile Completion</h3>
              <div className="text-3xl font-bold mb-2">85%</div>
              <p className="text-blue-100 text-sm">Complete your profile for better services</p>
              <button className="mt-4 text-sm bg-white/20 hover:bg-white/30 px-3 py-1 rounded-full transition-colors">
                Complete Profile
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default CustomerDashboard;
