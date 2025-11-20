CREATE TABLE order_notes (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_notes_order
        FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_notes_author
        FOREIGN KEY (author_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_order_notes_order_id ON order_notes (order_id);