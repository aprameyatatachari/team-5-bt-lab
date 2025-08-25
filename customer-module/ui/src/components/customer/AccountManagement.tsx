import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { 
  PlusCircle, 
  CreditCard, 
  Eye,
  EyeOff,
  Copy,
  Settings,
  AlertCircle
} from 'lucide-react';
import { accountAPI, type BankAccount } from '../../services/api';

interface CreateAccountModalProps {
  onClose: () => void;
  onAccountCreated: () => void;
}

const CreateAccountModal: React.FC<CreateAccountModalProps> = ({ onClose, onAccountCreated }) => {
  const [accountType, setAccountType] = useState<string>('SAVINGS');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    try {
      const response = await accountAPI.createAccount(accountType);
      if (response.success) {
        // Emit custom event for real-time updates
        window.dispatchEvent(new CustomEvent('accountCreated', {
          detail: { 
            accountType,
            accountId: response.data?.accountId 
          }
        }));
        
        onAccountCreated();
        onClose();
      } else {
        setError(response.message || 'Failed to create account');
      }
    } catch (error: any) {
      setError(error.response?.data?.message || 'Failed to create account');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md">
        <h3 className="text-lg font-semibold mb-4">Create New Account</h3>
        
        {error && (
          <div className="bg-red-50 text-red-700 p-3 rounded-md mb-4 flex items-center gap-2">
            <AlertCircle className="h-4 w-4" />
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Account Type *
            </label>
            <select
              value={accountType}
              onChange={(e) => setAccountType(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
              required
            >
              <option value="SAVINGS">Savings Account</option>
              <option value="CURRENT">Current Account</option>
              <option value="FIXED_DEPOSIT">Fixed Deposit</option>
            </select>
          </div>

          <div className="bg-blue-50 p-3 rounded-md">
            <p className="text-xs text-blue-700">
              <strong>Note:</strong> Your account will be created with a minimum opening balance as per bank policy.
            </p>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Creating...' : 'Create Account'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

interface AccountManagementProps {
  userId: string;
}

const AccountManagement: React.FC<AccountManagementProps> = ({ userId }) => {
  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [visibleBalances, setVisibleBalances] = useState<Record<string, boolean>>({});

  useEffect(() => {
    fetchAccounts();
  }, [userId]);

  const fetchAccounts = async () => {
    if (!userId) return;
    
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

  const toggleBalanceVisibility = (accountId: string) => {
    setVisibleBalances(prev => ({
      ...prev,
      [accountId]: !prev[accountId]
    }));
  };

  const copyAccountNumber = (accountNumber: string) => {
    navigator.clipboard.writeText(accountNumber);
    // You could add a toast notification here
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const getAccountTypeColor = (accountType: string) => {
    switch (accountType) {
      case 'SAVINGS': return 'bg-green-100 text-green-800';
      case 'CHECKING': return 'bg-blue-100 text-blue-800';
      case 'FIXED_DEPOSIT': return 'bg-purple-100 text-purple-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'INACTIVE': return 'bg-gray-100 text-gray-800';
      case 'FROZEN': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <CreditCard className="h-5 w-5" />
            My Accounts
          </CardTitle>
          <Button
            onClick={() => setShowCreateForm(true)}
            className="flex items-center gap-2"
            size="sm"
          >
            <PlusCircle className="h-4 w-4" />
            New Account
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {isLoading ? (
            // Loading skeleton
            [...Array(2)].map((_, index) => (
              <div key={index} className="border rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <div className="w-32 h-4 bg-gray-200 rounded animate-pulse"></div>
                  <div className="w-20 h-6 bg-gray-200 rounded animate-pulse"></div>
                </div>
                <div className="w-48 h-3 bg-gray-200 rounded animate-pulse mb-2"></div>
                <div className="w-24 h-6 bg-gray-200 rounded animate-pulse"></div>
              </div>
            ))
          ) : accounts.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <CreditCard className="h-12 w-12 mx-auto mb-3 text-gray-300" />
              <p className="text-lg font-medium mb-2">No accounts found</p>
              <p className="text-sm">Create your first account to get started</p>
              <Button
                onClick={() => setShowCreateForm(true)}
                className="mt-4"
                variant="outline"
              >
                <PlusCircle className="h-4 w-4 mr-2" />
                Create Account
              </Button>
            </div>
          ) : (
            accounts.map((account) => (
              <div key={account.accountId} className="border rounded-lg p-4 hover:bg-gray-50">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <Badge className={getAccountTypeColor(account.accountType)}>
                      {account.accountType.replace('_', ' ')}
                    </Badge>
                    <Badge className={getStatusColor(account.status)}>
                      {account.status}
                    </Badge>
                  </div>
                  <Button 
                    variant="outline" 
                    size="sm"
                    title="Account Settings"
                  >
                    <Settings className="h-4 w-4" />
                  </Button>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Account Number</span>
                    <div className="flex items-center gap-2">
                      <code className="text-sm font-mono bg-gray-100 px-2 py-1 rounded">
                        {account.accountNumber}
                      </code>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => copyAccountNumber(account.accountNumber)}
                        title="Copy Account Number"
                      >
                        <Copy className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Current Balance</span>
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold text-gray-900">
                        {visibleBalances[account.accountId] 
                          ? formatCurrency(account.balance) 
                          : '••••••'}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => toggleBalanceVisibility(account.accountId)}
                        title={visibleBalances[account.accountId] ? 'Hide Balance' : 'Show Balance'}
                      >
                        {visibleBalances[account.accountId] ? (
                          <EyeOff className="h-3 w-3" />
                        ) : (
                          <Eye className="h-3 w-3" />
                        )}
                      </Button>
                    </div>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Created</span>
                    <span className="text-sm text-gray-900">
                      {new Date(account.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Create Account Modal */}
        {showCreateForm && (
          <CreateAccountModal 
            onClose={() => setShowCreateForm(false)}
            onAccountCreated={fetchAccounts}
          />
        )}
      </CardContent>
    </Card>
  );
};

export default AccountManagement;
