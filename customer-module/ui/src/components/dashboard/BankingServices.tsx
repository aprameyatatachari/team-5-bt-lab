import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { 
  CreditCard, 
  Home, 
  Car, 
  GraduationCap, 
  PiggyBank, 
  Shield,
  TrendingUp,
  Globe
} from 'lucide-react';
import { Button } from '../ui/Button';

const BankingServices: React.FC = () => {
  const services = [
    {
      icon: <CreditCard className="h-8 w-8" />,
      title: "Credit Cards",
      description: "Premium credit cards with exclusive benefits",
      color: "bg-blue-500",
      features: ["Cashback rewards", "Zero annual fee", "Travel insurance"]
    },
    {
      icon: <Home className="h-8 w-8" />,
      title: "Home Loans",
      description: "Competitive interest rates for your dream home",
      color: "bg-green-500",
      features: ["Low interest rates", "Quick approval", "Flexible tenure"]
    },
    {
      icon: <Car className="h-8 w-8" />,
      title: "Auto Loans",
      description: "Finance your vehicle with easy EMIs",
      color: "bg-orange-500",
      features: ["100% financing", "Quick processing", "Minimal documentation"]
    },
    {
      icon: <GraduationCap className="h-8 w-8" />,
      title: "Education Loans",
      description: "Invest in your future with education financing",
      color: "bg-purple-500",
      features: ["Study abroad support", "Flexible repayment", "Competitive rates"]
    },
    {
      icon: <PiggyBank className="h-8 w-8" />,
      title: "Fixed Deposits",
      description: "Secure your savings with guaranteed returns",
      color: "bg-indigo-500",
      features: ["High interest rates", "Flexible tenure", "Safe investment"]
    },
    {
      icon: <Shield className="h-8 w-8" />,
      title: "Insurance",
      description: "Comprehensive insurance solutions",
      color: "bg-red-500",
      features: ["Life insurance", "Health coverage", "Term plans"]
    },
    {
      icon: <TrendingUp className="h-8 w-8" />,
      title: "Investments",
      description: "Grow your wealth with smart investments",
      color: "bg-teal-500",
      features: ["Mutual funds", "SIP options", "Expert advice"]
    },
    {
      icon: <Globe className="h-8 w-8" />,
      title: "Forex Services",
      description: "International banking and currency exchange",
      color: "bg-cyan-500",
      features: ["Currency exchange", "Wire transfers", "Travel cards"]
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">Banking Services</h2>
        <Button variant="outline">View All Services</Button>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        {services.map((service, index) => (
          <Card key={index} className="group hover:shadow-lg transition-all duration-300 cursor-pointer">
            <CardHeader className="pb-4">
              <div className={`w-16 h-16 ${service.color} rounded-xl flex items-center justify-center text-white mb-4 group-hover:scale-110 transition-transform duration-300`}>
                {service.icon}
              </div>
              <CardTitle className="text-lg">{service.title}</CardTitle>
              <p className="text-sm text-gray-600">{service.description}</p>
            </CardHeader>
            <CardContent className="pt-0">
              <ul className="space-y-2 mb-4">
                {service.features.map((feature, featureIndex) => (
                  <li key={featureIndex} className="text-xs text-gray-500 flex items-center">
                    <span className="w-1 h-1 bg-gray-400 rounded-full mr-2"></span>
                    {feature}
                  </li>
                ))}
              </ul>
              <Button size="sm" className="w-full" variant="outline">
                Learn More
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Featured Service Banner */}
      <Card className="bg-gradient-to-r from-blue-600 to-purple-700 text-white">
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-xl font-bold mb-2">Special Offer: NexaBank Premium</h3>
              <p className="text-blue-100 mb-4">
                Get premium banking services with exclusive benefits and priority support
              </p>
              <ul className="text-sm text-blue-100 space-y-1">
                <li>• Zero balance account</li>
                <li>• Free debit card</li>
                <li>• 24/7 customer support</li>
                <li>• Higher transaction limits</li>
              </ul>
            </div>
            <div className="text-right">
              <Button className="bg-white text-blue-600 hover:bg-gray-100">
                Upgrade Now
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default BankingServices;
