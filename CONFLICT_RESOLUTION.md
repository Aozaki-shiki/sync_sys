# Conflict Resolution Feature

## Overview
This document describes the conflict resolution feature that allows an admin to manually resolve data synchronization conflicts by choosing an authoritative database.

## How It Works

### 1. Conflict Detection
When the sync engine detects a conflict (version or timestamp mismatch between source and target), it:
- Creates a conflict_record with status='OPEN'
- Sends an email to the admin with a token-based link
- Skips the sync operation for that record

### 2. Conflict View
The admin receives an email with a link like: `http://localhost:8080/conflicts/view?token=...`

This page shows:
- Conflict details (table, pk, source/target DBs)
- Version and timestamp information
- JSON payloads from both source and target
- A resolution form (for OPEN conflicts)

### 3. Conflict Resolution
The admin selects one of the three databases (MYSQL/POSTGRES/SQLSERVER) as authoritative and clicks "Resolve Conflict".

The system then:
1. Validates the token
2. Fetches the **latest** data from the selected authoritative database (not the stored payload)
3. Parses the JSON into a Map
4. Upserts the data to the other two databases
5. Updates the conflict_record to status='RESOLVED'

### 4. Infinite Loop Prevention

**Key Insight**: When we resolve a conflict and propagate data, we copy the exact `version` and `updated_at` from the authoritative DB to all other DBs.

**How it prevents loops**:

1. After resolution, all three DBs have identical `version` and `updated_at` values
2. The upsert operations trigger UPDATE events in the target DBs
3. These create new change_log entries
4. When the sync engine processes these entries, it compares:
   ```java
   if (tgtVer > srcVer) return true;  // Conflict
   if (tgtVer.equals(srcVer) && tgtUpd.isAfter(srcUpd)) return true;  // Conflict
   return false;  // No conflict
   ```
5. Since all DBs have the same version and timestamp, `isConflict()` returns **false**
6. The sync proceeds with a simple upsert (essentially a no-op since data is identical)
7. No new conflict is created

**Example Flow**:
```
Initial State:
- MYSQL: version=5, updated_at=2024-01-01 10:00:00
- POSTGRES: version=6, updated_at=2024-01-01 11:00:00  ← Conflict!
- SQLSERVER: version=4, updated_at=2024-01-01 09:00:00

Admin selects POSTGRES as authoritative:
- System fetches data from POSTGRES (version=6, updated_at=2024-01-01 11:00:00)
- Upserts to MYSQL with version=6, updated_at=2024-01-01 11:00:00
- Upserts to SQLSERVER with version=6, updated_at=2024-01-01 11:00:00

After Resolution:
- MYSQL: version=6, updated_at=2024-01-01 11:00:00
- POSTGRES: version=6, updated_at=2024-01-01 11:00:00
- SQLSERVER: version=6, updated_at=2024-01-01 11:00:00

Subsequent Sync (from change_log):
- MYSQL→POSTGRES: version matches, timestamp matches → no conflict, upsert (no-op)
- MYSQL→SQLSERVER: version matches, timestamp matches → no conflict, upsert (no-op)
- SQLSERVER→MYSQL: version matches, timestamp matches → no conflict, upsert (no-op)
- SQLSERVER→POSTGRES: version matches, timestamp matches → no conflict, upsert (no-op)
```

### 5. Important Notes

- The upsert statements explicitly set `updated_at = VALUES(updated_at)`, which overrides the `ON UPDATE CURRENT_TIMESTAMP` behavior
- The system only allows one OPEN conflict per (table_name, pk_value) due to unique constraint
- Once resolved, a conflict can have a new OPEN conflict created if another version mismatch occurs
- The token has a 24-hour expiration by default

## API Endpoints

### GET /conflicts/view?token=...
- **Auth**: None (token-based)
- **Purpose**: View conflict details and resolution form
- **Returns**: HTML page

### POST /conflicts/resolve?token=...
- **Auth**: None (token-based)
- **Body**: `{"authoritativeDb": "MYSQL|POSTGRES|SQLSERVER"}`
- **Purpose**: Resolve the conflict by propagating data from authoritative DB
- **Returns**: `{"success": true/false, "message": "..."}`

## Security

- Both endpoints are exempt from JWT authentication (configured in SecurityConfig)
- Token-based access control using conflict link tokens (24-hour expiration)
- Tokens are generated with conflictId and admin username embedded
