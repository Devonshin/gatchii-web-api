package com.gatchii.domain.jwt

/**
 * Package: com.gatchii.domains.jwt
 * Created: Devonshin
 * Date: 25/12/2024
 */

sealed class ValidationResult {
    object Success : ValidationResult()
    object Expired : ValidationResult()
    data class InvalidClaim(val invalidField: String, val message: String) : ValidationResult()
    object MissingClaim : ValidationResult()
}