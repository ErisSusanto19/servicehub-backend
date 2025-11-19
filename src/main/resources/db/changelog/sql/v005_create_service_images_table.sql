CREATE TABLE service_images (
    id UUID PRIMARY KEY,
    image_url TEXT NOT NULL,
    service_id UUID NOT NULL,

    CONSTRAINT fk_service_images_service
        FOREIGN KEY (service_id)
        REFERENCES services (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_service_images_service_id ON service_images (service_id);