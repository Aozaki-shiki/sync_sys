# Manual Testing Guide for Conflict Resolution Feature

## Prerequisites
1. Three databases running (MySQL, PostgreSQL, SQL Server)
2. Application configured with correct database credentials in `application.yml`
3. Email configured (or mock email service)

## Test Scenario: Simulate and Resolve a Conflict

### Step 1: Start the Application
```bash
cd /home/runner/work/sync_sys/sync_sys
mvn spring-boot:run
```

The application will start on port 8080.

### Step 2: Create a Conflict Manually

**Option A: Using Database Clients**

1. Update the same product in two different databases with different versions:

```sql
-- In MySQL:
UPDATE product_info 
SET product_name = 'Updated from MySQL', 
    version = 10, 
    updated_at = '2024-01-01 10:00:00' 
WHERE product_id = 1;

-- In PostgreSQL:
UPDATE product_info 
SET product_name = 'Updated from PostgreSQL', 
    version = 11, 
    updated_at = '2024-01-01 11:00:00' 
WHERE product_id = 1;
```

2. Wait for sync engine to detect the conflict (runs every 1 second based on config)
3. Check email for conflict notification with token link

**Option B: Check Existing Conflicts**

```sql
-- In MySQL:
SELECT * FROM conflict_record WHERE status = 'OPEN';
```

If there's already an open conflict, you can use that for testing.

### Step 3: Access the Conflict View

1. Copy the token from the email (or generate one manually if needed)
2. Open browser and navigate to:
   ```
   http://localhost:8080/conflicts/view?token=YOUR_TOKEN_HERE
   ```

3. You should see:
   - Conflict details (table, pk, source/target DBs)
   - Version and timestamp information  
   - JSON payloads from source and target
   - **Resolution form with radio buttons** for MYSQL/POSTGRES/SQLSERVER
   - Submit button to resolve

### Step 4: Resolve the Conflict

1. Select one database as authoritative (e.g., POSTGRES)
2. Click "解决冲突 (Resolve Conflict)"
3. You should see a success message
4. Page will auto-reload after 3 seconds

### Step 5: Verify Resolution

**Check the Conflict Record:**
```sql
-- In MySQL:
SELECT conflict_id, status, resolved_by, resolved_at, resolution
FROM conflict_record 
WHERE conflict_id = YOUR_CONFLICT_ID;

-- Should show:
-- status = 'RESOLVED'
-- resolved_by = 'admin' (or the admin username from token)
-- resolved_at = current timestamp
-- resolution = 'POSTGRES' (or whatever DB you selected)
```

**Check Data Consistency:**
```sql
-- In MySQL:
SELECT product_id, product_name, version, updated_at 
FROM product_info 
WHERE product_id = 1;

-- In PostgreSQL:
SELECT product_id, product_name, version, updated_at 
FROM product_info 
WHERE product_id = 1;

-- In SQL Server:
SELECT product_id, product_name, version, updated_at 
FROM dbo.product_info 
WHERE product_id = 1;

-- All three should now have IDENTICAL values:
-- - Same product_name (from authoritative DB)
-- - Same version (from authoritative DB)
-- - Same updated_at timestamp (from authoritative DB)
```

**Verify No New Conflicts Created:**
```sql
-- Wait a few seconds for sync to process, then check:
SELECT COUNT(*) FROM conflict_record 
WHERE table_name = 'product_info' 
  AND pk_value = '1' 
  AND status = 'OPEN';

-- Should be 0 (no new conflicts)
```

### Step 6: Test Infinite Loop Prevention

1. Monitor the change_log table:
```sql
SELECT change_id, db_code, table_name, pk_value, row_version, created_at
FROM change_log
WHERE table_name = 'product_info' AND pk_value = '1'
ORDER BY change_id DESC
LIMIT 10;
```

2. You should see:
   - Change log entries from the propagation to MYSQL and SQLSERVER
   - But NO new conflict records created
   - Sync engine processes these changes without errors

3. Check application logs for:
   - No error messages
   - Successful upsert operations
   - No new conflict creation

## Expected Behavior

### Success Indicators:
✅ Conflict view page loads correctly with resolution form
✅ Form submission returns success response
✅ Conflict status changes to RESOLVED
✅ All three databases have identical data
✅ No new conflicts are created
✅ Sync engine continues to work normally

### Failure Indicators:
❌ 404 or 403 errors (check SecurityConfig)
❌ Token validation errors (check token generation/parsing)
❌ Data not propagated to all databases
❌ New conflicts created after resolution (infinite loop)
❌ Version/timestamp mismatch across databases

## Testing the UI

### Visual Verification:

1. **Before Resolution (OPEN status):**
   - Page should show conflict details
   - Resolution form should be visible with 3 radio buttons
   - Submit button should be enabled

2. **During Resolution:**
   - Message should show "正在处理..."
   - Button should be disabled (optional enhancement)

3. **After Resolution (RESOLVED status):**
   - Page should reload
   - Resolution form should disappear
   - Resolution information section should appear showing:
     - Resolved By: admin
     - Resolved At: timestamp
     - Resolution: POSTGRES (or selected DB)

## API Testing with curl

```bash
# Get conflict view (should return HTML)
curl "http://localhost:8080/conflicts/view?token=YOUR_TOKEN"

# Resolve conflict (should return JSON)
curl -X POST "http://localhost:8080/conflicts/resolve?token=YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"authoritativeDb":"POSTGRES"}'

# Expected response:
# {"success":true,"message":"Conflict resolved successfully"}
```

## Troubleshooting

### Issue: Token expired
- Generate a new token using ConflictLinkTokenService
- Default expiration is 24 hours

### Issue: Conflict not found
- Check conflict_id in token matches database
- Ensure conflict exists and is in OPEN status

### Issue: Data not propagated
- Check database connections
- Verify mapper queries work
- Check application logs for errors

### Issue: Infinite loop detected
- This shouldn't happen, but if it does:
  - Check version/timestamp values in all DBs
  - Verify upsert queries explicitly set updated_at
  - Check isConflict() logic in SyncEngineService
