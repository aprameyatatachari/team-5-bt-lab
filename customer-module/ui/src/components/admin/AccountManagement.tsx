import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { adminAPI, type BankAccountDto } from '../../services/api';
import { 
  CreditCard, 
  Plus, 
  TrendingUp, 
  DollarSign
} from 'lucide-react';

const AccountManagement: React.FC = () => {
  const [accounts, setAccounts] = useState<BankAccountDto[]>([]);
  const [filterType, setFilterType] = useState<'ALL' | 'SAVINGS' | 'CURRENT' | 'FIXED_DEPOSIT' | 'LOAN'>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [stats, setStats] = useState<any>(null);

  useEffect(() => {
    fetchAccountData();
  }, []);

  const fetchAccountData = async () => {
    try {
      setIsLoading(true);
      
      // Fetch admin stats
      const statsResponse = await adminAPI.getBankStats();
      if (statsResponse.success) {
        setStats(statsResponse.data);
      }
      
      // Fetch all accounts
      const accountsResponse = await adminAPI.getAllAccounts();
      if (accountsResponse.success) {
        setAccounts(accountsResponse.data);
      }
      
    } catch (error) {
      console.error('Error fetching account data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleStatusUpdate = async (accountId: string, newStatus: string) => {
    try {
      const response = await adminAPI.updateAccountStatus(accountId, newStatus);
      if (response.success) {
        // Update the local state
        setAccounts(prev => 
          prev.map(account => 
            account.accountId === accountId 
              ? { ...account, status: newStatus as BankAccountDto['status'] }
              : account
          )
        );

  // Emit event so AdminStats can refresh immediately
  window.dispatchEvent(new CustomEvent('accountUpdated', { detail: { accountId, status: newStatus } }));
        // Cross-tab notify
        try {
          const bc = new BroadcastChannel('nexabank-admin');
          bc.postMessage({ type: 'account:updated', accountId, status: newStatus });
          bc.close();
        } catch {}
      }
    } catch (error) {
      console.error('Error updating account status:', error);
    }
  };

  const filteredAccounts = accounts.filter(account => 
    filterType === 'ALL' || account.accountType === filterType
  );

  const getAccountTypeColor = (type: string) => {
    switch (type) {
      case 'SAVINGS': return 'bg-green-100 text-green-800';
      case 'CURRENT': return 'bg-blue-100 text-blue-800';
      case 'FIXED_DEPOSIT': return 'bg-purple-100 text-purple-800';
      case 'LOAN': return 'bg-red-100 text-red-800';
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
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CreditCard className="h-5 w-5" />
          Account Management
        </CardTitle>
      </CardHeader>
      <CardContent>
        {/* Summary Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="bg-blue-50 p-4 rounded-lg">
            <div className="flex items-center gap-2">
              <CreditCard className="h-5 w-5 text-blue-600" />
              <span className="text-sm font-medium text-blue-900">Total Accounts</span>
            </div>
            <p className="text-2xl font-bold text-blue-900 mt-1">
              {stats?.totalAccounts || accounts.length}
            </p>
          </div>
          <div className="bg-green-50 p-4 rounded-lg">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-green-600" />
              <span className="text-sm font-medium text-green-900">Total Balance</span>
            </div>
            <p className="text-2xl font-bold text-green-900 mt-1">
              {formatCurrency(stats?.totalDeposits || totalBalance)}
            </p>
          </div>
          <div className="bg-purple-50 p-4 rounded-lg">
            <div className="flex items-center gap-2">
              <DollarSign className="h-5 w-5 text-purple-600" />
              <span className="text-sm font-medium text-purple-900">Avg. Balance</span>
            </div>
            <p className="text-2xl font-bold text-purple-900 mt-1">
              {formatCurrency(accounts.length > 0 ? totalBalance / accounts.length : 0)}
            </p>
          </div>
        </div>

        {/* Filter Controls */}
        <div className="flex justify-between items-center mb-6">
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value as any)}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm"
          >
            <option value="ALL">All Account Types</option>
            <option value="SAVINGS">Savings Accounts</option>
            <option value="CURRENT">Current Accounts</option>
            <option value="FIXED_DEPOSIT">Fixed Deposits</option>
            <option value="LOAN">Loan Accounts</option>
          </select>
          <Button 
            className="flex items-center gap-2"
            onClick={() => fetchAccountData()}
          >
            <Plus className="h-4 w-4" />
            Refresh Data
          </Button>
        </div>

        {/* Accounts List */}
        <div className="space-y-4">
          {isLoading ? (
            // Loading skeleton
            [...Array(3)].map((_, index) => (
              <div key={index} className="border rounded-lg p-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <div className="w-10 h-10 bg-gray-200 rounded-full animate-pulse"></div>
                      <div>
                        <div className="w-32 h-4 bg-gray-200 rounded animate-pulse mb-2"></div>
                        <div className="w-24 h-3 bg-gray-200 rounded animate-pulse"></div>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="w-24 h-6 bg-gray-200 rounded animate-pulse mb-2"></div>
                    <div className="flex gap-2">
                      <div className="w-16 h-8 bg-gray-200 rounded animate-pulse"></div>
                      <div className="w-12 h-8 bg-gray-200 rounded animate-pulse"></div>
                    </div>
                  </div>
                </div>
              </div>
            ))
          ) : filteredAccounts.length > 0 ? (
            filteredAccounts.map((account) => (
              <div key={account.accountId} className="border rounded-lg p-4 hover:bg-gray-50">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center">
                        <CreditCard className="h-5 w-5 text-gray-600" />
                      </div>
                      <div>
                        <h4 className="font-medium text-gray-900">
                          Account #{account.accountNumber}
                        </h4>
                        <p className="text-sm text-gray-600">{account.userName}</p>
                        <p className="text-xs text-gray-500">{account.userEmail}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 ml-13">
                      <Badge className={getAccountTypeColor(account.accountType)}>
                        {account.accountType.replace('_', ' ')}
                      </Badge>
                      <Badge className={getStatusColor(account.status)}>
                        {account.status}
                      </Badge>
                      <span className="text-xs text-gray-500">
                        Created: {new Date(account.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold text-gray-900">
                      {formatCurrency(account.balance)}
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      <select
                        value={account.status}
                        onChange={(e) => handleStatusUpdate(account.accountId, e.target.value)}
                        className="px-2 py-1 text-xs border border-gray-300 rounded"
                      >
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                        <option value="SUSPENDED">Suspended</option>
                      </select>
                      <Button variant="outline" size="sm">
                        View Details
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-12">
              <CreditCard className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No Accounts Found</h3>
              <p className="text-gray-600 mb-4">
                {filterType === 'ALL' 
                  ? "No accounts are available in the system." 
                  : `No ${filterType.toLowerCase().replace('_', ' ')} accounts found.`}
              </p>
              <Button onClick={() => fetchAccountData()}>
                Refresh Data
              </Button>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

export default AccountManagement;
