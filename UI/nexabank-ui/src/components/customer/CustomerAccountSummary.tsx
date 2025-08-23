import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { 
  CreditCard, 
  Plus, 
  Eye, 
  TrendingUp,
  Wallet
} from 'lucide-react';
import { accountAPI, type BankAccount } from '../../services/api';

interface CustomerAccountSummaryProps {
  userId: string;
}

const CustomerAccountSummary: React.FC<CustomerAccountSummaryProps> = ({ userId }) => {
  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);

  useEffect(() => {
    if (userId) {
      fetchAccounts();
    }
  }, [userId]);

  const fetchAccounts = async () => {
    try {
      setIsLoading(true);
      const response = await accountAPI.getUserAccounts();
      if (response.success) {
        setAccounts(response.data);
      }
    } catch (error) {
      console.error('Error fetching accounts:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateAccount = async (accountType: string) => {
    try {
      const response = await accountAPI.createAccount(accountType);
      
      if (response.success) {
        alert('Account created successfully!');
        setShowCreateForm(false);
        fetchAccounts(); // Refresh the accounts list
      } else {
        alert('Failed to create account: ' + response.message);
      }
    } catch (error) {
      console.error('Error creating account:', error);
      alert('Failed to create account. Please try again.');
    }
  };

  const getAccountTypeColor = (type: string) => {
    switch (type) {
      case 'SAVINGS': return 'bg-green-100 text-green-800';
      case 'CURRENT': return 'bg-blue-100 text-blue-800';
      case 'FIXED_DEPOSIT': return 'bg-purple-100 text-purple-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'INACTIVE': return 'bg-gray-100 text-gray-800';
      case 'SUSPENDED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const totalBalance = accounts.reduce((sum, account) => sum + account.balance, 0);

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="bg-gradient-to-r from-blue-600 to-blue-700 text-white">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-blue-100 text-sm">Total Balance</p>
                <p className="text-2xl font-bold mt-1">{formatCurrency(totalBalance)}</p>
              </div>
              <Wallet className="h-8 w-8 text-blue-200" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-r from-green-600 to-green-700 text-white">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-green-100 text-sm">Active Accounts</p>
                <p className="text-2xl font-bold mt-1">{accounts.filter(acc => acc.status === 'ACTIVE').length}</p>
              </div>
              <CreditCard className="h-8 w-8 text-green-200" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-r from-purple-600 to-purple-700 text-white">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-purple-100 text-sm">Avg. Interest Rate</p>
                <p className="text-2xl font-bold mt-1">
                  {accounts.length > 0 
                    ? (accounts.reduce((sum, acc) => sum + acc.interestRate, 0) / accounts.length).toFixed(1)
                    : '0.0'
                  }%
                </p>
              </div>
              <TrendingUp className="h-8 w-8 text-purple-200" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Accounts List */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <CreditCard className="h-5 w-5" />
              Your Accounts
            </CardTitle>
            <Button 
              onClick={() => setShowCreateForm(true)}
              className="flex items-center gap-2"
            >
              <Plus className="h-4 w-4" />
              Open New Account
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {isLoading ? (
              // Loading skeleton
              [...Array(2)].map((_, index) => (
                <div key={index} className="border rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <div className="w-12 h-12 bg-gray-200 rounded-lg animate-pulse"></div>
                        <div>
                          <div className="w-32 h-4 bg-gray-200 rounded animate-pulse mb-1"></div>
                          <div className="w-24 h-3 bg-gray-200 rounded animate-pulse"></div>
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="w-24 h-6 bg-gray-200 rounded animate-pulse mb-2"></div>
                      <div className="w-20 h-3 bg-gray-200 rounded animate-pulse"></div>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              accounts.map((account) => (
                <div key={account.accountId} className="border rounded-lg p-4 hover:bg-gray-50">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                          <CreditCard className="h-6 w-6 text-white" />
                        </div>
                        <div>
                          <h4 className="font-semibold text-gray-900">
                            {account.accountType.replace('_', ' ')} Account
                          </h4>
                          <p className="text-sm text-gray-600">Account ****{account.accountNumber?.slice(-4) || 'N/A'}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 ml-15">
                        <Badge className={getAccountTypeColor(account.accountType)}>
                          {account.accountType.replace('_', ' ')}
                        </Badge>
                        <Badge className={getStatusColor(account.status)}>
                          {account.status}
                        </Badge>
                        <span className="text-xs text-gray-500">
                          {account.interestRate}% interest
                        </span>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-xl font-bold text-gray-900">
                        {formatCurrency(account.balance)}
                      </p>
                      {account.lastTransactionDate && (
                        <p className="text-xs text-gray-500 mt-1">
                          Last activity: {new Date(account.lastTransactionDate).toLocaleDateString()}
                        </p>
                      )}
                      <div className="flex items-center gap-2 mt-3">
                        <Button variant="outline" size="sm">
                          <Eye className="h-4 w-4 mr-1" />
                          View Details
                        </Button>
                        <Button variant="outline" size="sm">
                          Transfer
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {accounts.length === 0 && !isLoading && (
            <div className="text-center py-8">
              <CreditCard className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No accounts yet</h3>
              <p className="text-gray-600 mb-4">Open your first account to get started with banking.</p>
              <Button onClick={() => setShowCreateForm(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Open Your First Account
              </Button>
            </div>
          )}

          {/* Create Account Modal */}
          {showCreateForm && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-lg p-6 w-full max-w-md">
                <h3 className="text-lg font-semibold mb-4">Open New Account</h3>
                <p className="text-gray-600 mb-4">Select the type of account you want to open:</p>
                <div className="space-y-3">
                  <Button 
                    className="w-full justify-start" 
                    variant="outline"
                    onClick={() => handleCreateAccount('SAVINGS')}
                  >
                    <CreditCard className="h-4 w-4 mr-2" />
                    Savings Account (4.5% interest)
                  </Button>
                  <Button 
                    className="w-full justify-start" 
                    variant="outline"
                    onClick={() => handleCreateAccount('CURRENT')}
                  >
                    <CreditCard className="h-4 w-4 mr-2" />
                    Current Account (2.0% interest)
                  </Button>
                  <Button 
                    className="w-full justify-start" 
                    variant="outline"
                    onClick={() => handleCreateAccount('FIXED_DEPOSIT')}
                  >
                    <CreditCard className="h-4 w-4 mr-2" />
                    Fixed Deposit (6.5% interest)
                  </Button>
                </div>
                <div className="flex justify-end gap-2 mt-6">
                  <Button variant="outline" onClick={() => setShowCreateForm(false)}>
                    Cancel
                  </Button>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default CustomerAccountSummary;
