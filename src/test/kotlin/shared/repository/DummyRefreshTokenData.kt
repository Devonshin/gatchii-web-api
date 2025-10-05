package shared.repository

val dummyRefreshTokenQueryList = listOf(
  "INSERT INTO jwt_refresh_tokens (expire_at, is_valid, id, user_uid, created_at) VALUES ('2024-11-17T17:39:49.709344+01:00', TRUE, '019335d8-52d6-7d40-b434-af29f956faec', '019345d8-52d6-7d40-b434-af29f956faec', '2024-11-16T17:39:49.718633+01:00')",
  "INSERT INTO jwt_refresh_tokens (expire_at, is_valid, id, user_uid, created_at) VALUES ('2024-11-17T17:39:49.709344+01:00', TRUE, '019335d9-52d6-7d40-b434-af29f956faec', '019355d9-52d6-7d40-b434-af29f956faec', '2024-11-16T17:39:49.718633+01:00')",
)
