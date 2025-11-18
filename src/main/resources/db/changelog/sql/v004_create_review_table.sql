CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    customer_id UUID NOT NULL,
    service_id UUID NOT NULL,
    order_item_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reviews_customer
        FOREIGN KEY (customer_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_reviews_service
        FOREIGN KEY (service_id)
        REFERENCES services (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_reviews_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_items (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_reviews_service_id ON reviews (service_id);
CREATE INDEX idx_reviews_customer_id ON reviews (customer_id);