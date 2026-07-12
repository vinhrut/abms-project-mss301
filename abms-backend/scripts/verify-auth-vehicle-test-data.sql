SELECT role_id, role_name FROM roles ORDER BY role_id;

SELECT user_id, email, status, role_id
FROM users
ORDER BY email;

SELECT apartment_id, room_number, floor, area, status
FROM apartments;

SELECT resident_id, apartment_id, user_id, relationship, residence_type, status
FROM apartment_residents
ORDER BY created_at, user_id;

SELECT apartment_id, vehicle_type, max_quantity
FROM vehicle_limits
ORDER BY vehicle_type;

SELECT vehicle_id, license_plate, type, status, apartment_id, owner_id
FROM vehicles
ORDER BY license_plate;

SELECT
    u.email,
    (crypt('11111111', u.password) = u.password) AS password_matches_seed_password
FROM users u
ORDER BY u.email;

SELECT
    apartment_id,
    type,
    COUNT(*) FILTER (WHERE status = 'APPROVED') AS approved_count,
    COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_count
FROM vehicles
GROUP BY apartment_id, type
ORDER BY apartment_id, type;