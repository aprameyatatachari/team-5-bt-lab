import React from 'react';
import { Card, CardContent } from '../ui/Card';
import { Button } from '../ui/Button';
import { 
  Send, 
  CreditCard, 
  PlusCircle, 
  Receipt
} from 'lucide-react';

interface QuickActionsProps {
  onTransfer?: () => void;
  onAddMoney?: () => void;
  onPayBills?: () => void;
  onNewAccount?: () => void;
}

const QuickActions: React.FC<QuickActionsProps> = ({ 
  onTransfer, 
  onAddMoney, 
  onPayBills, 
  onNewAccount 
}) => {
  const handleTransfer = () => {
    if (onTransfer) {
      onTransfer();
    } else {
      alert('Transfer feature coming soon!');
    }
  };

  const handleAddMoney = () => {
    if (onAddMoney) {
      onAddMoney();
    } else {
      alert('Add money feature coming soon!');
    }
  };

  const handlePayBills = () => {
    if (onPayBills) {
      onPayBills();
    } else {
      alert('Bill payment feature coming soon!');
    }
  };

  const handleNewAccount = () => {
    if (onNewAccount) {
      onNewAccount();
    } else {
      alert('Account creation feature coming soon!');
    }
  };

  const quickActions = [
    {
      icon: Send,
      title: 'Transfer',
      description: 'Send money instantly',
      color: 'text-blue-600',
      bgColor: 'bg-blue-50',
      borderColor: 'border-blue-200',
      onClick: handleTransfer,
    },
    {
      icon: PlusCircle,
      title: 'Add Money',
      description: 'Deposit to account',
      color: 'text-green-600',
      bgColor: 'bg-green-50',
      borderColor: 'border-green-200',
      onClick: handleAddMoney,
    },
    {
      icon: Receipt,
      title: 'Pay Bills',
      description: 'Utility payments',
      color: 'text-purple-600',
      bgColor: 'bg-purple-50',
      borderColor: 'border-purple-200',
      onClick: handlePayBills,
    },
    {
      icon: CreditCard,
      title: 'New Account',
      description: 'Open account',
      color: 'text-orange-600',
      bgColor: 'bg-orange-50',
      borderColor: 'border-orange-200',
      onClick: handleNewAccount,
    },
  ];

  return (
    <Card className="bg-gradient-to-r from-blue-50 to-indigo-50">
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {quickActions.map((action, index) => {
            const Icon = action.icon;
            return (
              <Button
                key={index}
                variant="outline"
                onClick={action.onClick}
                className={`h-24 flex flex-col items-center justify-center gap-2 ${action.bgColor} ${action.borderColor} hover:shadow-md transition-all duration-200 hover:scale-105`}
              >
                <div className={`p-2 rounded-full bg-white shadow-sm`}>
                  <Icon className={`h-5 w-5 ${action.color}`} />
                </div>
                <div className="text-center">
                  <span className="text-sm font-medium text-gray-900 block">{action.title}</span>
                  <span className="text-xs text-gray-600">{action.description}</span>
                </div>
              </Button>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
};

export default QuickActions;
