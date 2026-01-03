# SSS Sync System - Frontend

Vue 3 SPA for the Multi-Database Synchronization Platform with role-based access control.

## Features

- **Unified Login**: Single login page with JWT authentication
- **Role-Based Access Control**: Different features for USER and ADMIN roles
- **User Features**:
  - Order submission form with product selection
  - Real-time order placement across multiple databases
- **Admin Features**:
  - Complex SQL analytics query interface
  - Daily sync analytics reports with interactive charts
  - Conflict management portal

## Technology Stack

- **Vue 3**: Progressive JavaScript framework
- **Vite**: Next-generation frontend build tool
- **Vue Router**: Official router for Vue.js
- **Pinia**: State management
- **Axios**: HTTP client with JWT interceptors
- **ECharts**: Data visualization library

## Setup

### Prerequisites

- Node.js 20.x or higher
- npm 10.x or higher

### Installation

```bash
cd frontend
npm install
```

### Development

Start the development server with hot reload:

```bash
npm run dev
```

The application will be available at `http://localhost:5173`.

API requests are proxied to `http://localhost:8080` (Spring Boot backend).

### Production Build

Build the frontend for production:

```bash
npm run build
```

The built files will be placed in `../src/main/resources/static/` and served by the Spring Boot application.

## Project Structure

```
frontend/
├── src/
│   ├── api/              # Axios API client with JWT interceptor
│   ├── components/       # Reusable Vue components
│   │   └── layout/       # Layout components (AdminLayout)
│   ├── router/           # Vue Router configuration with guards
│   ├── stores/           # Pinia state management (auth store)
│   └── views/            # Page components
│       ├── Login.vue     # Login page
│       ├── admin/        # Admin-only pages
│       │   ├── ComplexQuery.vue
│       │   ├── DailySyncReport.vue
│       │   └── ConflictManagement.vue
│       └── user/         # User pages
│           └── OrderNew.vue
├── public/               # Static assets
├── index.html            # HTML entry point
├── vite.config.js        # Vite configuration
└── package.json          # Project dependencies
```

## Authentication

The application uses JWT (JSON Web Token) for authentication:

1. User logs in via `/api/auth/login`
2. JWT token is stored in localStorage
3. Axios interceptor automatically adds `Authorization: Bearer <token>` header
4. Router guards check authentication and role before navigating

### Demo Accounts

- **Admin**: username: `admin`, password: `admin123`
- **User**: username: `user1`, password: `user123`

## Routes

### Public Routes
- `/login` - Login page

### User Routes (USER & ADMIN roles)
- `/orders/new` - Create new order

### Admin Routes (ADMIN role only)
- `/admin/queries/complex` - Complex analytics queries
- `/admin/reports/daily-sync` - Daily sync reports with charts
- `/admin/conflicts` - Conflict management portal

## API Integration

The frontend communicates with the Spring Boot backend through these endpoints:

- `POST /api/auth/login` - User authentication
- `GET /api/products` - List available products
- `POST /api/orders/place` - Create new order
- `POST /api/queries/order-analytics` - Complex analytics query
- `GET /api/reports/daily-sync` - Daily sync statistics
- `/conflicts/view?token=...` - View specific conflict (token-based)

## Development Notes

- The application uses Vue 3 Composition API with `<script setup>`
- All HTTP requests use the configured Axios instance with automatic token injection
- Route guards prevent unauthorized access based on authentication and role
- ECharts is used for interactive data visualization in reports

## Building and Deployment

The frontend integrates seamlessly with the Spring Boot backend:

1. Build frontend: `npm run build`
2. Built files are placed in `src/main/resources/static/`
3. Spring Boot serves the SPA at the root path `/`
4. API endpoints are available at `/api/*`

For production deployment, ensure:
- Frontend is built before building the Spring Boot JAR
- `application.yml` is configured correctly for database connections
- JWT secret is set securely

