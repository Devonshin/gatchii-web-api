-- V1__init.sql
-- Baseline migration: create tables if not exists with current schema (idempotent across H2/PostgreSQL)

-- LOGIN USERS
CREATE TABLE IF NOT EXISTS login_users
(
    id            UUID PRIMARY KEY,
    prefix_id     VARCHAR(50)              NOT NULL,
    suffix_id     VARCHAR(50)              NOT NULL,
    password      VARCHAR(255)             NOT NULL,
    rsa_uid       UUID                     NOT NULL,
    status        VARCHAR(10)              NOT NULL,
    "role"        VARCHAR(10)              NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at    TIMESTAMP WITH TIME ZONE NULL
);
CREATE INDEX IF NOT EXISTS login_users_user_uid_idx ON login_users (rsa_uid);
CREATE UNIQUE INDEX IF NOT EXISTS login_users_prefix_id_suffix_id_unique ON login_users (prefix_id, suffix_id);

-- RSA KEYS
CREATE TABLE IF NOT EXISTS rsa_keys
(
    id          UUID PRIMARY KEY,
    public_key  TEXT                     NOT NULL,
    private_key TEXT                     NOT NULL,
    exponent    TEXT                     NOT NULL,
    modulus     TEXT                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE NULL
);

-- JWKS
CREATE TABLE IF NOT EXISTS jwks
(
    id          UUID PRIMARY KEY,
    private_key VARCHAR(512)             NOT NULL,
    public_key  TEXT                     NOT NULL,
    status      VARCHAR(10),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE NULL
);

-- REFRESH TOKENS
CREATE TABLE IF NOT EXISTS jwt_refresh_tokens
(
    id         UUID PRIMARY KEY,
    is_valid   BOOLEAN                  NOT NULL,
    user_uid   UUID                     NOT NULL,
    expire_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);
CREATE INDEX IF NOT EXISTS jwt_refresh_tokens_user_uid_idx ON jwt_refresh_tokens (user_uid);
