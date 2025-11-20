CREATE TABLE payouts (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    payout_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payouts_provider
        FOREIGN KEY (provider_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_payouts_provider_id ON payouts (provider_id);

CREATE TABLE payout_items (
    id UUID PRIMARY KEY,
    payout_id UUID NOT NULL,
    order_id UUID NOT NULL UNIQUE,
    amount DECIMAL(19, 2) NOT NULL,

    CONSTRAINT fk_payout_items_payout
        FOREIGN KEY (payout_id)
        REFERENCES payouts (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_payout_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_payout_items_payout_id ON payout_items (payout_id);