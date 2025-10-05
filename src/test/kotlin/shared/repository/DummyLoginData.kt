package shared.repository

val dummyLoginQueryList = listOf(
  "DELETE FROM  login_users WHERE id in ('01922d5e-9721-77f0-8093-55f799339493', '01922d5e-9721-77f0-8093-55f799339494', '01922d5e-9721-77f0-8093-55f799339495')",
  "INSERT INTO login_users (id, prefix_id, suffix_id, password, rsa_uid, status, role, last_login_at, deleted_at) VALUES ('01922d5e-9721-77f0-8093-55f799339493', 'loginId', 'laudem', '10', '01922d5e-9721-77f0-8093-55f799339493', 'ACTIVE', 'USER','2024-09-26T10:07:06.7605+02:00', NULL)",
  "INSERT INTO login_users (id, prefix_id, suffix_id, password, rsa_uid, status, role, last_login_at, deleted_at) VALUES ('01922d5e-9721-77f0-8093-55f799339494', 'testId', 'laudem', '11', '01922d5e-9721-77f0-8093-55f799339494', 'INACTIVE', 'ADMIN','2024-09-26T10:07:06.7605+02:00', NULL)",
  "INSERT INTO login_users (id, prefix_id, suffix_id, password, rsa_uid, status, role, last_login_at, deleted_at) VALUES ('01922d5e-9721-77f0-8093-55f799339495', 'testI2', 'laudem', '12', '01922d5e-9721-77f0-8093-55f799339495', 'DELETED', 'PROF','2024-09-26T10:07:06.7605+02:00', NULL)",
)
