import React, { useState, useEffect, useRef } from 'react';
import { Card, CardContent } from '../ui/Card';
import { Users, CreditCard, TrendingUp, AlertTriangle, DollarSign, Activity, RefreshCw } from 'lucide-react';
import { adminAPI } from '../../services/api';

interface BankStats {
  totalUsers: number;
  totalCustomers: number;
  totalAdmins: number;
  totalEmployees: number;
  activeUsers: number;
  lockedUsers: number;
  totalAccounts: number;
  totalDeposits: number;
  totalTransactions: number;
  totalTransactionVolume: number;
}

const AdminStats: React.FC = () => {
  const [stats, setStats] = useState<BankStats>({
    totalUsers: 0,
    totalCustomers: 0,
    totalAdmins: 0,
    totalEmployees: 0,
    activeUsers: 0,
    lockedUsers: 0,
    totalAccounts: 0,
    totalDeposits: 0,
    totalTransactions: 0,
    totalTransactionVolume: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const intervalRef = useRef<number | null>(null);
  const bcRef = useRef<BroadcastChannel | null>(null);

  const fetchStats = async () => {
    try {
      setIsLoading(true);
      const response = await adminAPI.getBankStats();
      if (response.success) {
        setStats(response.data);
        setLastUpdated(new Date());
      }
    } catch (error) {
      console.error('Error fetching bank stats:', error);
      // Keep default values if API fails
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();

    const startPolling = () => {
      // Poll more frequently while visible; slow down when hidden
      const intervalMs = document.visibilityState === 'visible' ? 10000 : 30000;
      if (intervalRef.current) window.clearInterval(intervalRef.current);
      intervalRef.current = window.setInterval(fetchStats, intervalMs);
    };

    // Start initial polling
    startPolling();

    // Adjust polling on tab visibility changes
    const handleVisibility = () => {
      // Trigger immediate refresh when the tab becomes visible
      if (document.visibilityState === 'visible') {
        fetchStats();
      }
      startPolling();
    };
    document.addEventListener('visibilitychange', handleVisibility);

    return () => {
      if (intervalRef.current) window.clearInterval(intervalRef.current);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, []);

  // Listen for custom events and broadcast channel messages to trigger refresh
  useEffect(() => {
    const handleUserCreated = () => {
      console.log('User created event received, refreshing stats...');
      fetchStats();
    };

    const handleUserUpdated = () => {
      console.log('User updated event received, refreshing stats...');
      fetchStats();
    };

    const handleUserDeleted = () => {
      console.log('User deleted event received, refreshing stats...');
      fetchStats();
    };

    const handleAccountCreated = () => {
      console.log('Account created event received, refreshing stats...');
      fetchStats();
    };
    const handleAccountUpdated = () => {
      console.log('Account updated event received, refreshing stats...');
      fetchStats();
    };

    window.addEventListener('userCreated', handleUserCreated);
    window.addEventListener('userUpdated', handleUserUpdated);
    window.addEventListener('userDeleted', handleUserDeleted);
  window.addEventListener('accountCreated', handleAccountCreated);
  window.addEventListener('accountUpdated', handleAccountUpdated);

    // Cross-tab updates via BroadcastChannel (fallback guarded)
    try {
      const bc = new BroadcastChannel('nexabank-admin');
      bcRef.current = bc;
      bc.onmessage = (ev) => {
        if (ev?.data?.type?.startsWith('user:') || ev?.data?.type?.startsWith('account:') || ev?.data?.type === 'stats:refresh') {
          console.log('Broadcast message received:', ev.data);
          fetchStats();
        }
      };
    } catch (e) {
      // BroadcastChannel not supported; ignore silently
    }
    
    return () => {
      window.removeEventListener('userCreated', handleUserCreated);
      window.removeEventListener('userUpdated', handleUserUpdated);
      window.removeEventListener('userDeleted', handleUserDeleted);
  window.removeEventListener('accountCreated', handleAccountCreated);
  window.removeEventListener('accountUpdated', handleAccountUpdated);
      if (bcRef.current) bcRef.current.close();
    };
  }, []);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const handleRefresh = () => {
    fetchStats();
  };

  const statCards = [
    {
      title: 'Total Users',
      value: stats.totalUsers.toLocaleString(),
      subtitle: `${stats.totalCustomers} Customers, ${stats.totalAdmins} Admins`,
      icon: Users,
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
    },
    {
      title: 'Total Accounts',
      value: stats.totalAccounts.toLocaleString(),
      subtitle: `${stats.activeUsers} Active Users`,
      icon: CreditCard,
      color: 'text-green-600',
      bgColor: 'bg-green-100',
    },
    {
      title: 'Total Deposits',
      value: formatCurrency(stats.totalDeposits),
      subtitle: `Across ${stats.totalAccounts} accounts`,
      icon: DollarSign,
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
    },
    {
      title: 'Transactions',
      value: stats.totalTransactions.toLocaleString(),
      subtitle: formatCurrency(stats.totalTransactionVolume),
      icon: Activity,
      color: 'text-orange-600',
      bgColor: 'bg-orange-100',
    },
    {
      title: 'User Status',
      value: stats.activeUsers.toLocaleString(),
      subtitle: `${stats.lockedUsers} Locked`,
      icon: TrendingUp,
      color: 'text-emerald-600',
      bgColor: 'bg-emerald-100',
    },
    {
      title: 'System Health',
      value: stats.lockedUsers > 0 ? 'Issues' : 'Good',
      subtitle: `${stats.lockedUsers} Locked Accounts`,
      icon: AlertTriangle,
      color: stats.lockedUsers > 0 ? 'text-red-600' : 'text-green-600',
      bgColor: stats.lockedUsers > 0 ? 'bg-red-100' : 'bg-green-100',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header with refresh button */}
      <div className="flex justify-between items-center">
        <h2 className="text-lg font-semibold text-gray-900">System Statistics</h2>
        <div className="flex items-center gap-4">
          {lastUpdated && (
            <span className="text-sm text-gray-500">
              Last updated: {lastUpdated.toLocaleTimeString()}
            </span>
          )}
          <button
            onClick={handleRefresh}
            disabled={isLoading}
            className="flex items-center gap-2 px-3 py-1 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {statCards.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <Card key={index} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-600">{stat.title}</p>
                    {isLoading ? (
                      <div className="w-20 h-8 bg-gray-200 animate-pulse rounded mt-1"></div>
                    ) : (
                      <p className="text-2xl font-bold text-gray-900 mt-1">{stat.value}</p>
                    )}
                    {!isLoading && (
                      <p className="text-xs text-gray-500 mt-1">{stat.subtitle}</p>
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
    </div>
  );
};

export default AdminStats;
