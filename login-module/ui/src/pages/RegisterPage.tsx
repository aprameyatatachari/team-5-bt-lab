import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/Card';
import { useAuth } from '../contexts/AuthContext';
import type { RegisterRequest } from '../services/api';

const registerSchema = z.object({
  email: z.string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address')
    .max(100, 'Email cannot exceed 100 characters'),
  password: z.string()
    .min(8, 'Password must be at least 8 characters')
    .max(128, 'Password cannot exceed 128 characters')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/, 
           'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character'),
  confirmPassword: z.string(),
  firstName: z.string()
    .min(2, 'First name must be at least 2 characters')
    .max(50, 'First name cannot exceed 50 characters')
    .regex(/^[a-zA-Z\s'-]+$/, 'First name can only contain letters, spaces, hyphens, and apostrophes'),
  lastName: z.string()
    .min(2, 'Last name must be at least 2 characters')
    .max(50, 'Last name cannot exceed 50 characters')
    .regex(/^[a-zA-Z\s'-]+$/, 'Last name can only contain letters, spaces, hyphens, and apostrophes'),
  phoneNumber: z.string()
    .regex(/^[6-9]\d{9}$/, 'Phone number must be exactly 10 digits starting with 6, 7, 8, or 9'),
  dateOfBirth: z.string()
    .min(1, 'Date of birth is required')
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Date of birth must be in YYYY-MM-DD format'),
  address: z.string()
    .max(200, 'Address cannot exceed 200 characters')
    .optional()
    .or(z.literal('')),
  city: z.string()
    .max(50, 'City name cannot exceed 50 characters')
    .regex(/^[a-zA-Z\s'-]*$/, 'City can only contain letters, spaces, hyphens, and apostrophes')
    .optional()
    .or(z.literal('')),
  state: z.string()
    .max(50, 'State name cannot exceed 50 characters')
    .regex(/^[a-zA-Z\s'-]*$/, 'State can only contain letters, spaces, hyphens, and apostrophes')
    .optional()
    .or(z.literal('')),
  postalCode: z.string()
    .regex(/^[1-9]\d{5}$/, 'Postal code must be exactly 6 digits and cannot start with 0')
    .optional()
    .or(z.literal('')),
  aadharNumber: z.string()
    .regex(/^[2-9]\d{11}$/, 'Aadhar number must be exactly 12 digits and cannot start with 0 or 1')
    .optional()
    .or(z.literal('')),
  panNumber: z.string()
    .regex(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/, 'PAN number must follow format: AAAAA9999A')
    .optional()
    .or(z.literal('')),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

type RegisterFormData = z.infer<typeof registerSchema>;

const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register: registerUser } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError: setFormError,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormData) => {
    try {
      setIsLoading(true);
      setError(null);
      setValidationErrors({});
      
      const { confirmPassword, ...registerData } = data;
      
      // Default user type to CUSTOMER for public registration
      const customerData = {
        ...registerData,
        userType: 'CUSTOMER'
      };
      await registerUser(customerData as RegisterRequest);
      navigate('/dashboard');
    } catch (err: any) {
      console.log('Registration error:', err.response?.data);
      
      // Check if we have validation errors from the backend
      if (err.response?.data?.errors && typeof err.response.data.errors === 'object') {
        const backendErrors = err.response.data.errors;
        setValidationErrors(backendErrors);
        
        // Also set form errors for react-hook-form
        Object.keys(backendErrors).forEach(field => {
          setFormError(field as keyof RegisterFormData, {
            type: 'server',
            message: backendErrors[field]
          });
        });
        
        setError('Please fix the validation errors below');
      } else {
        // Generic error
        setError(err.response?.data?.message || err.message || 'Registration failed');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Helper function to get error message for a field (frontend or backend)
  const getFieldError = (fieldName: keyof RegisterFormData): string | undefined => {
    return errors[fieldName]?.message || validationErrors[fieldName];
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <Card className="w-full max-w-2xl">
        <CardHeader className="space-y-1 text-center">
          <div className="mx-auto mb-4 w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-md">
            <img src="/logo.png" alt="NexaBank Logo" className="w-16 h-16 object-contain" />
          </div>
          <CardTitle className="text-2xl font-bold">Join NexaBank</CardTitle>
          <CardDescription>
            Create your account to get started
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
                {error}
              </div>
            )}
            
            {/* Personal Information */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <label htmlFor="firstName" className="text-sm font-medium text-gray-700">
                  First Name *
                </label>
                <Input
                  id="firstName"
                  placeholder="Enter your first name"
                  {...register('firstName')}
                  className={getFieldError('firstName') ? 'border-red-500' : ''}
                />
                {getFieldError('firstName') && (
                  <p className="text-red-500 text-sm">{getFieldError('firstName')}</p>
                )}
              </div>
              <div className="space-y-2">
                <label htmlFor="lastName" className="text-sm font-medium text-gray-700">
                  Last Name *
                </label>
                <Input
                  id="lastName"
                  placeholder="Enter your last name"
                  {...register('lastName')}
                  className={getFieldError('lastName') ? 'border-red-500' : ''}
                />
                {getFieldError('lastName') && (
                  <p className="text-red-500 text-sm">{getFieldError('lastName')}</p>
                )}
              </div>
            </div>

            {/* Contact Information */}
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm font-medium text-gray-700">
                Email *
              </label>
              <Input
                id="email"
                type="email"
                placeholder="Enter your email"
                {...register('email')}
                className={getFieldError('email') ? 'border-red-500' : ''}
              />
              {getFieldError('email') && (
                <p className="text-red-500 text-sm">{getFieldError('email')}</p>
              )}
            </div>

            <div className="space-y-2">
              <label htmlFor="phoneNumber" className="text-sm font-medium text-gray-700">
                Phone Number *
              </label>
              <Input
                id="phoneNumber"
                placeholder="Enter 10-digit mobile number (e.g., 9876543210)"
                {...register('phoneNumber')}
                className={getFieldError('phoneNumber') ? 'border-red-500' : ''}
              />
              {getFieldError('phoneNumber') && (
                <p className="text-red-500 text-sm">{getFieldError('phoneNumber')}</p>
              )}
            </div>

            {/* Password */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <label htmlFor="password" className="text-sm font-medium text-gray-700">
                  Password *
                </label>
                <Input
                  id="password"
                  type="password"
                  placeholder="Enter your password"
                  {...register('password')}
                  className={getFieldError('password') ? 'border-red-500' : ''}
                />
                {getFieldError('password') && (
                  <p className="text-red-500 text-sm">{getFieldError('password')}</p>
                )}
                <p className="text-xs text-gray-500">Must contain uppercase, lowercase, number, and special character</p>
              </div>
              <div className="space-y-2">
                <label htmlFor="confirmPassword" className="text-sm font-medium text-gray-700">
                  Confirm Password *
                </label>
                <Input
                  id="confirmPassword"
                  type="password"
                  placeholder="Confirm your password"
                  {...register('confirmPassword')}
                  className={getFieldError('confirmPassword') ? 'border-red-500' : ''}
                />
                {getFieldError('confirmPassword') && (
                  <p className="text-red-500 text-sm">{getFieldError('confirmPassword')}</p>
                )}
              </div>
            </div>

            {/* Address Information */}
            <div className="space-y-2">
              <label htmlFor="address" className="text-sm font-medium text-gray-700">
                Address
              </label>
              <Input
                id="address"
                placeholder="Enter your address"
                {...register('address')}
                className={getFieldError('address') ? 'border-red-500' : ''}
              />
              {getFieldError('address') && (
                <p className="text-red-500 text-sm">{getFieldError('address')}</p>
              )}
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-2">
                <label htmlFor="city" className="text-sm font-medium text-gray-700">
                  City
                </label>
                <Input
                  id="city"
                  placeholder="City"
                  {...register('city')}
                  className={getFieldError('city') ? 'border-red-500' : ''}
                />
                {getFieldError('city') && (
                  <p className="text-red-500 text-sm">{getFieldError('city')}</p>
                )}
              </div>
              <div className="space-y-2">
                <label htmlFor="state" className="text-sm font-medium text-gray-700">
                  State
                </label>
                <Input
                  id="state"
                  placeholder="State"
                  {...register('state')}
                  className={getFieldError('state') ? 'border-red-500' : ''}
                />
                {getFieldError('state') && (
                  <p className="text-red-500 text-sm">{getFieldError('state')}</p>
                )}
              </div>
              <div className="space-y-2">
                <label htmlFor="postalCode" className="text-sm font-medium text-gray-700">
                  Postal Code
                </label>
                <Input
                  id="postalCode"
                  placeholder="6-digit postal code (cannot start with 0)"
                  {...register('postalCode')}
                  className={getFieldError('postalCode') ? 'border-red-500' : ''}
                />
                {getFieldError('postalCode') && (
                  <p className="text-red-500 text-sm">{getFieldError('postalCode')}</p>
                )}
              </div>
            </div>

            {/* Identity Documents */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <label htmlFor="aadharNumber" className="text-sm font-medium text-gray-700">
                  Aadhar Number
                </label>
                <Input
                  id="aadharNumber"
                  placeholder="12-digit Aadhar number (cannot start with 0 or 1)"
                  {...register('aadharNumber')}
                  className={getFieldError('aadharNumber') ? 'border-red-500' : ''}
                />
                {getFieldError('aadharNumber') && (
                  <p className="text-red-500 text-sm">{getFieldError('aadharNumber')}</p>
                )}
              </div>
              <div className="space-y-2">
                <label htmlFor="panNumber" className="text-sm font-medium text-gray-700">
                  PAN Number
                </label>
                <Input
                  id="panNumber"
                  placeholder="PAN number (e.g., ABCDE1234F)"
                  {...register('panNumber')}
                  className={getFieldError('panNumber') ? 'border-red-500' : ''}
                />
                {getFieldError('panNumber') && (
                  <p className="text-red-500 text-sm">{getFieldError('panNumber')}</p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="dateOfBirth" className="text-sm font-medium text-gray-700">
                Date of Birth *
              </label>
              <Input
                id="dateOfBirth"
                type="date"
                {...register('dateOfBirth')}
                className={getFieldError('dateOfBirth') ? 'border-red-500' : ''}
              />
              {getFieldError('dateOfBirth') && (
                <p className="text-red-500 text-sm">{getFieldError('dateOfBirth')}</p>
              )}
              <p className="text-xs text-gray-500">You must be at least 18 years old to register</p>
            </div>
          </CardContent>
          
          <CardFooter className="flex flex-col space-y-4">
            <Button
              type="submit"
              className="w-full"
              disabled={isLoading}
            >
              {isLoading ? 'Creating account...' : 'Create Account'}
            </Button>
            <div className="text-center text-sm text-gray-600">
              Already have an account?{' '}
              <Link to="/login" className="text-blue-600 hover:underline">
                Sign in
              </Link>
            </div>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
};

export default RegisterPage;
