CREATE TABLE IF NOT EXISTS sync_checkpoint (
  checkpoint_id BIGSERIAL PRIMARY KEY,
  source_db     VARCHAR(20) NOT NULL UNIQUE,
  last_change_id BIGINT NOT NULL DEFAULT 0,
  updated_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sync_checkpoint(source_db, last_change_id)
VALUES ('MYSQL', 0), ('POSTGRES', 0)
ON CONFLICT (source_db) DO UPDATE SET last_change_id = EXCLUDED.last_change_id;