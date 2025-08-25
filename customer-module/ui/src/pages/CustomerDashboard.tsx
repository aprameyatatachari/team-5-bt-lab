import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import BankingHeader from '../components/layout/BankingHeader';
import CustomerAccountSummary from '../components/customer/CustomerAccountSummary';
import CustomerTransactionHistory from '../components/customer/CustomerTransactionHistory';
import CustomerServices from '../components/customer/CustomerServices';
import QuickActions from '../components/customer/QuickActions';
import AccountManagement from '../components/customer/AccountManagement';

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
            Here's an overview of your banking activity and accounts.
          </p>
        </div>

        {/* Quick Actions */}
        <div className="mb-8">
          <QuickActions />
        </div>

        {/* Account Summary */}
        <div className="mb-8">
          <CustomerAccountSummary userId={user?.userId || ''} />
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Transaction History */}
          <div className="lg:col-span-2 space-y-8">
            <CustomerTransactionHistory userId={user?.userId || ''} />
            
            {/* Account Management */}
            <AccountManagement userId={user?.userId || ''} />
          </div>

          {/* Right Column - Services & Stats */}
          <div className="space-y-6">
            <CustomerServices />
            
            {/* Monthly Summary - Real data implementation needed */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">This Month</h3>
              <p className="text-xs text-gray-500 mb-4 italic">
                Note: Real-time data implementation pending - values below are placeholders
              </p>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Income</span>
                  <span className="text-sm font-medium text-green-600">Loading...</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Expenses</span>
                  <span className="text-sm font-medium text-red-600">Loading...</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Savings</span>
                  <span className="text-sm font-medium text-blue-600">Loading...</span>
                </div>
              </div>
            </div>

            {/* Credit Score - Real data implementation needed */}
            <div className="bg-gradient-to-r from-purple-600 to-blue-600 rounded-lg text-white p-6">
              <h3 className="text-lg font-semibold mb-2">Credit Score</h3>
              <div className="text-3xl font-bold mb-2">-</div>
              <p className="text-purple-100 text-sm">Credit score data will be integrated from credit agencies</p>
              <button className="mt-4 text-sm bg-white/20 hover:bg-white/30 px-3 py-1 rounded-full transition-colors">
                Coming Soon
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default CustomerDashboard;
