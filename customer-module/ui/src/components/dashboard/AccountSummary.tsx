import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { CreditCard, TrendingUp, TrendingDown, Eye, EyeOff } from 'lucide-react';
import { Button } from '../ui/Button';

interface AccountSummaryProps {
  userType: string;
}

const AccountSummary: React.FC<AccountSummaryProps> = ({ userType: _ }) => {
  const [showBalance, setShowBalance] = React.useState(true);

  const accounts = [
    {
      type: 'Savings Account',
      number: '**** **** **** 1234',
      balance: 125840.50,
      currency: '₹',
      color: 'bg-gradient-to-r from-blue-600 to-blue-800'
    },
    {
      type: 'Current Account',
      number: '**** **** **** 5678',
      balance: 45230.75,
      currency: '₹',
      color: 'bg-gradient-to-r from-green-600 to-green-800'
    },
    {
      type: 'Fixed Deposit',
      number: '**** **** **** 9012',
      balance: 500000.00,
      currency: '₹',
      color: 'bg-gradient-to-r from-purple-600 to-purple-800'
    }
  ];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const totalBalance = accounts.reduce((sum, account) => sum + account.balance, 0);

  return (
    <div className="space-y-6">
      {/* Total Balance Overview */}
      <Card className="bg-gradient-to-r from-indigo-600 to-purple-700 text-white">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg font-medium">Total Balance</CardTitle>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowBalance(!showBalance)}
              className="text-white hover:bg-white/20"
            >
              {showBalance ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-3xl font-bold">
            {showBalance ? formatCurrency(totalBalance) : '₹ ****,***.**'}
          </div>
          <p className="text-indigo-100 mt-2">Across all accounts</p>
        </CardContent>
      </Card>

      {/* Individual Accounts */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {accounts.map((account, index) => (
          <Card key={index} className="relative overflow-hidden">
            <div className={`absolute inset-0 ${account.color} opacity-5`}></div>
            <CardHeader className="pb-3">
              <div className="flex items-center space-x-2">
                <CreditCard className="h-5 w-5 text-gray-600" />
                <CardTitle className="text-sm font-medium">{account.type}</CardTitle>
              </div>
              <p className="text-xs text-gray-500">{account.number}</p>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {showBalance ? formatCurrency(account.balance) : '₹ ****,***.**'}
              </div>
              <div className="flex items-center space-x-2 mt-2">
                <TrendingUp className="h-4 w-4 text-green-500" />
                <span className="text-sm text-green-600">+2.5% this month</span>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Button className="h-16 flex-col bg-blue-600 hover:bg-blue-700">
              <CreditCard className="h-6 w-6 mb-1" />
              <span className="text-xs">Transfer</span>
            </Button>
            <Button variant="outline" className="h-16 flex-col">
              <TrendingUp className="h-6 w-6 mb-1" />
              <span className="text-xs">Investment</span>
            </Button>
            <Button variant="outline" className="h-16 flex-col">
              <CreditCard className="h-6 w-6 mb-1" />
              <span className="text-xs">Pay Bills</span>
            </Button>
            <Button variant="outline" className="h-16 flex-col">
              <TrendingDown className="h-6 w-6 mb-1" />
              <span className="text-xs">Statements</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AccountSummary;
