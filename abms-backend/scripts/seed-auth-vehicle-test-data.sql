-- Deprecated: this script was created for the old single-database setup.
-- The project now follows database-per-service with separate PostgreSQL
-- containers. Initial data is seeded by each service DataInitializer:
--   auth-service/config/DataInitializer.java
--   apartment-service/config/DataInitializer.java
--   vehicle-service/config/DataInitializer.java
--
-- Do not run this script against the new separated service databases because it
-- creates tables from multiple bounded contexts in the same database.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS roles (
    role_id INT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY,
    role_id INT NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    id_card VARCHAR(50),
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS apartments (
    apartment_id UUID PRIMARY KEY,
    building_id UUID NOT NULL,
    room_number VARCHAR(255) NOT NULL,
    floor INT NOT NULL,
    area NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS apartment_residents (
    resident_id UUID PRIMARY KEY,
    apartment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    residence_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS vehicle_limits (
    limit_id UUID PRIMARY KEY,
    apartment_id UUID NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    max_quantity INT NOT NULL
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id UUID PRIMARY KEY,
    apartment_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    license_plate VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL
);

TRUNCATE TABLE vehicles, vehicle_limits RESTART IDENTITY;
TRUNCATE TABLE apartment_residents RESTART IDENTITY;
TRUNCATE TABLE apartments RESTART IDENTITY;
TRUNCATE TABLE users RESTART IDENTITY;
TRUNCATE TABLE roles RESTART IDENTITY CASCADE;

INSERT INTO roles (role_id, role_name)
VALUES
    (1, 'RESIDENT'),
    (2, 'STAFF'),
    (3, 'MANAGER')
ON CONFLICT (role_id) DO UPDATE SET role_name = EXCLUDED.role_name;

INSERT INTO users (user_id, role_id, email, password, full_name, phone, id_card, status)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        1,
        'resident1@abms.local',
        crypt('11111111', gen_salt('bf')),
        'Resident One',
        '0900000001',
        'ID-RESIDENT-001',
        'ACTIVE'
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        2,
        'staff1@abms.local',
        crypt('11111111', gen_salt('bf')),
        'Staff One',
        '0900000002',
        'ID-STAFF-001',
        'ACTIVE'
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        3,
        'manager1@abms.local',
        crypt('11111111', gen_salt('bf')),
        'Manager One',
        '0900000003',
        'ID-MANAGER-001',
        'ACTIVE'
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        1,
        'resident2@abms.local',
        crypt('11111111', gen_salt('bf')),
        'Resident Two',
        '0900000004',
        'ID-RESIDENT-002',
        'ACTIVE'
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        1,
        'resident-pending@abms.local',
        crypt('11111111', gen_salt('bf')),
        'Resident Pending',
        '0900000005',
        'ID-RESIDENT-003',
        'PENDING_APPROVAL'
    )
ON CONFLICT (email) DO UPDATE SET
    role_id = EXCLUDED.role_id,
    password = EXCLUDED.password,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    id_card = EXCLUDED.id_card,
    status = EXCLUDED.status;

INSERT INTO apartments (apartment_id, building_id, room_number, floor, area, status)
VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'A-101',
    1,
    75.50,
    'OCCUPIED'
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'A-102',
    1,
    68.00,
    'OCCUPIED'
)
ON CONFLICT (apartment_id) DO UPDATE SET
    building_id = EXCLUDED.building_id,
    room_number = EXCLUDED.room_number,
    floor = EXCLUDED.floor,
    area = EXCLUDED.area,
    status = EXCLUDED.status;

INSERT INTO apartment_residents (
    resident_id,
    apartment_id,
    user_id,
    relationship,
    residence_type,
    status,
    created_at,
    approved_at,
    rejected_at
)
VALUES
    (
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'OWNER',
        'PERMANENT',
        'ACTIVE',
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '9 days',
        NULL
    ),
    (
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab',
        '44444444-4444-4444-4444-444444444444',
        'TENANT',
        'TEMPORARY',
        'ACTIVE',
        NOW() - INTERVAL '8 days',
        NOW() - INTERVAL '7 days',
        NULL
    ),
    (
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee3',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '55555555-5555-5555-5555-555555555555',
        'FAMILY',
        'TEMPORARY',
        'PENDING_APPROVAL',
        NOW() - INTERVAL '1 day',
        NULL,
        NULL
    )
ON CONFLICT (resident_id) DO UPDATE SET
    apartment_id = EXCLUDED.apartment_id,
    user_id = EXCLUDED.user_id,
    relationship = EXCLUDED.relationship,
    residence_type = EXCLUDED.residence_type,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    approved_at = EXCLUDED.approved_at,
    rejected_at = EXCLUDED.rejected_at;

INSERT INTO vehicle_limits (limit_id, apartment_id, vehicle_type, max_quantity)
VALUES
    ('c1111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MOTORBIKE', 2),
    ('c2222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'CAR', 1),
    ('c3333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'MOTORBIKE', 2),
    ('c4444444-4444-4444-4444-444444444444', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'CAR', 1)
ON CONFLICT (limit_id) DO UPDATE SET
    apartment_id = EXCLUDED.apartment_id,
    vehicle_type = EXCLUDED.vehicle_type,
    max_quantity = EXCLUDED.max_quantity;

INSERT INTO vehicles (vehicle_id, apartment_id, owner_id, license_plate, type, brand, status)
VALUES
    (
        'd1111111-1111-1111-1111-111111111111',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        '29A-99999',
        'MOTORBIKE',
        'Honda',
        'APPROVED'
    ),
    (
        'd2222222-2222-2222-2222-222222222222',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        '30B-12345',
        'CAR',
        'Toyota',
        'PENDING'
    ),
    (
        'd3333333-3333-3333-3333-333333333333',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab',
        '44444444-4444-4444-4444-444444444444',
        '29B-67890',
        'MOTORBIKE',
        'Yamaha',
        'PENDING'
    )
ON CONFLICT (license_plate) DO UPDATE SET
    apartment_id = EXCLUDED.apartment_id,
    owner_id = EXCLUDED.owner_id,
    type = EXCLUDED.type,
    brand = EXCLUDED.brand,
    status = EXCLUDED.status;