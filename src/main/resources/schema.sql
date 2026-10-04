-- The Fliegu Ferries schema. It runs unchanged on H2 and on PostgreSQL, so
-- nothing here may use a dialect of its own.

CREATE TABLE IF NOT EXISTS sailings (
    id                 VARCHAR(64)  NOT NULL,
    departs_from       VARCHAR(16)  NOT NULL,
    arrives_at         VARCHAR(16)  NOT NULL,
    departs_at         TIMESTAMP    NOT NULL,
    passenger_capacity INTEGER      NOT NULL,
    vehicle_capacity   INTEGER      NOT NULL,
    CONSTRAINT pk_sailings PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bookings (
    reference            VARCHAR(32)  NOT NULL,
    sailing_id           VARCHAR(64)  NOT NULL,
    contact_email        VARCHAR(255) NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    reserved_at          TIMESTAMP    NOT NULL,
    payment_reference    VARCHAR(64),
    vehicle_registration VARCHAR(16),
    CONSTRAINT pk_bookings PRIMARY KEY (reference),
    CONSTRAINT fk_bookings_sailing FOREIGN KEY (sailing_id) REFERENCES sailings (id)
);

CREATE TABLE IF NOT EXISTS passengers (
    booking_reference VARCHAR(32)  NOT NULL,
    ordinal           INTEGER      NOT NULL,
    name              VARCHAR(128) NOT NULL,
    date_of_birth     DATE         NOT NULL,
    gozo_resident     BOOLEAN      NOT NULL,
    CONSTRAINT pk_passengers PRIMARY KEY (booking_reference, ordinal),
    CONSTRAINT fk_passengers_booking FOREIGN KEY (booking_reference) REFERENCES bookings (reference)
);

CREATE INDEX IF NOT EXISTS ix_bookings_sailing ON bookings (sailing_id);
