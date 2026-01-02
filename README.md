# SSS Sync System

Multi-database synchronization system with conflict resolution.

## Architecture

This system synchronizes data across three databases:
- MySQL
- PostgreSQL
- SQL Server

### Sync Mechanism

The system uses database triggers to capture changes in `change_log` tables, which are then processed by the sync engine to replicate data across databases.

## Infinite Loop Prevention

To prevent infinite feedback loops where sync operations trigger more change_log entries, the system implements a **session-level skip flag mechanism**:

### How It Works

1. **Database Triggers Honor Skip Flags**: Each database has been configured to check a session-level flag before writing to `change_log`:
   - **MySQL**: Triggers check `@sss_skip_changelog = 1` session variable
   - **PostgreSQL**: Triggers check `current_setting('sss.skip_changelog', true) = '1'`
   - **SQL Server**: Triggers check `SESSION_CONTEXT('sss_skip_changelog') = 1`

2. **Sync Engine Sets Skip Flags**: Before performing any cross-database upsert operation, the sync engine:
   - Sets the appropriate skip flag for the target database
   - Executes the upsert operation
   - Clears the skip flag

3. **Transaction Isolation**: Each upsert operation is wrapped in a `@Transactional` block using the correct transaction manager for the target database, ensuring:
   - The skip flag and upsert execute on the same connection
   - The flag is scoped to the transaction/session only
   - No interference with concurrent operations

### Implementation Details

#### MySQL Trigger Example
```sql
CREATE TRIGGER trg_product_info_au AFTER UPDATE ON product_info
    FOR EACH ROW
lbl_skip: BEGIN
    IF @sss_skip_changelog = 1 THEN
        LEAVE lbl_skip;
    END IF;
    INSERT INTO change_log(...) VALUES (...);
END
```

#### PostgreSQL Trigger Example
```sql
CREATE OR REPLACE FUNCTION fn_log_generic()
RETURNS TRIGGER AS $$
BEGIN
  IF current_setting('sss.skip_changelog', true) = '1' THEN
    RETURN COALESCE(NEW, OLD);
  END IF;
  INSERT INTO change_log(...) VALUES (...);
  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;
```

#### SQL Server Trigger Example
```sql
CREATE TRIGGER dbo.trg_product_info_log ON dbo.product_info
    AFTER INSERT, UPDATE, DELETE AS
BEGIN
  IF CAST(SESSION_CONTEXT(N'sss_skip_changelog') AS INT) = 1
  BEGIN
    RETURN;
  END;
  INSERT INTO dbo.change_log(...) SELECT ...;
END
```

#### Java Service Example
```java
@Transactional(transactionManager = "mysqlTxManager")
public void upsertProductMysql(Map<String, Object> row) {
    mysqlBiz.setSkipChangeLog();  // SET @sss_skip_changelog = 1
    mysqlBiz.upsertProduct(row);   // Upsert won't trigger change_log
    mysqlBiz.clearSkipChangeLog(); // SET @sss_skip_changelog = 0
}
```

### Benefits

- **Prevents Infinite Loops**: Sync operations don't trigger new change_log entries
- **User Changes Still Tracked**: Normal application writes (without the skip flag) continue to generate change_log entries as expected
- **No Performance Impact**: Flag check is fast and occurs only during trigger execution
- **Thread-Safe**: Session-level flags don't interfere with concurrent operations

## Development

### Building

```bash
mvn clean package
```

### Running

```bash
mvn spring-boot:run
```

## Database Setup

Execute the SQL scripts in the `db/` directory for each database:
1. Schema creation scripts
2. Trigger scripts (with skip flag support)
