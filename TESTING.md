# Testing Guide - Vue SPA with Role-Based Access Control

This guide explains how to test the unified login-driven Vue SPA with role-based access control.

## Prerequisites

Before testing, ensure you have:

1. **Database Setup**:
   - MySQL (port 3306)
   - PostgreSQL (port 5432)
   - SQL Server (port 1433)
   - All databases initialized with schema from `db/` directory

2. **Backend Configuration**:
   - Update `src/main/resources/application.yml` with correct database credentials
   - JWT secret configured
   - Mail server configured (if testing conflict notifications)

3. **Frontend Dependencies**:
   ```bash
   cd frontend
   npm install
   ```

## Development Testing

### Option 1: Frontend Dev Server with Backend API Proxy

This is the recommended approach for frontend development:

1. **Start the Spring Boot Backend**:
   ```bash
   mvn spring-boot:run
   ```
   Backend will run on `http://localhost:8080`

2. **Start the Frontend Dev Server**:
   ```bash
   cd frontend
   npm run dev
   ```
   Frontend will run on `http://localhost:5173`
   API requests are automatically proxied to the backend

3. **Access the Application**:
   Open `http://localhost:5173` in your browser

### Option 2: Production Build

Test the production build served by Spring Boot:

1. **Build the Frontend**:
   ```bash
   cd frontend
   npm run build
   ```
   This creates production files in `src/main/resources/static/`

2. **Start Spring Boot**:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the Application**:
   Open `http://localhost:8080` in your browser

## Test Scenarios

### 1. Login Flow

**Test USER Login**:
1. Navigate to the application root (`/`)
2. You should be redirected to `/login`
3. Enter credentials:
   - Username: `user1`
   - Password: `user123`
4. Click "Login"
5. **Expected**: Redirect to `/orders/new` (Order Submission page)

**Test ADMIN Login**:
1. Navigate to `/login`
2. Enter credentials:
   - Username: `admin`
   - Password: `admin123`
3. Click "Login"
4. **Expected**: Redirect to `/admin` (Admin Console with Complex Query page)

### 2. Role-Based Access Control

**Test USER Restrictions**:
1. Login as `user1`
2. Try to manually navigate to `/admin` in the address bar
3. **Expected**: Redirected back to `/orders/new`
4. Verify the user can:
   - Access `/orders/new` ✓
   - Cannot access `/admin/*` routes ✗

**Test ADMIN Access**:
1. Login as `admin`
2. Verify the admin can access:
   - `/admin/queries/complex` ✓
   - `/admin/reports/daily-sync` ✓
   - `/admin/conflicts` ✓
   - `/orders/new` ✓ (Admins can also place orders)

### 3. User Features

**Order Submission (USER or ADMIN)**:
1. Login as `user1` or `admin`
2. Navigate to `/orders/new`
3. Verify the page displays:
   - Product dropdown (loaded from API)
   - Quantity input
   - Shipping address textarea
   - Target database selector (MySQL/PostgreSQL)
4. Fill out the form:
   - Select a product
   - Enter quantity (e.g., 5)
   - Enter shipping address
   - Select target database
5. Click "Submit Order"
6. **Expected**:
   - Success message with Order ID and database
   - Form resets
   - Order is created in the selected database

### 4. Admin Features

**Complex Query Page** (`/admin/queries/complex`):
1. Login as `admin`
2. Click "Complex Query" in the admin sidebar
3. Verify:
   - Date range inputs (defaulted to last month)
   - Optional category and supplier filters
   - Page size selector
4. Click "Execute Query"
5. **Expected**:
   - Results table with columns: Category, Supplier, Total Orders, etc.
   - Pagination controls
   - Summary statistics (Total Records, Current Page, Results Shown)

**Daily Sync Report** (`/admin/reports/daily-sync`):
1. Login as `admin`
2. Click "Daily Sync Report" in the admin sidebar
3. Verify:
   - Time range selector (7, 14, 30, 60, 90 days)
   - Summary cards (Total Synced Changes, Conflicts, Failures)
   - Two interactive charts:
     - Daily Sync Operations (bar chart)
     - Conflict Tracking (line chart with area)
4. Change time range and click "Refresh"
5. **Expected**:
   - Charts update with new data
   - Statistics recalculate

**Conflict Management** (`/admin/conflicts`):
1. Login as `admin`
2. Click "Conflict Management" in the admin sidebar
3. Verify:
   - Information about conflict management
   - Token input field
   - Instructions for accessing conflicts
4. Enter a valid conflict token (if available)
5. Click "View Conflict"
6. **Expected**:
   - Opens conflict details page in new tab
   - Shows conflict information with resolution form

### 5. Logout

**Test Logout**:
1. Login as any user
2. Click "Logout" button
   - For USER: Button in header of order page
   - For ADMIN: Button in sidebar footer
3. **Expected**:
   - Redirected to `/login`
   - Token cleared from localStorage
   - Cannot access protected routes without logging in again

### 6. Authentication Persistence

**Test Token Persistence**:
1. Login as any user
2. Refresh the page (F5)
3. **Expected**:
   - User remains logged in
   - Redirected to appropriate page for their role
4. Close browser and reopen
5. Navigate to the application
6. **Expected**:
   - If token hasn't expired: User is still logged in
   - If token expired: Redirected to login page

### 7. Unauthorized Access

**Test Without Login**:
1. Open browser in incognito/private mode
2. Try to access protected routes directly:
   - `/orders/new`
   - `/admin`
   - `/admin/queries/complex`
3. **Expected**: All redirected to `/login`

**Test Expired Token**:
1. Login
2. Manually expire the token in localStorage (set past date) or wait 4 hours
3. Try to access protected routes
4. **Expected**: Redirected to `/login`

## Testing Checklist

- [ ] USER can login and access order submission
- [ ] ADMIN can login and access admin console
- [ ] USER cannot access admin routes
- [ ] ADMIN can access all routes
- [ ] Order submission works correctly
- [ ] Complex query returns and displays results
- [ ] Daily sync report loads and displays charts
- [ ] Conflict management page loads
- [ ] Logout works and clears authentication
- [ ] Unauthenticated users are redirected to login
- [ ] Token persists across page refreshes
- [ ] 401 responses redirect to login
- [ ] Responsive design works on mobile

## Browser Testing

Test in the following browsers:
- [ ] Chrome (latest)
- [ ] Firefox (latest)
- [ ] Safari (latest)
- [ ] Edge (latest)

## Responsive Testing

Test the following screen sizes:
- [ ] Desktop (1920x1080)
- [ ] Laptop (1366x768)
- [ ] Tablet (768x1024)
- [ ] Mobile (375x667)

## API Endpoints Used

The frontend makes requests to:
- `POST /api/auth/login` - Authentication
- `GET /api/products` - Product list
- `POST /api/orders/place` - Create order
- `POST /api/queries/order-analytics` - Complex analytics
- `GET /api/reports/daily-sync` - Sync statistics
- `/conflicts/view?token=...` - Conflict details (separate page)

## Known Limitations

1. Conflict management uses a separate token-based authentication system outside the SPA
2. The conflict view page is a standalone HTML page served by Spring Boot, not part of the Vue SPA
3. ECharts bundle is large (~1.1MB) for the daily sync report - consider code splitting for production

## Troubleshooting

**Frontend won't start**:
- Check Node.js version (20.x required)
- Run `npm install` in frontend directory
- Check port 5173 is available

**Cannot login**:
- Verify backend is running on port 8080
- Check database connections
- Verify user exists in database (admin/admin123, user1/user123)

**401 Unauthorized errors**:
- Check JWT secret in application.yml
- Verify token hasn't expired (default: 4 hours)
- Check Authorization header in browser dev tools

**Charts not displaying**:
- Check browser console for errors
- Verify API returns data
- Check that echarts library loaded correctly
