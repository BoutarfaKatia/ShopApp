CREATE TABLE refresh_token (
                               id         BIGINT      NOT NULL AUTO_INCREMENT,
                               token_hash CHAR(64)    NOT NULL,
                               user_id    BIGINT      NOT NULL,
                               expires_at DATETIME(6) NOT NULL,
                               revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
                               CONSTRAINT pk_refresh_token PRIMARY KEY (id),
                               CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
                               CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                               INDEX idx_refresh_token_user (user_id)
);