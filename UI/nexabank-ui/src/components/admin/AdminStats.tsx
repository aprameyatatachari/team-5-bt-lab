import React, { useState, useEffect } from 'react';
import { Card, CardContent } from '../ui/Card';
import { Users, CreditCard, TrendingUp, AlertTriangle } from 'lucide-react';
import { adminAPI } from '../../services/api';

interface BankStats {
  totalUsers: number;
  totalAccounts: number;
  totalBalance: number;
  pendingApprovals: number;
}

const AdminStats: React.FC = () => {
  const [stats, setStats] = useState<BankStats>({
    totalUsers: 0,
    totalAccounts: 0,
    totalBalance: 0,
    pendingApprovals: 0,
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setIsLoading(true);
        const response = await adminAPI.getBankStats();
        if (response.success) {
          setStats(response.data);
        }
      } catch (error) {
        console.error('Error fetching bank stats:', error);
        // Fallback to default values if API fails
        setStats({
          totalUsers: 0,
          totalAccounts: 0,
          totalBalance: 0,
          pendingApprovals: 0,
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, []);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const statCards = [
    {
      title: 'Total Users',
      value: stats.totalUsers.toLocaleString(),
      icon: Users,
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
    },
    {
      title: 'Total Accounts',
      value: stats.totalAccounts.toLocaleString(),
      icon: CreditCard,
      color: 'text-green-600',
      bgColor: 'bg-green-100',
    },
    {
      title: 'Total Balance',
      value: formatCurrency(stats.totalBalance),
      icon: TrendingUp,
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
    },
    {
      title: 'Pending Approvals',
      value: stats.pendingApprovals.toString(),
      icon: AlertTriangle,
      color: 'text-orange-600',
      bgColor: 'bg-orange-100',
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
      {statCards.map((stat, index) => {
        const Icon = stat.icon;
        return (
          <Card key={index} className="hover:shadow-md transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">{stat.title}</p>
                  {isLoading ? (
                    <div className="w-20 h-8 bg-gray-200 animate-pulse rounded mt-1"></div>
                  ) : (
                    <p className="text-2xl font-bold text-gray-900 mt-1">{stat.value}</p>
                  )}
                </div>
                <div className={`p-3 rounded-full ${stat.bgColor}`}>
                  <Icon className={`h-6 w-6 ${stat.color}`} />
                </div>
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
};

export default AdminStats;
