package com.gatchii.utils

import com.gatchii.config.GlobalConfig
import io.ktor.util.encodeBase64
import io.ktor.util.logging.KtorSimpleLogger
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.io.File
import kotlin.test.assertEquals

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 18/09/2024
 */

@UnitTest
class RsaPairGeneratorUnitTest {

    val logger = KtorSimpleLogger("RsaPairGeneratorUnitTest")


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

    /*@Test
    fun generateRsaPubPrvFile() {
        val secretPath = GlobalConfig.getConfigedValue("ktor.secret.path")
        val canonicalPath = File(".").canonicalPath

        logger.info("secretPath: $secretPath, canonicalPath = $canonicalPath")
        val rsaKeyPair = RsaPairHandler.generateRSAKeyPair()
        FileUtil.writeFile("$secretPath/public.pem", "-----BEGIN RSA PUBLIC KEY-----" + rsaKeyPair.public.encoded.encodeBase64() + "-----END RSA PUBLIC KEY-----")
        FileUtil.writeFile("$secretPath/private.pem", "-----BEGIN RSA PRIVATE KEY-----" + rsaKeyPair.private.encoded.encodeBase64() + "-----END RSA PRIVATE KEY-----")
    }*/

}