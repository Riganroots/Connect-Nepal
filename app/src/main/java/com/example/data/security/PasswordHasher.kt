package com.example.data.security

import android.os.Build
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 password hashing for locally stored accounts.
 *
 * Stored format: `pbkdf2-<digest>$<iterations>$<base64 salt>$<base64 hash>`.
 * PBKDF2WithHmacSHA256 is only available from API 26, so older devices fall back to SHA-1.
 */
object PasswordHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private const val SHA256 = "sha256"
    private const val SHA1 = "sha1"

    fun hash(password: String): String {
        val digest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) SHA256 else SHA1
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt, ITERATIONS, digest)
        return listOf("pbkdf2-$digest", ITERATIONS.toString(), encode(salt), encode(hash)).joinToString("$")
    }

    /** Returns false for blank or malformed stored hashes, so such accounts cannot be logged into. */
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4 || !parts[0].startsWith("pbkdf2-")) return false
        return try {
            val digest = parts[0].removePrefix("pbkdf2-")
            val iterations = parts[1].toInt()
            val salt = decode(parts[2])
            val expected = decode(parts[3])
            val actual = derive(password, salt, iterations, digest)
            MessageDigest.isEqual(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int, digest: String): ByteArray {
        val algorithm = when (digest) {
            SHA256 -> "PBKDF2WithHmacSHA256"
            SHA1 -> "PBKDF2WithHmacSHA1"
            else -> throw IllegalArgumentException("Unsupported digest: $digest")
        }
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}
