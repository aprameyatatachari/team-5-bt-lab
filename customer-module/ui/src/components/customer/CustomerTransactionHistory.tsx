import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { accountAPI, type Transaction } from '../../services/api';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  Clock,
  Download
} from 'lucide-react';

interface CustomerTransactionHistoryProps {
  userId: string;
}

const CustomerTransactionHistory: React.FC<CustomerTransactionHistoryProps> = ({ userId }) => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [filter, setFilter] = useState<'ALL' | 'CREDIT' | 'DEBIT'>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchTransactions();
  }, [userId]);

  const fetchTransactions = async () => {
    try {
      setIsLoading(true);
      const response = await accountAPI.getUserTransactions(20); // Get last 20 transactions
      if (response.success) {
        setTransactions(response.data);
      } else {
        console.error('Failed to fetch transactions:', response.message);
      }
    } catch (error) {
      console.error('Error fetching transactions:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const filteredTransactions = transactions.filter(transaction => {
    if (filter === 'ALL') return true;
    if (filter === 'CREDIT') return transaction.transactionType === 'CREDIT' || transaction.transactionType === 'TRANSFER_IN';
    if (filter === 'DEBIT') return transaction.transactionType === 'DEBIT' || transaction.transactionType === 'TRANSFER_OUT';
    return false;
  });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getTransactionIcon = (type: string) => {
    return type === 'CREDIT' || type === 'TRANSFER_IN' ? ArrowDownLeft : ArrowUpRight;
  };

  const getTransactionColor = (type: string) => {
    return type === 'CREDIT' || type === 'TRANSFER_IN' ? 'text-green-600' : 'text-red-600';
  };

  const isCredit = (type: string) => {
    return type === 'CREDIT' || type === 'TRANSFER_IN';
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'FAILED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5" />
            Recent Transactions
          </CardTitle>
          <div className="flex items-center gap-2">
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value as any)}
              className="px-3 py-1 border border-gray-300 rounded-md text-sm"
            >
              <option value="ALL">All Transactions</option>
              <option value="CREDIT">Credits Only</option>
              <option value="DEBIT">Debits Only</option>
            </select>
            <button className="p-2 text-gray-500 hover:text-gray-700">
              <Download className="h-4 w-4" />
            </button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {isLoading ? (
            // Loading skeleton
            [...Array(5)].map((_, index) => (
              <div key={index} className="flex items-center justify-between p-4 border rounded-lg">
                <div className="flex items-center gap-4">
                  <div className="w-8 h-8 bg-gray-200 rounded-full animate-pulse"></div>
                  <div>
                    <div className="w-32 h-4 bg-gray-200 rounded animate-pulse mb-2"></div>
                    <div className="w-24 h-3 bg-gray-200 rounded animate-pulse mb-1"></div>
                    <div className="w-16 h-3 bg-gray-200 rounded animate-pulse"></div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="w-20 h-6 bg-gray-200 rounded animate-pulse mb-2"></div>
                  <div className="w-16 h-3 bg-gray-200 rounded animate-pulse"></div>
                </div>
              </div>
            ))
          ) : (
            filteredTransactions.map((transaction) => {
              const TransactionIcon = getTransactionIcon(transaction.transactionType);
              const creditTransaction = isCredit(transaction.transactionType);
              return (
                <div key={transaction.transactionId} className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50">
                  <div className="flex items-center gap-4">
                    <div className={`p-2 rounded-full ${creditTransaction ? 'bg-green-100' : 'bg-red-100'}`}>
                      <TransactionIcon className={`h-4 w-4 ${getTransactionColor(transaction.transactionType)}`} />
                    </div>
                    <div>
                      <h4 className="font-medium text-gray-900">{transaction.description}</h4>
                      <div className="flex items-center gap-2 mt-1">
                        <p className="text-sm text-gray-600">
                          {transaction.referenceNumber} • {transaction.category}
                        </p>
                        <Badge className={getStatusColor(transaction.status)}>
                          {transaction.status}
                        </Badge>
                      </div>
                      <p className="text-xs text-gray-500 mt-1">
                        {formatDate(transaction.createdAt)}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`font-semibold ${getTransactionColor(transaction.transactionType)}`}>
                      {creditTransaction ? '+' : '-'}{formatCurrency(transaction.amount)}
                    </p>
                    <p className="text-xs text-gray-500 mt-1">
                      Balance: {formatCurrency(transaction.balanceAfter)}
                    </p>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {filteredTransactions.length === 0 && (
          <div className="text-center py-8">
            <Clock className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No transactions found</h3>
            <p className="text-gray-600">No transactions match your current filter criteria.</p>
          </div>
        )}

        {filteredTransactions.length > 0 && (
          <div className="mt-6 text-center">
            <button className="text-blue-600 hover:text-blue-700 text-sm font-medium">
              View All Transactions
            </button>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default CustomerTransactionHistory;
