package com.gatchii.utils

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.math.ec.ECPoint
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*


/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 13/09/2024
 */
class ECKeyPairHandler {
    companion object {
        init {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
        const val ALGORITHM = "EC"
        const val SIGN_ALGORITHM = "SHA256withECDSA"
        const val PRAM_SPEC = "secp256r1"

        fun generateKeyPair(): KeyPair {
            val keyGen = KeyPairGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
            val ecGenParameterSpec = ECGenParameterSpec(PRAM_SPEC)
            keyGen.initialize(ecGenParameterSpec, SecureRandom())
            return keyGen.generateKeyPair()
        }

        fun sign(message: String, privateKey: PrivateKey): String {
            Signature.getInstance(SIGN_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME).apply {
                initSign(privateKey)
                update(message.toByteArray(StandardCharsets.UTF_8))
                return Base64.getUrlEncoder().encodeToString(sign())
            }
        }

        fun verify(message: String, publicKey: PublicKey, signature: String): Boolean {
            Signature.getInstance(SIGN_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME).apply {
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

       fun generatePublicKeyFromPrivateKey(privateKey: PrivateKey): PublicKey {
           val keyFactory: KeyFactory = KeyFactory.getInstance("EC") // KeyFactory 설정

           // BouncyCastle의 곡선 스펙 가져오기
           val ecSpec = ECNamedCurveTable.getParameterSpec(PRAM_SPEC)
           val privateKeyD = (privateKey as ECPrivateKey).s // PrivateKey의 Scalar 값
           val q: ECPoint = ecSpec.g.multiply(privateKeyD).normalize() // 좌표를 정규화

           // JDK가 이해할 수 있는 ECParameterSpec으로 변환
           val ecParameterSpec = ECNamedCurveSpec(
               PRAM_SPEC,
               ecSpec.curve,
               ecSpec.g,
               ecSpec.n,
               ecSpec.h,
               ecSpec.seed
           )

           // 정규화된 좌표(q)를 사용해 PublicKey 생성
           val publicPoint = java.security.spec.ECPoint(q.affineXCoord.toBigInteger(), q.affineYCoord.toBigInteger())
           val pubSpec = ECPublicKeySpec(publicPoint, ecParameterSpec)
           return keyFactory.generatePublic(pubSpec)
       }

        fun convertPublicKey(pem: String): PublicKey {
            val keyFactory = KeyFactory.getInstance("EC")
            val privateKeySpec = X509EncodedKeySpec(Base64.getDecoder().decode(pem))
            val publicKey = keyFactory.generatePublic(privateKeySpec)
            return publicKey
        }

    }

}