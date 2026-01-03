# SSS Sync System - Multi-Database Synchronization Platform

A comprehensive database synchronization system with a Vue 3 SPA frontend featuring role-based access control, order management, analytics, and conflict resolution.

## Features

### Backend (Spring Boot)
- **Multi-Database Support**: MySQL, PostgreSQL, SQL Server
- **Change Data Capture**: Trigger-based change log system
- **Automatic Synchronization**: Background sync engine with conflict detection
- **JWT Authentication**: Secure token-based authentication with role-based access
- **RESTful APIs**: Comprehensive APIs for all features
- **Email Notifications**: Conflict alerts with resolution links

### Frontend (Vue 3 SPA)
- **Unified Login**: Single login page with JWT authentication
- **Role-Based Access Control**:
  - **USER Role**: Order submission and management
  - **ADMIN Role**: Full access to analytics, reports, and conflict management
- **Admin Features**:
  - Complex SQL analytics with multi-table joins
  - Interactive daily sync reports with charts (ECharts)
  - Conflict management portal
- **User Features**:
  - Product selection and order submission
  - Multi-database order placement
- **Responsive Design**: Works on desktop, tablet, and mobile

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Vue 3 SPA Frontend                       │
│  ┌───────────┐  ┌──────────┐  ┌────────────────────────┐   │
│  │   Login   │  │   User   │  │   Admin Console        │   │
│  │   Page    │  │  Orders  │  │  - Analytics Queries   │   │
│  └───────────┘  └──────────┘  │  - Sync Reports        │   │
│                                │  - Conflict Mgmt       │   │
│                                └────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │ JWT + REST API
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                        │
│  ┌──────────────┐ ┌─────────────┐ ┌──────────────────────┐ │
│  │     Auth     │ │   Orders    │ │   Analytics & Sync   │ │
│  │   Service    │ │   Service   │ │      Services        │ │
│  └──────────────┘ └─────────────┘ └──────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │             Sync Engine (Change Log)                   │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
    ┌─────────┐        ┌─────────┐        ┌─────────┐
    │  MySQL  │        │PostgreSQL│        │SQL Server│
    └─────────┘        └─────────┘        └─────────┘
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 20+
- MySQL 8.0+
- PostgreSQL 12+
- SQL Server 2019+

### 1. Database Setup

Initialize all three databases using scripts in `db/` directory:

```bash
# MySQL
mysql -u root -p < db/mysql/00_create_db.sql
mysql -u root -p sss_db < db/mysql/01_schema.sql
mysql -u root -p sss_db < db/mysql/02_triggers_change_log.sql
mysql -u root -p sss_db < db/mysql/03_procedures.sql
mysql -u root -p sss_db < db/mysql/04_seed.sql

# PostgreSQL
psql -U postgres -f db/postgres/00_create_db.sql
psql -U postgres -d sss_db -f db/postgres/01_schema.sql
psql -U postgres -d sss_db -f db/postgres/02_triggers_change_log.sql
psql -U postgres -d sss_db -f db/postgres/03_procedures.sql
psql -U postgres -d sss_db -f db/postgres/04_seed.sql

# SQL Server (similar pattern)
```

### 2. Backend Configuration

Update `src/main/resources/application.yml`:

```yaml
sss:
  datasource:
    mysql:
      url: jdbc:mysql://localhost:3306/sss_db
      username: root
      password: your_password
    postgres:
      url: jdbc:postgresql://localhost:5432/sss_db
      username: postgres
      password: your_password
    sqlserver:
      url: jdbc:sqlserver://localhost:1433;databaseName=sss_db
      username: sa
      password: your_password
```

### 3. Frontend Setup

```bash
cd frontend
npm install
```

### 4. Run the Application

**Development Mode** (recommended for frontend development):

```bash
# Terminal 1: Start backend
mvn spring-boot:run

# Terminal 2: Start frontend dev server
cd frontend
npm run dev
```

Access at: `http://localhost:5173`

**Production Mode**:

```bash
# Build frontend
cd frontend
npm run build

# Start backend (serves static files)
cd ..
mvn spring-boot:run
```

Access at: `http://localhost:8080`

## User Accounts

### Default Demo Accounts

| Username | Password   | Role  | Description                     |
|----------|-----------|-------|---------------------------------|
| admin    | admin123  | ADMIN | Full access to all features     |
| user1    | user123   | USER  | Order submission only           |

## Project Structure

```
sync_sys/
├── db/                          # Database initialization scripts
│   ├── mysql/
│   ├── postgres/
│   └── sqlserver/
├── frontend/                    # Vue 3 SPA
│   ├── src/
│   │   ├── api/                # Axios API client
│   │   ├── components/         # Vue components
│   │   ├── router/             # Vue Router config
│   │   ├── stores/             # Pinia state management
│   │   └── views/              # Page components
│   ├── package.json
│   └── vite.config.js
├── src/main/
│   ├── java/com/sss/sync/
│   │   ├── config/             # Spring configuration
│   │   ├── domain/             # Domain entities
│   │   ├── infra/              # Infrastructure (mappers)
│   │   ├── service/            # Business logic
│   │   └── web/                # Controllers and DTOs
│   └── resources/
│       ├── static/             # Built frontend files
│       ├── mapper/             # MyBatis XML mappers
│       └── application.yml     # Configuration
├── pom.xml                      # Maven configuration
├── TESTING.md                   # Testing guide
└── README.md                    # This file
```

## API Documentation

When the application is running, access the OpenAPI documentation:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

### Key Endpoints

#### Authentication
- `POST /api/auth/login` - Login and get JWT token

#### Orders
- `GET /api/products` - List all products
- `POST /api/orders/place` - Create new order

#### Admin Only
- `POST /api/queries/order-analytics` - Complex analytics query
- `GET /api/reports/daily-sync` - Daily sync statistics
- `GET /conflicts/view?token=...` - View conflict details

## Features in Detail

### Order Synchronization

Orders can be placed in any of the three databases. The sync engine automatically:
1. Detects changes via trigger-based change log
2. Replicates to other databases
3. Detects and records conflicts
4. Sends email notifications for conflicts

### Conflict Resolution

When conflicts occur (simultaneous updates in multiple DBs):
1. System creates conflict record
2. Email sent to admin with resolution link
3. Admin views conflict details with both versions
4. Admin selects authoritative database
5. System applies resolution and notifies

### Analytics

Complex queries support:
- Multi-table joins across product catalog, orders, and categories
- Date range filtering
- Category and supplier filtering
- Pagination
- Aggregations (totals, averages, unique counts)

### Reporting

Daily sync reports provide:
- Total synced changes over time
- Conflict creation and resolution trends
- Failure tracking
- Interactive charts with zoom and export

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.10
- **Security**: Spring Security with JWT
- **Database**: MyBatis Plus (MySQL, PostgreSQL, SQL Server drivers)
- **API Docs**: SpringDoc OpenAPI 3
- **Email**: Spring Mail

### Frontend
- **Framework**: Vue 3.5
- **Build Tool**: Vite 7
- **Router**: Vue Router 4
- **State**: Pinia 3
- **HTTP**: Axios 1.13
- **Charts**: ECharts 6

## Development

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start dev server with hot reload
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Backend Development

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package

# Run application
mvn spring-boot:run
```

## Testing

See [TESTING.md](TESTING.md) for comprehensive testing guide.

## Configuration

### JWT Configuration

```yaml
sss:
  jwt:
    issuer: "sss-sync"
    secret: "your-secret-key-32-bytes-minimum"
    accessTokenExpireMinutes: 240  # 4 hours
```

### Sync Engine Configuration

```yaml
sss:
  sync:
    enabled: true
    pollIntervalMillis: 10000  # Check for changes every 10 seconds
    batchSize: 200              # Process 200 changes per batch
```

### Email Configuration

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 465
    username: your-email@example.com
    password: your-password

sss:
  mail:
    enabled: true
    from: "your-email@example.com"
    adminTo: "admin@example.com"
    conflictViewBaseUrl: "http://localhost:8080"
```

## Deployment

### Docker Deployment (Future)

A Docker Compose setup will be provided to run all databases and the application.

### Production Considerations

1. **Security**:
   - Change JWT secret to a strong random value
   - Use HTTPS in production
   - Secure database credentials
   - Enable CSRF for non-API endpoints if needed

2. **Performance**:
   - Adjust sync engine poll interval based on load
   - Configure database connection pools
   - Consider Redis for session storage if scaling horizontally

3. **Monitoring**:
   - Add Spring Boot Actuator for health checks
   - Configure logging levels
   - Set up alerts for sync failures

## License

This project is for demonstration and educational purposes.

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## Support

For issues and questions, please open an issue on GitHub.
