package com.gatchii.utils

import io.ktor.util.*
import org.assertj.core.api.Assertions
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.security.KeyPair
import java.security.PrivateKey
import java.security.Security

/**
 * Package: com.gatchii.unit.utils
 * Created: Devonshin
 * Date: 03/11/2024
 */

@UnitTest
class ECKeyPairHandlerTest {
    companion object {
        init {
            // Add Bouncy Castle Provider for tests
            Security.addProvider(BouncyCastleProvider())
        }
    }

    @Test
    fun `ECDSA enerateKeyPair test`() {
        val keyPair: KeyPair = ECKeyPairHandler.generateKeyPair()
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
    }

    @Test
    fun `ECDSA signAndVerify test`() {
        val message = "This is a test message"
        val keyPair: KeyPair = ECKeyPairHandler.generateKeyPair()

        val signature = ECKeyPairHandler.sign(message, keyPair.private)
        assertNotNull(signature)

        val isVerified = ECKeyPairHandler.verify(message, keyPair.public, signature)
        assertTrue(isVerified, "Signature should be verified.")
    }

    @Test
    fun `ECDSA InvalidSignature test`() {
        val message = "This is a test message"
        val otherMessage = "This is a different message"
        val keyPair: KeyPair = ECKeyPairHandler.generateKeyPair()

        val signature = ECKeyPairHandler.sign(message, keyPair.private)

        val isVerified = ECKeyPairHandler.verify(otherMessage, keyPair.public, signature)
        assertFalse(isVerified, "Signature should not be verified.")
    }

    @Test
    fun `test if convertPrivateKey generates PrivateKey from PEM format`() {
        // given
        val keyPair: KeyPair = ECKeyPairHandler.generateKeyPair()
        val privateKeyPem = keyPair.private.encoded.encodeBase64()

        // when
        val privateKey: PrivateKey = ECKeyPairHandler.convertPrivateKey(privateKeyPem)

        //then
        Assertions.assertThat(privateKey)
            .isEqualTo(keyPair.private)
            .withFailMessage("PrivateKey should not be null for valid PEM format")
    }

    @Test
    fun `test if convertPrivateKey throws exception for invalid PEM format`() {
        // given
        val invalidPemString = "This is an invalid PEM format"
        // when - then
        assertThrows(IllegalArgumentException::class.java) {
            ECKeyPairHandler.convertPrivateKey(invalidPemString)
        }
    }

}