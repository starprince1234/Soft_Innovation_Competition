UPDATE users SET role='MANAGER', status='ACTIVE' WHERE username='admin';
UPDATE users SET role='ARCHAEOLOGIST', status='ACTIVE' WHERE username='arch_li';
SELECT id, username, role, status FROM users WHERE username IN ('admin','arch_li');
