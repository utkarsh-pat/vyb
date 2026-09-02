package social.vyb.app.features.messages

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
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
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.PBEKeySpec

internal object ChatCrypto {
    const val IDENTITY_ALGORITHM = "ECDH-P256"
    const val MESSAGE_ALGORITHM = "ECDH-P256/AES-GCM"
    const val ATTACHMENT_ALGORITHM = "ECDH-P256/AES-GCM/attachment-v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS_PREFIX = "vyb_chat_p256_"
    private const val WRAP_ALIAS_PREFIX = "vyb_chat_wrap_"
    private const val LEGACY_PREFERENCES = "vyb_chat_identity_v1"
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    data class LocalIdentity(val publicKey: String, val privateKey: PrivateKey)
    data class EncryptedMessage(val cipherText: String, val cipherIv: String)
    data class EncryptedAttachment(
        val bytes: ByteArray,
        val cipherIv: String,
        val senderPublicKey: String,
        val recipientPublicKey: String
    )

    fun findLocalIdentity(context: Context, userId: String): LocalIdentity? {
        findWrappedIdentity(context, userId)?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null
        }
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val alias = alias(userId)
        val privateKey = store.getKey(alias, null) as? PrivateKey ?: return null
        val publicKey = store.getCertificate(alias)?.publicKey as? ECPublicKey ?: return null
        return LocalIdentity(encodeRawPublicKey(publicKey), privateKey)
    }

    fun getOrCreateLocalIdentity(context: Context, userId: String): LocalIdentity {
        findLocalIdentity(context, userId)?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return createWrappedLegacyIdentity(context, userId)
        }
        return createHardwareIdentity(userId)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createHardwareIdentity(userId: String): LocalIdentity {
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

    /**
     * Android Keystore only supports EC key-agreement keys from API 31. On
     * API 26-30 we therefore generate the ECDH key in the platform provider
     * and persist it only as an AES-GCM ciphertext whose non-exportable AES
     * wrapping key lives in Android Keystore.
     */
    private fun createWrappedLegacyIdentity(context: Context, userId: String): LocalIdentity {
        val pair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        }.generateKeyPair()
        val identity = LocalIdentity(
            publicKey = encodeRawPublicKey(pair.public as ECPublicKey),
            privateKey = pair.private
        )
        persistWrappedIdentity(context, userId, identity)
        return identity
    }

    fun restoreIdentity(
        context: Context,
        userId: String,
        backup: ChatKeyBackupDto,
        secret: String
    ): LocalIdentity {
        require(backup.wrappingAlgorithm == "PBKDF2-SHA-256/AES-GCM") {
            "This secure-chat backup uses an unsupported format."
        }
        val normalizedPin = secret.filter(Char::isDigit).take(6)
        val normalizedPhrase = secret.lowercase().trim()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+")).filter(String::isNotBlank).joinToString(" ")
        val isPin = secret.trim().all(Char::isDigit) && normalizedPin.length == 6
        val isPhrase = normalizedPhrase.split(' ').filter(String::isNotBlank).size == 24

        val (cipherText, iv, key) = when {
            backup.version >= 2 && isPin -> Triple(
                backup.pinWrappedPrivateKey ?: backup.wrappedPrivateKey,
                backup.pinIv ?: backup.iv,
                deriveBackupKey(
                    normalizedPin,
                    String(decode(backup.pinSalt ?: backup.salt), StandardCharsets.UTF_8),
                    backup.pinIterations ?: BACKUP_ITERATIONS
                )
            )
            backup.version >= 2 && isPhrase &&
                backup.recoveryWrappedPrivateKey != null &&
                backup.recoverySalt != null && backup.recoveryIv != null -> Triple(
                    backup.recoveryWrappedPrivateKey,
                    backup.recoveryIv,
                    deriveBackupKey(
                        normalizedPhrase,
                        decode(backup.recoverySalt),
                        backup.recoveryIterations ?: BACKUP_ITERATIONS
                    )
                )
            backup.version < 2 && secret.trim().length >= 16 -> Triple(
                backup.wrappedPrivateKey,
                backup.iv,
                deriveBackupKey(secret.trim(), decode(backup.salt), backup.iterations)
            )
            else -> error("Enter your 6-digit security PIN or full 24-word recovery phrase.")
        }
        val plaintext = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, decode(iv)))
            doFinal(decode(cipherText))
        }
        val restored = json.decodeFromString(
            BackupIdentity.serializer(),
            plaintext.toString(StandardCharsets.UTF_8)
        )
        require(restored.userId == userId && restored.publicKey == backup.publicKey) {
            "This backup belongs to a different secure-chat identity."
        }
        val identity = LocalIdentity(
            publicKey = restored.publicKey,
            privateKey = KeyFactory.getInstance("EC")
                .generatePrivate(PKCS8EncodedKeySpec(decode(restored.privateKey)))
        )
        persistWrappedIdentity(context, userId, identity)
        return identity
    }

    private fun persistWrappedIdentity(context: Context, userId: String, identity: LocalIdentity) {
        val plaintext = json.encodeToString(
            WrappedIdentity.serializer(),
            WrappedIdentity(
                publicKey = identity.publicKey,
                privateKey = encode(identity.privateKey.encoded)
            )
        ).toByteArray(StandardCharsets.UTF_8)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val encrypted = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, legacyWrappingKey(userId), GCMParameterSpec(128, iv))
            doFinal(plaintext)
        }
        context.applicationContext
            .getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putString(preferenceKey(userId), "${encode(iv)}.${encode(encrypted)}")
            }
    }

    private fun findWrappedIdentity(context: Context, userId: String): LocalIdentity? =
        runCatching {
            val encodedIdentity = context.applicationContext
                .getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
                .getString(preferenceKey(userId), null)
                ?: return null
            val parts = encodedIdentity.split('.', limit = 2)
            require(parts.size == 2) { "Stored chat identity is invalid." }
            val plaintext = Cipher.getInstance("AES/GCM/NoPadding").run {
                init(
                    Cipher.DECRYPT_MODE,
                    legacyWrappingKey(userId, createIfMissing = false),
                    GCMParameterSpec(128, decode(parts[0]))
                )
                doFinal(decode(parts[1]))
            }
            val stored = json.decodeFromString(
                WrappedIdentity.serializer(),
                plaintext.toString(StandardCharsets.UTF_8)
            )
            LocalIdentity(
                publicKey = stored.publicKey,
                privateKey = KeyFactory.getInstance("EC")
                    .generatePrivate(PKCS8EncodedKeySpec(decode(stored.privateKey)))
            )
        }.getOrNull()

    private fun deriveBackupKey(secret: String, salt: String, iterations: Int): SecretKey =
        deriveBackupKey(secret, salt.toByteArray(StandardCharsets.UTF_8), iterations)

    private fun deriveBackupKey(secret: String, salt: ByteArray, iterations: Int): SecretKey {
        require(iterations in 100_000..1_000_000) { "Secure-chat backup work factor is invalid." }
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(secret.toCharArray(), salt, iterations, 256))
            .encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun legacyWrappingKey(
        userId: String,
        createIfMissing: Boolean = true
    ): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val keyAlias = wrapAlias(userId)
        (store.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let {
            return it
        }
        check(createIfMissing) { "Chat identity wrapping key is missing." }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            generateKey()
        }
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

    fun encryptAttachment(
        plaintext: ByteArray,
        identity: LocalIdentity,
        peerPublicKey: String
    ): EncryptedAttachment {
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        return EncryptedAttachment(
            bytes = cipher(Cipher.ENCRYPT_MODE, identity.privateKey, peerPublicKey, iv).doFinal(plaintext),
            cipherIv = encode(iv),
            senderPublicKey = identity.publicKey,
            recipientPublicKey = peerPublicKey
        )
    }

    fun decryptAttachment(
        cipherBytes: ByteArray,
        attachment: ChatAttachmentDto,
        identity: LocalIdentity,
        fallbackPeerPublicKey: String
    ): ByteArray {
        require(attachment.cipherAlgorithm == ATTACHMENT_ALGORITHM && !attachment.cipherIv.isNullOrBlank()) {
            "This chat attachment is missing its encryption envelope."
        }
        val peerPublicKey = when {
            attachment.senderPublicKey == identity.publicKey -> attachment.recipientPublicKey
            attachment.recipientPublicKey == identity.publicKey -> attachment.senderPublicKey
            else -> fallbackPeerPublicKey
        } ?: error("This chat attachment cannot be opened without the peer public key.")
        return cipher(
            Cipher.DECRYPT_MODE,
            identity.privateKey,
            peerPublicKey,
            decode(requireNotNull(attachment.cipherIv))
        ).doFinal(cipherBytes)
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
    private fun wrapAlias(userId: String) =
        WRAP_ALIAS_PREFIX + userId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    private fun preferenceKey(userId: String) =
        userId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.DEFAULT)

    private const val BACKUP_ITERATIONS = 250_000

    @Serializable
    private data class WrappedIdentity(
        val publicKey: String,
        val privateKey: String
    )

    @Serializable
    private data class BackupIdentity(
        val userId: String,
        val identityId: String? = null,
        val publicKey: String,
        val privateKey: String,
        val algorithm: String = IDENTITY_ALGORITHM,
        val keyVersion: Int = 1,
        val updatedAt: String = ""
    )
}
