package shared.repository

val dummyLoginQueryList = listOf(
    "INSERT INTO login_users (id, prefix_id, password, suffix_id, status, last_login_at, deleted_at) VALUES ('01922d5e-9721-77f0-8093-55f799339493', 'loginId', 'laudem', '10', 'ACTIVE', '2024-09-26T10:07:06.7605+02:00', NULL)",
    "INSERT INTO login_users (id, prefix_id, password, suffix_id, status, last_login_at, deleted_at) VALUES ('01922d5e-9721-77f0-8093-55f799339494', 'testId', 'laudem', '11', 'INACTIVE', '2024-09-26T10:07:06.7605+02:00', NULL)",
    "INSERT INTO login_users (id, prefix_id, password, suffix_id, status, last_login_at, deleted_at) VALUES ('01922d5e-9721-77f0-8093-55f799339495', 'testI2', 'laudem', '12', 'DELETED', '2024-09-26T10:07:06.7605+02:00', NULL)",
)
