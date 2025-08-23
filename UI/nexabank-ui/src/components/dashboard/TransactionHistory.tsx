import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { ArrowUpRight, ArrowDownLeft, CreditCard, Smartphone, Building } from 'lucide-react';
import { Badge } from '../ui/Badge';

interface Transaction {
  id: string;
  type: 'credit' | 'debit';
  amount: number;
  description: string;
  category: string;
  date: string;
  status: 'completed' | 'pending' | 'failed';
  account: string;
}

const TransactionHistory: React.FC = () => {
  const transactions: Transaction[] = [
    {
      id: '1',
      type: 'credit',
      amount: 50000,
      description: 'Salary Credit - Tech Corp',
      category: 'Salary',
      date: '2024-01-15',
      status: 'completed',
      account: 'Savings Account'
    },
    {
      id: '2',
      type: 'debit',
      amount: 2500,
      description: 'Amazon Purchase',
      category: 'Shopping',
      date: '2024-01-14',
      status: 'completed',
      account: 'Current Account'
    },
    {
      id: '3',
      type: 'debit',
      amount: 15000,
      description: 'Rent Payment',
      category: 'Bills',
      date: '2024-01-10',
      status: 'completed',
      account: 'Savings Account'
    },
    {
      id: '4',
      type: 'credit',
      amount: 5000,
      description: 'UPI Transfer from John',
      category: 'Transfer',
      date: '2024-01-09',
      status: 'completed',
      account: 'Current Account'
    },
    {
      id: '5',
      type: 'debit',
      amount: 850,
      description: 'Mobile Recharge',
      category: 'Utilities',
      date: '2024-01-08',
      status: 'pending',
      account: 'Current Account'
    }
  ];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const getTransactionIcon = (category: string) => {
    switch (category.toLowerCase()) {
      case 'transfer':
        return <ArrowUpRight className="h-4 w-4" />;
      case 'shopping':
        return <CreditCard className="h-4 w-4" />;
      case 'utilities':
        return <Smartphone className="h-4 w-4" />;
      case 'salary':
      case 'bills':
        return <Building className="h-4 w-4" />;
      default:
        return <CreditCard className="h-4 w-4" />;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'completed':
        return <Badge variant="secondary" className="bg-green-100 text-green-800">Completed</Badge>;
      case 'pending':
        return <Badge variant="secondary" className="bg-yellow-100 text-yellow-800">Pending</Badge>;
      case 'failed':
        return <Badge variant="secondary" className="bg-red-100 text-red-800">Failed</Badge>;
      default:
        return <Badge variant="secondary">Unknown</Badge>;
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          Recent Transactions
          <Badge variant="outline">{transactions.length} transactions</Badge>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {transactions.map((transaction) => (
            <div
              key={transaction.id}
              className="flex items-center justify-between p-4 rounded-lg border hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center space-x-4">
                <div className={`p-2 rounded-full ${
                  transaction.type === 'credit' 
                    ? 'bg-green-100 text-green-600' 
                    : 'bg-red-100 text-red-600'
                }`}>
                  {transaction.type === 'credit' 
                    ? <ArrowDownLeft className="h-4 w-4" />
                    : getTransactionIcon(transaction.category)
                  }
                </div>
                <div>
                  <p className="font-medium text-sm">{transaction.description}</p>
                  <div className="flex items-center space-x-2 text-xs text-gray-500">
                    <span>{transaction.account}</span>
                    <span>•</span>
                    <span>{new Date(transaction.date).toLocaleDateString()}</span>
                    <span>•</span>
                    <span>{transaction.category}</span>
                  </div>
                </div>
              </div>
              <div className="flex items-center space-x-3">
                <div className="text-right">
                  <p className={`font-semibold ${
                    transaction.type === 'credit' 
                      ? 'text-green-600' 
                      : 'text-red-600'
                  }`}>
                    {transaction.type === 'credit' ? '+' : '-'}{formatCurrency(transaction.amount)}
                  </p>
                </div>
                {getStatusBadge(transaction.status)}
              </div>
            </div>
          ))}
        </div>
        <div className="mt-6 text-center">
          <button className="text-blue-600 hover:text-blue-700 text-sm font-medium">
            View All Transactions
          </button>
        </div>
      </CardContent>
    </Card>
  );
};

export default TransactionHistory;
