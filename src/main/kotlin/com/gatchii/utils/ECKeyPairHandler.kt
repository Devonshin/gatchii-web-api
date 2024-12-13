package com.gatchii.utils

import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*


/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 13/09/2024
 */
class ECKeyPairHandler {

    init {
        Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
    }

    companion object {
        const val KEY_ALGORITHM = "ES256"
        private const val ALGORITHM = "EC"
        private const val SIGN_ALGORITHM = "SHA256withECDSA"
        private const val PRAM_SPEC = "secp256r1"

        fun generateKeyPair(): KeyPair {
            val keyGen = KeyPairGenerator.getInstance(ALGORITHM)
            val ecGenParameterSpec = ECGenParameterSpec(PRAM_SPEC)
            keyGen.initialize(ecGenParameterSpec, SecureRandom())
            return keyGen.generateKeyPair()
        }

        fun sign(message: String, privateKey: PrivateKey): String {
            Signature.getInstance(SIGN_ALGORITHM).apply {
                initSign(privateKey)
                update(message.toByteArray(StandardCharsets.UTF_8))
                return Base64.getUrlEncoder().encodeToString(sign())
            }
        }

        fun verify(message: String, publicKey: PublicKey, signature: String): Boolean {
            Signature.getInstance(SIGN_ALGORITHM).apply {
                initVerify(publicKey)
                update(message.toByteArray(StandardCharsets.UTF_8))
                return verify(Base64.getUrlDecoder().decode(signature))
            }
        }

        fun convertPrivateKey(pem: String): PrivateKey {
            val keyFactory = KeyFactory.getInstance("EC")
            val privateKeySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem))
            val privateKey = keyFactory.generatePrivate(privateKeySpec)
            return privateKey
        }

        fun convertPublicKey(pem: String): PublicKey {
            val keyFactory = KeyFactory.getInstance("EC")
            val privateKeySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem))
            val publicKey = keyFactory.generatePublic(privateKeySpec)
            return publicKey
        }

    }

}