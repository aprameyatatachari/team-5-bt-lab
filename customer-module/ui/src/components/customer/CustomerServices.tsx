import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { Button } from '../ui/Button';
import { 
  Send, 
  CreditCard, 
  Smartphone, 
  Receipt, 
  FileText,
  PlusCircle
} from 'lucide-react';

const CustomerServices: React.FC = () => {
  const services = [
    {
      icon: Send,
      title: 'Transfer Money',
      description: 'Send money to any account instantly',
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
    },
    {
      icon: CreditCard,
      title: 'Request Card',
      description: 'Apply for debit or credit cards',
      color: 'text-green-600',
      bgColor: 'bg-green-100',
    },
    {
      icon: Smartphone,
      title: 'Mobile Recharge',
      description: 'Recharge your mobile phone',
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
    },
    {
      icon: Receipt,
      title: 'Pay Bills',
      description: 'Pay utility and other bills',
      color: 'text-orange-600',
      bgColor: 'bg-orange-100',
    },
    {
      icon: FileText,
      title: 'Statements',
      description: 'Download account statements',
      color: 'text-indigo-600',
      bgColor: 'bg-indigo-100',
    },
    {
      icon: PlusCircle,
      title: 'More Services',
      description: 'Explore additional services',
      color: 'text-gray-600',
      bgColor: 'bg-gray-100',
    },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Quick Services</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-4">
          {services.map((service, index) => {
            const Icon = service.icon;
            return (
              <Button
                key={index}
                variant="outline"
                className="h-20 flex flex-col items-center justify-center gap-2 hover:shadow-md transition-shadow"
              >
                <div className={`p-2 rounded-full ${service.bgColor}`}>
                  <Icon className={`h-4 w-4 ${service.color}`} />
                </div>
                <span className="text-xs font-medium text-center">{service.title}</span>
              </Button>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
};

export default CustomerServices;
