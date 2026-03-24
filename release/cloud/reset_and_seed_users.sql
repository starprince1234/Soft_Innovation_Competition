-- 先查一条已知密码为 123456 的 bcrypt 哈希，若无则使用固定哈希兜底
SET @pwd_hash := (
  SELECT password_hash
  FROM users
  WHERE username LIKE 'fav_e2e_%'
  ORDER BY id DESC
  LIMIT 1
);

SET @pwd_hash := IFNULL(@pwd_hash, '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi4wB6fQ9mN4Kx4d5D4wY/boYYGr2eK');

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_favorites;
TRUNCATE TABLE dialog_records;
TRUNCATE TABLE detection_tasks;
TRUNCATE TABLE feedback;
TRUNCATE TABLE user_team_membership;
TRUNCATE TABLE teams;
TRUNCATE TABLE artifacts;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (username, password_hash, role, status) VALUES
('admin', @pwd_hash, 'MANAGER', 'ACTIVE'),
('arc_li', @pwd_hash, 'ARCHAEOLOGIST', 'ACTIVE'),
('user', @pwd_hash, 'PUBLIC', 'ACTIVE');

SELECT id, username, role, status FROM users ORDER BY id;
