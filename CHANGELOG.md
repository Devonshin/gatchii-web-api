# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.1.0/), and this project adheres to Semantic Versioning (https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2025-09-14

Inferred SemVer bump: minor (new features and user‑visible changes since initial commit).

### Added
- RSA: Add automatic default RSA key generation and wire configurations ([4ff2159](https://github.com/Devonshin/gatchii-web-api/commit/4ff2159)).
- JWK: Add JWK route and public JWKS endpoint ([264ffa7](https://github.com/Devonshin/gatchii-web-api/commit/264ffa7)).
- JWT/JWK: Introduce base JWK domain and improve access/refresh token creation logic ([aeb1e39](https://github.com/Devonshin/gatchii-web-api/commit/aeb1e39)).

### Changed
- JWK: Switch key algorithm from secp256k1 to secp256r1 (P-256); update tokens and routes accordingly ([5a603d3](https://github.com/Devonshin/gatchii-web-api/commit/5a603d3)).
- Config/JWK: Introduce GlobalConfig and tidy JWK implementations/tests ([8701677](https://github.com/Devonshin/gatchii-web-api/commit/8701677)).
- Modules: Split JWK/util and remove internal utils in favor of external dependency ([fdcbd72](https://github.com/Devonshin/gatchii-web-api/commit/fdcbd72)).
- Project: Package reorganization (domains → domain), docs moved, and test paths cleaned ([aff13a7](https://github.com/Devonshin/gatchii-web-api/commit/aff13a7)).

### Fixed
- Router: Handle favicon.ico requests to avoid 404 noise ([06e35c1](https://github.com/Devonshin/gatchii-web-api/commit/06e35c1)).

### Build
- Temporarily lower Java version for broader compatibility ([39fe803](https://github.com/Devonshin/gatchii-web-api/commit/39fe803)).

### Chore
- Update .gitignore ([6e96969](https://github.com/Devonshin/gatchii-web-api/commit/6e96969)).

### Documentation
- Add initial README ([f63c6dd](https://github.com/Devonshin/gatchii-web-api/commit/f63c6dd)).

