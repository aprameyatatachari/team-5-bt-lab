import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import BankingHeader from '../components/layout/BankingHeader';
import AccountSummary from '../components/dashboard/AccountSummary';
import TransactionHistory from '../components/dashboard/TransactionHistory';
import BankingServices from '../components/dashboard/BankingServices';

const DashboardPage: React.FC = () => {
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
            Here's an overview of your banking activity and services.
          </p>
        </div>

        {/* Account Summary */}
        <div className="mb-8">
          <AccountSummary userType={user?.userType || 'CUSTOMER'} />
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Transaction History */}
          <div className="lg:col-span-2">
            <TransactionHistory />
          </div>

          {/* Right Column - Quick Stats */}
          <div className="space-y-6">
            {/* Monthly Summary */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">This Month</h3>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Income</span>
                  <span className="font-semibold text-green-600">+₹55,000</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Expenses</span>
                  <span className="font-semibold text-red-600">-₹18,350</span>
                </div>
                <div className="flex justify-between items-center pt-2 border-t">
                  <span className="text-sm text-gray-600">Net Savings</span>
                  <span className="font-bold text-blue-600">₹36,650</span>
                </div>
              </div>
            </div>

            {/* Goals */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">Savings Goals</h3>
              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span>Emergency Fund</span>
                    <span>75%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div className="bg-blue-600 h-2 rounded-full" style={{ width: '75%' }}></div>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">₹75,000 of ₹100,000</p>
                </div>
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span>Vacation Fund</span>
                    <span>40%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div className="bg-green-600 h-2 rounded-full" style={{ width: '40%' }}></div>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">₹20,000 of ₹50,000</p>
                </div>
              </div>
            </div>

            {/* Credit Score */}
            <div className="bg-gradient-to-r from-purple-600 to-blue-600 rounded-lg text-white p-6">
              <h3 className="text-lg font-semibold mb-2">Credit Score</h3>
              <div className="text-3xl font-bold mb-2">752</div>
              <p className="text-purple-100 text-sm">Good • Updated yesterday</p>
              <button className="mt-4 text-sm bg-white/20 hover:bg-white/30 px-3 py-1 rounded-full transition-colors">
                View Details
              </button>
            </div>
          </div>
        </div>

        {/* Banking Services */}
        <div className="mt-12">
          <BankingServices />
        </div>
      </main>
    </div>
  );
};

export default DashboardPage;
