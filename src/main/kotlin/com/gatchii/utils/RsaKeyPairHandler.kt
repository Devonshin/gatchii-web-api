package com.gatchii.utils

import com.gatchii.config.GlobalConfig
import com.nimbusds.jose.jwk.RSAKey
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher


/**
 * Package: com.gatchii.web
 * Created: Devonshin
 * Date: 13/09/2024
 */
data class PrivateKeyData(
    val kid: String,
    val privateKey: String,
    val createdAt: LocalDateTime
)

data class PublicKeyData(
    val kid: String,
    val publicKey: String,
    val n: String,
    val e: String,
    val createdAt: LocalDateTime
)

data class RsaKeyDataPair(
    val privateKey: PrivateKeyData,
    val publicKey: PublicKeyData
)

class RsaPairHandler {

    companion object {

        private const val ALGORITHM = "RSA"
        private const val TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val KEY_SIZE = 2048
        lateinit var rsaPublicKey: PublicKey
        lateinit var rsaPrivateKey: PrivateKey
        init {
            loadMainRsaPair()
        }
        private fun loadMainRsaPair() {
            val secretPath = GlobalConfig.getConfigedValue("ktor.secret.path")
            val rsaPrivateKeyStr = GlobalConfig.getConfigedValue("ktor.secret.privateKey")
            val rsaPublicKeyStr = GlobalConfig.getConfigedValue("ktor.secret.publicKey")
            rsaPrivateKey = if (rsaPrivateKeyStr.isNotBlank()) {
                strToPrivateKey(rsaPrivateKeyStr)
            } else {
                strToPrivateKey(FileUtil.readFile(secretPath + "/private.pem")!!)
            }

            rsaPublicKey = (if (rsaPublicKeyStr.isNotBlank()) {
                strToPublicKey(rsaPublicKeyStr)
            } else {
                strToPublicKey(FileUtil.readFile(secretPath + "/public.pem")!!)
            })
        }

        fun encrypt(textToEncrypt: String): String {
            return encrypt(textToEncrypt, rsaPublicKey)
        }

        fun decrypt(encryptedText: String): String {
            return decrypt(encryptedText, rsaPrivateKey)
        }

        fun encrypt(textToEncrypt: String, publicKeyStr: String): String {
            return encrypt(textToEncrypt, strToPublicKey(publicKeyStr))
        }

        fun encrypt(textToEncrypt: String, publicKey: PublicKey): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = cipher.doFinal(textToEncrypt.toByteArray(StandardCharsets.UTF_8))
            return encodeToStr(encryptedBytes)
        }

        fun decrypt(encryptedText: String, privateKeyStr: String): String {
            val privateKey = strToPrivateKey(privateKeyStr)
            return decrypt(encryptedText, privateKey)
        }

        fun decrypt(encryptedText: String, privateKey: PrivateKey): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val decryptedBytes = cipher.doFinal(decodeFromStr(encryptedText))
            return String(decryptedBytes)
        }

        @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class)
        fun strToPublicKey(publicKeyStr: String): PublicKey {
            val publicKeyStr = publicKeyStr.replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("\\s", "")
                .replace("\n", "")

            val keyBytes: ByteArray = decodeFromStr(publicKeyStr)
            val spec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            return keyFactory.generatePublic(spec)
        }

        fun strToPrivateKey(privateKeyStr: String): PrivateKey {
            val privateKeyStr = privateKeyStr.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("\\s", "")
                .replace("\n", "")

            val kf = KeyFactory.getInstance(ALGORITHM)
            val keySpecPKCS8 = PKCS8EncodedKeySpec(decodeFromStr(privateKeyStr))
            val privateKey = kf.generatePrivate(keySpecPKCS8)
            return privateKey
        }

        fun generateRSAKeyPair(): KeyPair {
            val keyGen = KeyPairGenerator.getInstance(ALGORITHM)
            keyGen.initialize(KEY_SIZE)
            return keyGen.generateKeyPair()
        }

        fun generateRsaDataPair(): RsaKeyDataPair {
            val uuid = UUID.randomUUID().toString()
            // RSA 키 페어 생성
            val keyPair: KeyPair = generateRSAKeyPair()
            // 비밀키 및 공개키 가져오기
            val privateKey: PrivateKey = keyPair.private
            val publicKey: RSAPublicKey = keyPair.public as RSAPublicKey
            // 비밀키를 PEM 형식으로 출력
            val privatePem = encodeToStr(privateKey.encoded)
            // 공개키를 PEM 형식으로 출력
            val publicPem = encodeToStr(publicKey.encoded)
            // 공개키를 JWK 형식으로 변환
            val jwk = RSAKey.Builder(publicKey).build().toJSONObject()
            val now = LocalDateTime.now()
            return RsaKeyDataPair(
                PrivateKeyData(uuid, privatePem, now),
                PublicKeyData(
                    uuid,
                    publicPem,
                    jwk["n"].toString(),
                    jwk["e"].toString(),
                    now
                )
            )
        }

        private fun encodeToStr(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
        private fun decodeFromStr(data: String): ByteArray = Base64.getDecoder().decode(data)

    }

    //    @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class, IOException::class, URISyntaxException::class)
    //    fun main() {
    //        var privateKeyContent = String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("private_key_pkcs8.pem").toURI())))
    //        var publicKeyContent = String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("public_key.pem").toURI())))
    //
    //        privateKeyContent = privateKeyContent.replace("\\n".toRegex(), "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
    //        publicKeyContent = publicKeyContent.replace("\\n".toRegex(), "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
    //
    //        val kf = KeyFactory.getInstance("RSA")
    //
    //        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent))
    //        val privKey = kf.generatePrivate(keySpecPKCS8)
    //
    //        val keySpecX509 = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent))
    //        val pubKey = kf.generatePublic(keySpecX509) as RSAPublicKey
    //
    //        println(privKey)
    //        println(pubKey)
    //    }

}