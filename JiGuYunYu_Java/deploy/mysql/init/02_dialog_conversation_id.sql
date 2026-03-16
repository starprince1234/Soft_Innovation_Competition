SET @col_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dialog_records'
    AND column_name = 'conversation_id'
);

SET @sql = IF(
  @col_exists = 0,
  'ALTER TABLE dialog_records ADD COLUMN conversation_id BIGINT NULL AFTER artifact_id',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'dialog_records'
    AND index_name = 'idx_dialog_conversation'
);

SET @sql = IF(
  @idx_exists = 0,
  'CREATE INDEX idx_dialog_conversation ON dialog_records (user_id, conversation_id)',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
