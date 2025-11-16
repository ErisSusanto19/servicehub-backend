CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE services (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19, 2) NOT NULL,

    provider_id UUID NOT NULL,
    category_id UUID NOT NULL,

    CONSTRAINT fk_services_provider
        FOREIGN KEY (provider_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_services_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_services_provider_id ON services (provider_id);
CREATE INDEX idx_services_category_id ON services (category_id);