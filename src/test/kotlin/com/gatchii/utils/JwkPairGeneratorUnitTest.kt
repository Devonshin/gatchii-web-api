package com.gatchii.utils

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import kotlin.test.assertEquals

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 18/09/2024
 */

@UnitTest
class JwkPairGeneratorUnitTest {

    companion object {
        private lateinit var generateRsaPair: RsaKeyDataPair
        private lateinit var publicKey: PublicKeyData
        private lateinit var privateKey: PrivateKeyData
        private lateinit var textEncrypted: String
        private val text = "test encrypt string"
        @BeforeAll
        @JvmStatic
        fun setUp() {
            generateRsaPair = RsaPairHandler.generateRsaDataPair()
            publicKey = generateRsaPair.publicKey
            privateKey = generateRsaPair.privateKey
            textEncrypted = RsaPairHandler.encrypt(text, publicKey.publicKey)
        }
    }

    @Test
    fun `encrypt decrypt test`() {
        //given
        //when
        val decrypt = RsaPairHandler.decrypt(textEncrypted, privateKey.privateKey)
        //then
        println("decrypt: $decrypt")
        println("text: $text")
        assert(text == decrypt)
    }

    @Test
    fun generateRsaPair() {
        //given
        val rsaKeyPair = RsaPairHandler.generateRsaDataPair()
        val publicKey = rsaKeyPair.publicKey
        val privateKey = rsaKeyPair.privateKey
        val textPlain = "test plain string"
        //when
        val encrypt = RsaPairHandler.encrypt(textPlain, publicKey.publicKey)
        val decrypt = RsaPairHandler.decrypt(encrypt, privateKey.privateKey)
        //then
        assertEquals(textPlain, decrypt)
    }
}