USE sss_db;

-- sync_checkpoint: stores last processed change_id per source DB
DROP TABLE IF EXISTS sync_checkpoint;
CREATE TABLE sync_checkpoint (
                                 checkpoint_id  BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 source_db      VARCHAR(20) NOT NULL UNIQUE, -- MYSQL / POSTGRES
                                 last_change_id BIGINT NOT NULL DEFAULT 0,
                                 updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

-- init rows (must exist, otherwise UPDATE ... WHERE source_db=? affects 0 rows forever)
INSERT INTO sync_checkpoint(source_db, last_change_id)
VALUES ('MYSQL', 0), ('POSTGRES', 0)
    ON DUPLICATE KEY UPDATE last_change_id = VALUES(last_change_id);