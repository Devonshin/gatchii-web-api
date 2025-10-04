package com.gatchii.serialization

import com.gatchii.domain.jwt.AccessToken
import com.gatchii.domain.jwt.JwtModel
import com.gatchii.plugins.ErrorResponse
import com.gatchii.plugins.JwtResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationConfigTest {

    private val json = Json // default config, matches application configureSerialization()

    @Test
    fun `JwtResponse should serialize and deserialize`() {
        // given
        val jwt = JwtModel(
            accessToken = AccessToken(token = "abc", expiresIn = 123L),
            refreshToken = com.gatchii.domain.jwt.RefreshToken(token = "def", expiresIn = 456L)
        )
        val dto = JwtResponse(message = "SUCCESS", code = 200, jwt = jwt)
        // when
        val encoded = json.encodeToString(dto)
        val decoded = json.decodeFromString<JwtResponse>(encoded)
        // then
        assertEquals(dto, decoded)
    }

    @Test
    fun `ErrorResponse should serialize and deserialize`() {
        // given
        val dto = ErrorResponse(message = "Bad Request", code = 400, path = "/login/attempt")
        // when
        val encoded = json.encodeToString(dto)
        val decoded = json.decodeFromString<ErrorResponse>(encoded)
        // then
        assertEquals(dto.message, decoded.message)
        assertEquals(dto.code, decoded.code)
        assertEquals(dto.path, decoded.path)
    }
}
