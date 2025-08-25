import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken: refreshToken,
          });

          const { accessToken, refreshToken: newRefreshToken } = response.data.data;
          localStorage.setItem('accessToken', accessToken);
          localStorage.setItem('refreshToken', newRefreshToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed, redirect to login
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export interface User {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  userType: 'CUSTOMER' | 'ADMIN' | 'EMPLOYEE';
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED';
  lastLogin?: string;
  maskedAadhar?: string;
  maskedPan?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  dateOfBirth?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
  aadharNumber?: string;
  panNumber?: string;
  userType?: 'CUSTOMER' | 'ADMIN' | 'EMPLOYEE';
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  userType: 'CUSTOMER' | 'ADMIN' | 'EMPLOYEE';
  dateOfBirth?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
  aadharNumber?: string;
  panNumber?: string;
}

// Auth API functions
export const authAPI = {
  login: async (credentials: LoginRequest): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post('/auth/login', credentials);
    return response.data;
  },

  register: async (userData: RegisterRequest): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },

  logout: async (): Promise<ApiResponse<string>> => {
    const response = await api.post('/auth/logout');
    return response.data;
  },

  logoutAllDevices: async (): Promise<ApiResponse<string>> => {
    const response = await api.post('/auth/logout-all');
    return response.data;
  },

  getCurrentUser: async (): Promise<ApiResponse<User>> => {
    const response = await api.get('/auth/me');
    return response.data;
  },

  validateToken: async (): Promise<ApiResponse<string>> => {
    const response = await api.get('/auth/validate');
    return response.data;
  },

  refreshToken: async (refreshToken: string): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post('/auth/refresh', { refreshToken });
    return response.data;
  },
};

// Admin API functions
export const adminAPI = {
  getAllUsers: async (): Promise<ApiResponse<User[]>> => {
    const response = await api.get('/admin/users');
    return response.data;
  },

  getUserById: async (userId: string): Promise<ApiResponse<User>> => {
    const response = await api.get(`/admin/users/${userId}`);
    return response.data;
  },

  createUser: async (userData: CreateUserRequest): Promise<ApiResponse<User>> => {
    const response = await api.post('/admin/users', userData);
    return response.data;
  },

  updateUser: async (userId: string, userData: Partial<User>): Promise<ApiResponse<User>> => {
    const response = await api.put(`/admin/users/${userId}`, userData);
    return response.data;
  },

  updateUserStatus: async (userId: string, status: string): Promise<ApiResponse<User>> => {
    const response = await api.put(`/admin/users/${userId}/status?status=${status}`);
    return response.data;
  },

  deleteUser: async (userId: string): Promise<ApiResponse<string>> => {
    const response = await api.delete(`/admin/users/${userId}`);
    return response.data;
  },

  getBankStats: async (): Promise<ApiResponse<any>> => {
    const response = await api.get('/admin/stats');
    return response.data;
  },

  // Account management functions
  getAllAccounts: async (accountType?: string, status?: string): Promise<ApiResponse<BankAccountDto[]>> => {
    let url = '/admin/accounts';
    const params = new URLSearchParams();
    if (accountType) params.append('accountType', accountType);
    if (status) params.append('status', status);
    if (params.toString()) url += '?' + params.toString();

    const response = await api.get(url);
    return response.data;
  },

  getAccountById: async (accountId: string): Promise<ApiResponse<BankAccountDto>> => {
    const response = await api.get(`/admin/accounts/${accountId}`);
    return response.data;
  },

  updateAccountStatus: async (accountId: string, status: string): Promise<ApiResponse<BankAccountDto>> => {
    const response = await api.put(`/admin/accounts/${accountId}/status?status=${status}`);
    return response.data;
  },
};

// Account API functions
export const accountAPI = {
  getUserAccounts: async (): Promise<ApiResponse<BankAccount[]>> => {
    const response = await api.get('/accounts/my-accounts');
    return response.data;
  },

  getUserTransactions: async (limit: number = 10): Promise<ApiResponse<Transaction[]>> => {
    const response = await api.get(`/accounts/my-transactions?limit=${limit}`);
    return response.data;
  },

  createAccount: async (accountType: string): Promise<ApiResponse<BankAccount>> => {
    const response = await api.post(`/accounts/create?accountType=${accountType}`);
    return response.data;
  },

  getAccountTransactions: async (accountId: string, limit: number = 20): Promise<ApiResponse<Transaction[]>> => {
    const response = await api.get(`/accounts/${accountId}/transactions?limit=${limit}`);
    return response.data;
  },
};

// Additional interfaces for new entities
export interface BankAccount {
  accountId: string;
  accountNumber: string;
  accountType: 'SAVINGS' | 'CURRENT' | 'FIXED_DEPOSIT' | 'LOAN';
  balance: number;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';
  interestRate: number;
  createdAt: string;
  lastTransactionDate?: string;
}

export interface BankAccountDto {
  accountId: string;
  accountNumber: string;
  accountType: 'SAVINGS' | 'CURRENT' | 'FIXED_DEPOSIT' | 'LOAN';
  balance: number;
  userId: string;
  userName: string;
  userEmail: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

export interface Transaction {
  transactionId: string;
  transactionType: 'CREDIT' | 'DEBIT' | 'TRANSFER_IN' | 'TRANSFER_OUT';
  amount: number;
  description: string;
  balanceAfter: number;
  referenceNumber: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  category: string;
  createdAt: string;
  processedAt?: string;
}

export default api;
