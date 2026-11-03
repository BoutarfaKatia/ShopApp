
CREATE TABLE orders (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        total DECIMAL(19, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        version BIGINT NOT NULL DEFAULT 0,

                        CONSTRAINT pk_orders PRIMARY KEY (id),

                        CONSTRAINT fk_orders_user
                            FOREIGN KEY (user_id)
                                REFERENCES user(id),

                                INDEX idx_orders_user_created (user_id, created_at)

);
