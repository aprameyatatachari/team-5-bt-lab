# NexaBank Project Structure

A modern banking application with microservices architecture, featuring separate authentication and customer management modules.

## 🏗️ Project Architecture

```
team-5-bt-lab/
├── 🔐 login-module/                 # Authentication Service (Port 8080)
│   ├── src/main/java/
│   │   └── com/nexabank/auth/
│   │       ├── controller/          # REST Controllers
│   │       ├── service/             # Business Logic
│   │       ├── entity/              # JPA Entities (Auth only)
│   │       ├── repository/          # Data Access Layer
│   │       ├── security/            # Security Configuration
│   │       └── dto/                 # Data Transfer Objects
│   ├── ui/                          # React Frontend (Port 5173)
│   │   ├── src/
│   │   │   ├── components/          # Reusable Components
│   │   │   ├── pages/               # Page Components
│   │   │   ├── contexts/            # React Contexts
│   │   │   ├── hooks/               # Custom Hooks
│   │   │   └── services/            # API Services
│   │   └── package.json
│   └── pom.xml
│
├── 👥 customer-module/              # Customer Service (Port 8081)
│   ├── src/main/java/
│   │   └── com/nexabank/customer/
│   │       ├── controller/          # REST Controllers
│   │       ├── service/             # Business Logic
│   │       ├── entity/              # JPA Entities (Customer data)
│   │       ├── repository/          # Data Access Layer
│   │       └── dto/                 # Data Transfer Objects
│   ├── ui/                          # React Frontend (Port 5174)
│   │   ├── src/
│   │   │   ├── components/          # Banking Components
│   │   │   ├── pages/               # Dashboard Pages
│   │   │   └── services/            # API Services
│   │   └── package.json
│   └── pom.xml
│
├── 🚀 start-nexabank.bat           # Simple startup script
├── 🚀 start-nexabank-advanced.bat  # Advanced startup with monitoring
└── 📋 README.md                    # This file
```

## 🔧 Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.5
- **Security**: Spring Security with JWT
- **Database**: MySQL
- **Session Management**: Redis
- **ORM**: JPA/Hibernate

### Frontend
- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **UI Components**: Custom component library
- **State Management**: React Context

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 1. Database Setup
Create MySQL databases:
```sql
CREATE DATABASE nexabank_auth;
CREATE DATABASE nexabank_customer;
```

### 2. Redis Setup
Start Redis server:
```bash
redis-server
```

### 3. Start All Services
Use the convenient batch script:
```bash
# Windows
start-nexabank-advanced.bat

# Manual startup (Linux/Mac)
# Terminal 1: Login Backend
cd login-module && mvn spring-boot:run

# Terminal 2: Customer Backend  
cd customer-module && mvn spring-boot:run

# Terminal 3: Login Frontend
cd login-module/ui && npm install && npm run dev

# Terminal 4: Customer Frontend
cd customer-module/ui && npm install && npm run dev
```

## 🌐 Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Login Backend | http://localhost:8080 | Authentication API |
| Customer Backend | http://localhost:8081 | Customer Management API |
| Login Frontend | http://localhost:5173 | Authentication UI |
| Customer Frontend | http://localhost:5174 | Banking Dashboard UI |

## 🔐 Key Features

### Authentication Module
- ✅ JWT-based authentication with Redis session management
- ✅ 10-minute automatic lockout mechanism
- ✅ Token blacklisting for immediate logout
- ✅ Comprehensive audit logging
- ✅ Role-based access control (CUSTOMER, ADMIN, EMPLOYEE)

### Customer Module
- ✅ Comprehensive customer profile management
- ✅ Bank account management with proper relationships
- ✅ KYC status tracking and compliance
- ✅ Risk category management
- ✅ Advanced search and filtering capabilities
- ✅ Customer statistics and reporting

## 📊 Database Design

### Authentication Database (nexabank_auth)
- **users**: Authentication data only
- **user_sessions**: Session management (replaced by Redis)

### Customer Database (nexabank_customer)
- **customers**: Complete customer profiles
- **bank_accounts**: Account information
- **customer_identification**: Document management
- **transactions**: Transaction history

## 🔄 Entity Relationships

```
Auth Module:
User (auth-only) ←--→ Customer (business data)
                       ↓
                   BankAccount
                       ↓
                  Transactions
```

## 🛡️ Security Features

1. **JWT with JTI**: Unique token identification
2. **Redis Denylist**: Immediate token blacklisting
3. **Session Lockout**: 10-minute automatic lockout
4. **Audit Trails**: Comprehensive operation logging
5. **Entity Separation**: Clean separation of auth and business data

## 📝 API Documentation

### Authentication Endpoints
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout (with lockout clearing)
- `POST /api/auth/register` - User registration
- `POST /api/auth/refresh` - Token refresh
- `GET /api/auth/lockout-status/{email}` - Check lockout status

### Customer Endpoints
- `GET /api/customers` - List all customers
- `POST /api/customers` - Create customer
- `GET /api/customers/{id}` - Get customer by ID
- `PUT /api/customers/{id}` - Update customer
- `GET /api/customers/by-user/{userId}` - Get customer by user ID

## 🧪 Development

### Adding New Features
1. **Backend**: Add controllers, services, and entities
2. **Frontend**: Create components and integrate with API
3. **Database**: Update schema if needed
4. **Tests**: Add unit and integration tests

### Code Quality
- Follow Spring Boot best practices
- Use TypeScript for type safety
- Implement proper error handling
- Add comprehensive logging

## 🤝 Contributing

1. Follow the existing code structure
2. Maintain separation between auth and customer modules
3. Update documentation when adding features
4. Test thoroughly before committing

## 📄 License

This project is part of the Banking Technology Lab and is intended for educational purposes.