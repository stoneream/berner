CREATE DATABASE IF NOT EXISTS berner
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS berner_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'berner'@'%' IDENTIFIED BY 'berner';

GRANT ALL PRIVILEGES ON berner.* TO 'berner'@'%';
GRANT ALL PRIVILEGES ON berner_test.* TO 'berner'@'%';

FLUSH PRIVILEGES;
