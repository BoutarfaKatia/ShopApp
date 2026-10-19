
CREATE TABLE product (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          name VARCHAR(150)  NOT NULL UNIQUE ,
                          description VARCHAR(2000) NULL,
                          price DECIMAL(19, 2) NOT NULL CHECK ( price>=0 ),
                          stock INT NOT NULL CHECK ( stock >=0 ),
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          version BIGINT NOT NULL DEFAULT 0,

                          CONSTRAINT pk_product PRIMARY KEY (id)
);
