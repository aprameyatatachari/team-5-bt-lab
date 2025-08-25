import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Label } from '../components/ui/Label';
import BankingHeader from '../components/layout/BankingHeader';
import { ArrowRight, Shield, Clock, DollarSign } from 'lucide-react';
import { Badge } from '../components/ui/Badge';

const transferSchema = z.object({
  fromAccount: z.string().min(1, 'Please select an account'),
  toAccount: z.string().min(10, 'Account number must be at least 10 digits'),
  amount: z.number().min(1, 'Amount must be greater than 0').max(500000, 'Amount cannot exceed ₹5,00,000'),
  purpose: z.string().min(1, 'Purpose is required'),
  beneficiaryName: z.string().min(2, 'Beneficiary name is required'),
  remarks: z.string().optional(),
});

type TransferForm = z.infer<typeof transferSchema>;

const TransferPage: React.FC = () => {
  const [step, setStep] = useState(1);
  const [isProcessing, setIsProcessing] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<TransferForm>({
    resolver: zodResolver(transferSchema),
  });

  const watchedValues = watch();

  const userAccounts = [
    {
      type: 'Savings Account',
      number: '1234567890123456',
      balance: 125840.50,
      masked: '**** **** **** 3456'
    },
    {
      type: 'Current Account',
      number: '5678901234567890',
      balance: 45230.75,
      masked: '**** **** **** 7890'
    }
  ];

  const transferPurposes = [
    'Family Maintenance',
    'Education',
    'Medical Treatment',
    'Business Payment',
    'Investment',
    'Loan Repayment',
    'Gift',
    'Others'
  ];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const onSubmit = async (data: TransferForm) => {
    setIsProcessing(true);
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000));
    setStep(3);
    setIsProcessing(false);
  };

  const renderStepIndicator = () => (
    <div className="flex items-center justify-center mb-8">
      <div className="flex items-center space-x-4">
        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
          step >= 1 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'
        }`}>
          1
        </div>
        <div className={`w-16 h-1 ${step >= 2 ? 'bg-blue-600' : 'bg-gray-200'}`}></div>
        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
          step >= 2 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'
        }`}>
          2
        </div>
        <div className={`w-16 h-1 ${step >= 3 ? 'bg-blue-600' : 'bg-gray-200'}`}></div>
        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
          step >= 3 ? 'bg-green-600 text-white' : 'bg-gray-200 text-gray-600'
        }`}>
          ✓
        </div>
      </div>
    </div>
  );

  if (step === 3) {
    return (
      <div className="min-h-screen bg-gray-50">
        <BankingHeader />
        <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {renderStepIndicator()}
          <Card className="max-w-md mx-auto">
            <CardContent className="p-8 text-center">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Shield className="h-8 w-8 text-green-600" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">Transfer Successful!</h2>
              <p className="text-gray-600 mb-6">
                Your transfer of {formatCurrency(watchedValues.amount || 0)} has been processed successfully.
              </p>
              <div className="bg-gray-50 rounded-lg p-4 mb-6">
                <div className="text-sm text-gray-600 space-y-1">
                  <p><span className="font-medium">Transaction ID:</span> TXN{Date.now()}</p>
                  <p><span className="font-medium">To:</span> {watchedValues.beneficiaryName}</p>
                  <p><span className="font-medium">Account:</span> ****{watchedValues.toAccount?.slice(-4)}</p>
                </div>
              </div>
              <Button className="w-full mb-2">
                Download Receipt
              </Button>
              <Button variant="outline" className="w-full" onClick={() => setStep(1)}>
                Make Another Transfer
              </Button>
            </CardContent>
          </Card>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <BankingHeader />
      
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Transfer Money</h1>
          <p className="text-gray-600">Send money to other accounts quickly and securely</p>
        </div>

        {renderStepIndicator()}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Transfer Form */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle>
                  {step === 1 ? 'Transfer Details' : 'Review & Confirm'}
                </CardTitle>
              </CardHeader>
              <CardContent>
                {step === 1 ? (
                  <form onSubmit={handleSubmit(() => setStep(2))} className="space-y-6">
                    {/* From Account */}
                    <div>
                      <Label htmlFor="fromAccount">From Account</Label>
                      <select
                        {...register('fromAccount')}
                        className="w-full mt-1 p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      >
                        <option value="">Select Account</option>
                        {userAccounts.map((account, index) => (
                          <option key={index} value={account.number}>
                            {account.type} - {account.masked} (₹{account.balance.toLocaleString()})
                          </option>
                        ))}
                      </select>
                      {errors.fromAccount && (
                        <p className="text-red-500 text-sm mt-1">{errors.fromAccount.message}</p>
                      )}
                    </div>

                    {/* To Account */}
                    <div>
                      <Label htmlFor="toAccount">To Account Number</Label>
                      <Input
                        {...register('toAccount')}
                        placeholder="Enter account number"
                        className="mt-1"
                      />
                      {errors.toAccount && (
                        <p className="text-red-500 text-sm mt-1">{errors.toAccount.message}</p>
                      )}
                    </div>

                    {/* Beneficiary Name */}
                    <div>
                      <Label htmlFor="beneficiaryName">Beneficiary Name</Label>
                      <Input
                        {...register('beneficiaryName')}
                        placeholder="Enter beneficiary name"
                        className="mt-1"
                      />
                      {errors.beneficiaryName && (
                        <p className="text-red-500 text-sm mt-1">{errors.beneficiaryName.message}</p>
                      )}
                    </div>

                    {/* Amount */}
                    <div>
                      <Label htmlFor="amount">Amount (₹)</Label>
                      <Input
                        {...register('amount', { valueAsNumber: true })}
                        type="number"
                        placeholder="Enter amount"
                        className="mt-1"
                        min="1"
                        max="500000"
                      />
                      {errors.amount && (
                        <p className="text-red-500 text-sm mt-1">{errors.amount.message}</p>
                      )}
                    </div>

                    {/* Purpose */}
                    <div>
                      <Label htmlFor="purpose">Purpose</Label>
                      <select
                        {...register('purpose')}
                        className="w-full mt-1 p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      >
                        <option value="">Select Purpose</option>
                        {transferPurposes.map((purpose) => (
                          <option key={purpose} value={purpose}>
                            {purpose}
                          </option>
                        ))}
                      </select>
                      {errors.purpose && (
                        <p className="text-red-500 text-sm mt-1">{errors.purpose.message}</p>
                      )}
                    </div>

                    {/* Remarks */}
                    <div>
                      <Label htmlFor="remarks">Remarks (Optional)</Label>
                      <Input
                        {...register('remarks')}
                        placeholder="Additional notes"
                        className="mt-1"
                      />
                    </div>

                    <Button type="submit" className="w-full">
                      Review Transfer
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </Button>
                  </form>
                ) : (
                  <div className="space-y-6">
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                      <h3 className="font-medium text-blue-900 mb-3">Transfer Summary</h3>
                      <div className="space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-blue-700">From:</span>
                          <span className="font-medium">
                            {userAccounts.find(acc => acc.number === watchedValues.fromAccount)?.type}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-blue-700">To:</span>
                          <span className="font-medium">{watchedValues.beneficiaryName}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-blue-700">Account:</span>
                          <span className="font-medium">****{watchedValues.toAccount?.slice(-4)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-blue-700">Amount:</span>
                          <span className="font-bold text-lg">{formatCurrency(watchedValues.amount || 0)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-blue-700">Purpose:</span>
                          <span className="font-medium">{watchedValues.purpose}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex space-x-4">
                      <Button
                        variant="outline"
                        onClick={() => setStep(1)}
                        className="flex-1"
                      >
                        Back
                      </Button>
                      <Button
                        onClick={handleSubmit(onSubmit)}
                        disabled={isProcessing}
                        className="flex-1"
                      >
                        {isProcessing ? 'Processing...' : 'Confirm Transfer'}
                      </Button>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Side Panel */}
          <div className="space-y-6">
            {/* Transfer Limits */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <DollarSign className="h-5 w-5 mr-2" />
                  Transfer Limits
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span>Daily Limit:</span>
                  <span className="font-medium">₹5,00,000</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Per Transaction:</span>
                  <span className="font-medium">₹5,00,000</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Used Today:</span>
                  <span className="font-medium text-green-600">₹0</span>
                </div>
              </CardContent>
            </Card>

            {/* Security Notice */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Shield className="h-5 w-5 mr-2" />
                  Security
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm text-gray-600">
                  <div className="flex items-start space-x-2">
                    <Badge variant="secondary" className="mt-0.5">✓</Badge>
                    <span>256-bit SSL encryption</span>
                  </div>
                  <div className="flex items-start space-x-2">
                    <Badge variant="secondary" className="mt-0.5">✓</Badge>
                    <span>Two-factor authentication</span>
                  </div>
                  <div className="flex items-start space-x-2">
                    <Badge variant="secondary" className="mt-0.5">✓</Badge>
                    <span>Real-time fraud monitoring</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Processing Time */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Clock className="h-5 w-5 mr-2" />
                  Processing Time
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-gray-600">
                  IMPS transfers are processed instantly and are available 24/7, including weekends and holidays.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
};

export default TransferPage;
