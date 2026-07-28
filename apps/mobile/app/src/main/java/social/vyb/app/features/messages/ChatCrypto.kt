package social.vyb.app.features.messages

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object ChatCrypto {
    const val IDENTITY_ALGORITHM = "ECDH-P256"
    const val MESSAGE_ALGORITHM = "ECDH-P256/AES-GCM"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS_PREFIX = "vyb_chat_p256_"
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    data class LocalIdentity(val publicKey: String, val privateKey: PrivateKey)
    data class EncryptedMessage(val cipherText: String, val cipherIv: String)

    fun findLocalIdentity(userId: String): LocalIdentity? {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val alias = alias(userId)
        val privateKey = store.getKey(alias, null) as? PrivateKey ?: return null
        val publicKey = store.getCertificate(alias)?.publicKey as? ECPublicKey ?: return null
        return LocalIdentity(encodeRawPublicKey(publicKey), privateKey)
    }
    fun getOrCreateLocalIdentity(userId: String): LocalIdentity {
        findLocalIdentity(userId)?.let { return it }
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        generator.initialize(
            KeyGenParameterSpec.Builder(
                alias(userId),
                KeyProperties.PURPOSE_AGREE_KEY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        val pair = generator.generateKeyPair()
        return LocalIdentity(
            publicKey = encodeRawPublicKey(pair.public as ECPublicKey),
            privateKey = pair.private
        )
    }

    fun encrypt(
        plaintext: String,
        identity: LocalIdentity,
        peerPublicKey: String
    ): EncryptedMessage {
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipherBytes = cipher(Cipher.ENCRYPT_MODE, identity.privateKey, peerPublicKey, iv)
            .doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val encodedIv = encode(iv)
        val envelope = ChatCipherEnvelope(
            cipherText = encode(cipherBytes),
            iv = encodedIv,
            senderPublicKey = identity.publicKey,
            recipientPublicKey = peerPublicKey
        )
        return EncryptedMessage(json.encodeToString(ChatCipherEnvelope.serializer(), envelope), encodedIv)
    }

    fun decrypt(
        message: ChatMessageDto,
        identity: LocalIdentity,
        fallbackPeerPublicKey: String
    ): String {
        if (message.cipherAlgorithm != MESSAGE_ALGORITHM) return message.cipherText
        val envelope = runCatching {
            json.decodeFromString(ChatCipherEnvelope.serializer(), message.cipherText)
        }.getOrNull()
        val peerPublicKey = when {
            envelope?.senderPublicKey == identity.publicKey -> envelope.recipientPublicKey
            envelope?.recipientPublicKey == identity.publicKey -> envelope.senderPublicKey
            else -> fallbackPeerPublicKey
        }
        val iv = decode(envelope?.iv ?: message.cipherIv)
        val cipherBytes = decode(envelope?.cipherText ?: message.cipherText)
        return cipher(Cipher.DECRYPT_MODE, identity.privateKey, peerPublicKey, iv)
            .doFinal(cipherBytes)
            .toString(StandardCharsets.UTF_8)
    }

    private fun cipher(
        mode: Int,
        privateKey: PrivateKey,
        peerPublicKey: String,
        iv: ByteArray
    ): Cipher {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(decodeRawPublicKey(peerPublicKey), true)
        val aesKey = SecretKeySpec(agreement.generateSecret(), "AES")
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, aesKey, GCMParameterSpec(128, iv))
        }
    }

    private fun encodeRawPublicKey(publicKey: ECPublicKey): String {
        val x = unsigned32(publicKey.w.affineX)
        val y = unsigned32(publicKey.w.affineY)
        return encode(byteArrayOf(4) + x + y)
    }

    private fun decodeRawPublicKey(value: String): ECPublicKey {
        val raw = decode(value)
        require(raw.size == 65 && raw[0] == 4.toByte()) { "Peer chat key is invalid." }
        val params = AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec("secp256r1"))
        }.getParameterSpec(ECParameterSpec::class.java)
        val point = ECPoint(
            BigInteger(1, raw.copyOfRange(1, 33)),
            BigInteger(1, raw.copyOfRange(33, 65))
        )
        return KeyFactory.getInstance("EC")
            .generatePublic(ECPublicKeySpec(point, params)) as ECPublicKey
    }

    private fun unsigned32(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        val withoutSign = if (raw.size == 33 && raw[0] == 0.toByte()) raw.copyOfRange(1, 33) else raw
        return ByteArray(32).also { output ->
            withoutSign.copyInto(output, 32 - withoutSign.size.coerceAtMost(32), (withoutSign.size - 32).coerceAtLeast(0))
        }
    }

    private fun alias(userId: String) = ALIAS_PREFIX + userId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.DEFAULT)
}

internal suspend fun FirebaseUser.chatIdToken(): String =
    suspendCancellableCoroutine { continuation ->
        getIdToken(false)
            .addOnSuccessListener { result ->
                result.token?.let(continuation::resume)
                    ?: continuation.resumeWithException(IllegalStateException("Firebase returned an empty ID token."))
            }
            .addOnFailureListener(continuation::resumeWithException)
    }
