
CREATE TABLE user (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       role VARCHAR(50) NOT NULL,

                       CONSTRAINT pk_user PRIMARY KEY (id),
                       CONSTRAINT uk_user_email UNIQUE (email)
);
