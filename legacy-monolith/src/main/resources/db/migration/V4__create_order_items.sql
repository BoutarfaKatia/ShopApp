
CREATE TABLE order_item (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL CHECK ( quantity > 0 ),
                             unit_price DECIMAL(19, 2) NOT NULL,

                             CONSTRAINT pk_order_item PRIMARY KEY (id),

                             CONSTRAINT fk_order_item_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(id),

                             CONSTRAINT fk_order_item_product
                                 FOREIGN KEY (product_id)
                                     REFERENCES product(id)
);
