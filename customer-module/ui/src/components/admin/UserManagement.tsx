import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Badge } from '../ui/Badge';
import { 
  User, 
  Plus, 
  Edit, 
  Trash2, 
  Search, 
  Eye,
  Lock,
  Unlock
} from 'lucide-react';
import { adminAPI, type User as UserData, type CreateUserRequest } from '../../services/api';

interface CreateUserModalProps {
  onClose: () => void;
  onUserCreated: () => void;
}

const CreateUserModal: React.FC<CreateUserModalProps> = ({ onClose, onUserCreated }) => {
  const [formData, setFormData] = useState<CreateUserRequest>({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    userType: 'CUSTOMER',
    dateOfBirth: '',
    address: '',
    city: '',
    state: '',
    country: 'India',
    postalCode: '',
    aadharNumber: '',
    panNumber: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    try {
      const response = await adminAPI.createUser(formData);
      if (response.success) {
        // Emit custom event for real-time stats update
        window.dispatchEvent(new CustomEvent('userCreated', {
          detail: { 
            userType: formData.userType,
            userId: response.data?.userId 
          }
        }));
        
        onUserCreated();
        onClose();
      } else {
        setError(response.message || 'Failed to create user');
      }
    } catch (error: any) {
      setError(error.response?.data?.message || 'Failed to create user');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <h3 className="text-lg font-semibold mb-4">Create New User</h3>
        
        {error && (
          <div className="bg-red-50 text-red-700 p-3 rounded-md mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email *
              </label>
              <Input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                required
                placeholder="user@example.com"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Password *
              </label>
              <Input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                required
                placeholder="Minimum 8 characters"
                minLength={8}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                First Name *
              </label>
              <Input
                type="text"
                name="firstName"
                value={formData.firstName}
                onChange={handleInputChange}
                required
                placeholder="John"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Last Name *
              </label>
              <Input
                type="text"
                name="lastName"
                value={formData.lastName}
                onChange={handleInputChange}
                required
                placeholder="Doe"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Phone Number *
              </label>
              <Input
                type="tel"
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleInputChange}
                required
                placeholder="+1-555-0123"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                User Type *
              </label>
              <select
                name="userType"
                value={formData.userType}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                required
              >
                <option value="CUSTOMER">Customer</option>
                <option value="EMPLOYEE">Employee</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Date of Birth
              </label>
              <Input
                type="date"
                name="dateOfBirth"
                value={formData.dateOfBirth}
                onChange={handleInputChange}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                City
              </label>
              <Input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleInputChange}
                placeholder="New York"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                State
              </label>
              <Input
                type="text"
                name="state"
                value={formData.state}
                onChange={handleInputChange}
                placeholder="NY"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Postal Code
              </label>
              <Input
                type="text"
                name="postalCode"
                value={formData.postalCode}
                onChange={handleInputChange}
                placeholder="10001"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Aadhar Number
              </label>
              <Input
                type="text"
                name="aadharNumber"
                value={formData.aadharNumber}
                onChange={handleInputChange}
                placeholder="1234-5678-9012"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                PAN Number
              </label>
              <Input
                type="text"
                name="panNumber"
                value={formData.panNumber}
                onChange={handleInputChange}
                placeholder="ABCDE1234F"
              />
            </div>
          </div>

          <div className="col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Address
            </label>
            <Input
              type="text"
              name="address"
              value={formData.address}
              onChange={handleInputChange}
              placeholder="123 Main Street"
            />
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Creating...' : 'Create User'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<UserData[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<'ALL' | 'CUSTOMER' | 'ADMIN' | 'EMPLOYEE'>('ALL');
  const [isLoading, setIsLoading] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setIsLoading(true);
      const response = await adminAPI.getAllUsers();
      if (response.success) {
        setUsers(response.data);
      }
    } catch (error) {
      console.error('Error fetching users:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateUserStatus = async (userId: string, currentStatus: string) => {
    try {
      const newStatus = currentStatus === 'LOCKED' ? 'ACTIVE' : 'LOCKED';
      const response = await adminAPI.updateUserStatus(userId, newStatus);
      if (response.success) {
        // Update local state
        setUsers(users.map(user => 
          user.userId === userId 
            ? { ...user, status: newStatus as 'ACTIVE' | 'INACTIVE' | 'LOCKED' }
            : user
        ));
        
        // Emit custom event for real-time stats update
        window.dispatchEvent(new CustomEvent('userUpdated', {
          detail: { 
            userId,
            oldStatus: currentStatus,
            newStatus 
          }
        }));
      }
    } catch (error) {
      console.error('Error updating user status:', error);
    }
  };

  const handleDeleteUser = async (userId: string) => {
    if (window.confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
      try {
        const userToDelete = users.find(user => user.userId === userId);
        const response = await adminAPI.deleteUser(userId);
        if (response.success) {
          // Remove user from local state
          setUsers(users.filter(user => user.userId !== userId));
          
          // Emit custom event for real-time stats update
          window.dispatchEvent(new CustomEvent('userDeleted', {
            detail: { 
              userId,
              userType: userToDelete?.userType 
            }
          }));
        }
      } catch (error) {
        console.error('Error deleting user:', error);
      }
    }
  };

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterType === 'ALL' || user.userType === filterType;
    return matchesSearch && matchesFilter;
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'INACTIVE': return 'bg-gray-100 text-gray-800';
      case 'LOCKED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getUserTypeColor = (userType: string) => {
    switch (userType) {
      case 'ADMIN': return 'bg-purple-100 text-purple-800';
      case 'EMPLOYEE': return 'bg-blue-100 text-blue-800';
      case 'CUSTOMER': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <User className="h-5 w-5" />
          User Management
        </CardTitle>
      </CardHeader>
      <CardContent>
        {/* Search and Filter Controls */}
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
            <Input
              placeholder="Search users..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
          <div className="flex gap-2">
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value as any)}
              className="px-3 py-2 border border-gray-300 rounded-md text-sm"
            >
              <option value="ALL">All Users</option>
              <option value="CUSTOMER">Customers</option>
              <option value="ADMIN">Admins</option>
              <option value="EMPLOYEE">Employees</option>
            </select>
            <Button
              onClick={() => setShowCreateForm(true)}
              className="flex items-center gap-2"
            >
              <Plus className="h-4 w-4" />
              Add User
            </Button>
          </div>
        </div>

        {/* Users List */}
        <div className="space-y-4">
          {isLoading ? (
            // Loading skeleton
            [...Array(3)].map((_, index) => (
              <div key={index} className="border rounded-lg p-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <div className="w-10 h-10 bg-gray-200 rounded-full animate-pulse"></div>
                      <div>
                        <div className="w-32 h-4 bg-gray-200 rounded animate-pulse mb-1"></div>
                        <div className="w-48 h-3 bg-gray-200 rounded animate-pulse"></div>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {[...Array(4)].map((_, i) => (
                      <div key={i} className="w-8 h-8 bg-gray-200 rounded animate-pulse"></div>
                    ))}
                  </div>
                </div>
              </div>
            ))
          ) : (
            filteredUsers.map((user) => (
              <div key={user.userId} className="border rounded-lg p-4 hover:bg-gray-50">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center">
                        <User className="h-5 w-5 text-gray-600" />
                      </div>
                      <div>
                        <h4 className="font-medium text-gray-900">
                          {user.firstName} {user.lastName}
                        </h4>
                        <p className="text-sm text-gray-600">{user.email}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 ml-13">
                      <Badge className={getUserTypeColor(user.userType)}>
                        {user.userType}
                      </Badge>
                      <Badge className={getStatusColor(user.status)}>
                        {user.status}
                      </Badge>
                      {user.lastLogin && (
                        <span className="text-xs text-gray-500">
                          Last login: {new Date(user.lastLogin).toLocaleDateString()}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button variant="outline" size="sm" title="View Details">
                      <Eye className="h-4 w-4" />
                    </Button>
                    <Button variant="outline" size="sm" title="Edit User">
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button 
                      variant="outline" 
                      size="sm"
                      className={user.status === 'LOCKED' ? 'text-green-600' : 'text-orange-600'}
                      onClick={() => handleUpdateUserStatus(user.userId, user.status)}
                      title={user.status === 'LOCKED' ? 'Unlock User' : 'Lock User'}
                    >
                      {user.status === 'LOCKED' ? <Unlock className="h-4 w-4" /> : <Lock className="h-4 w-4" />}
                    </Button>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      className="text-red-600"
                      onClick={() => handleDeleteUser(user.userId)}
                      title="Delete User"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {filteredUsers.length === 0 && !isLoading && (
          <div className="text-center py-8 text-gray-500">
            No users found matching your criteria.
          </div>
        )}

        {/* Create User Modal */}
        {showCreateForm && (
          <CreateUserModal 
            onClose={() => setShowCreateForm(false)}
            onUserCreated={fetchUsers}
          />
        )}
      </CardContent>
    </Card>
  );
};

export default UserManagement;
