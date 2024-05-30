package ro.ase.ism.network

import android.os.Build
import android.util.Log
import com.androiddevs.mvvmnewsapp.api.RetrofitInstance
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


class KeyManager private constructor() {
    private var clientKeyPair: KeyPair? = null
    private lateinit var derivedKey: ByteArray

    init {
        generateClientKeyPair()
    }

    private fun generateClientKeyPair() {
        clientKeyPair = try {
            val keyPairGenerator = KeyPairGenerator.getInstance(ellipticCurve)
            val ecSpec = ECGenParameterSpec(ecStandardName)
            keyPairGenerator.initialize(ecSpec)
            keyPairGenerator.generateKeyPair()
        } catch (e: Exception) {
            Log.e(TAG, "Error while generating client key pair: " + e.message)
            throw RuntimeException("Error generating client key pair", e)
        }
    }

    fun decodePublicKey(publicKeyString: String?): PublicKey {
        return try {
            var publicKeyBytes: ByteArray? = ByteArray(0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                publicKeyBytes = Base64.getDecoder().decode(publicKeyString)
            }
            val keyFactory = KeyFactory.getInstance(ellipticCurve)
            keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding EC public key: " + e.message)
            throw RuntimeException("Error decoding EC public key", e)
        }
    }

    fun encodePublicKey(): String {
        val serverPublicKey = clientKeyPair!!.public
        var clientPublicKeyString = emptyString
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            clientPublicKeyString = Base64.getEncoder().encodeToString(serverPublicKey.encoded)
        }
        return clientPublicKeyString
    }

    // Perform Diffie-Hellman key exchange to calculate shared secret
    fun generateDerivedKey(serverPublicKeyString: String) {
        try {
            val sharedSecret = calculateSharedSecret(serverPublicKeyString)

            // Derive the key using the same method as on the server
            derivedKey = pbkdf2(sharedSecret, "mysalt", 100000, 32, SHA256)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i(TAG, "Derived key: " + Base64.getEncoder().encodeToString(derivedKey))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while generating derived key: " + e.message)
        }
    }

    private fun calculateSharedSecret(serverPublicKeyString: String): String {
        try {
            // Server's public key received
            val serverPublicKey = decodePublicKey(serverPublicKeyString)

            // Calculate the shared secret using a KeyAgreement
            val keyAgreement = KeyAgreement.getInstance(ellipticCurveDH)
            keyAgreement.init(clientKeyPair!!.private)
            keyAgreement.doPhase(serverPublicKey, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Base64.getEncoder().encodeToString(keyAgreement.generateSecret())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while calculating shared secret: " + e.message)
        }
        return emptyString
    }

    // PBKDF2 function
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun pbkdf2(
        secret: String,
        salt: String,
        iterations: Int,
        keyLength: Int,
        algorithm: String
    ): ByteArray {
        val keyFactory = SecretKeyFactory.getInstance(pbkdfAlgorithm + algorithm)
        val keySpec: KeySpec =
            PBEKeySpec(secret.toCharArray(), salt.toByteArray(), iterations, keyLength * 8)
        val secretKey = keyFactory.generateSecret(keySpec)
        return secretKey.encoded
    }

    fun encryptMessage(message: String): String {
        try {
            // Use a fixed Initialization Vector (IV) or generate one as needed
            val iv = generateRandomBytes() // Example: use 16 bytes for AES-256-CBC

            // Create a Cipher instance with AES algorithm and CBC mode
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

            // Create a SecretKey using the derivedKey
            val secretKey: SecretKey = SecretKeySpec(derivedKey, AES)

            // Initialize the Cipher with the SecretKey and IV for encryption
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            // Encrypt the message
            val encryptedBytes = cipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))

            // Combine IV and encrypted message
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            // Encode the combined byte array to Base64
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Base64.getEncoder().encodeToString(combined)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while encrypting the message: " + e.message)
            throw RuntimeException("Error while encrypting the message", e)
        }
        return emptyString
    }

    companion object {
        private val TAG = KeyManager::class.java.simpleName
        private var keyManagerInstance: KeyManager? = null
        private const val ellipticCurve = "EC"
        private const val ellipticCurveDH = "ECDH"
        private const val ecStandardName = "secp384r1"
        private const val emptyString = ""
        private const val SHA256 = "SHA256"
        private const val AES = "AES"
        private const val pbkdfAlgorithm = "PBKDF2WithHmac"
        val instance: KeyManager?
            get() {
                if (keyManagerInstance == null) {
                    synchronized(RetrofitInstance::class.java) {
                        if (keyManagerInstance == null) {
                            keyManagerInstance = KeyManager()
                        }
                    }
                }
                return keyManagerInstance
            }

        private fun generateRandomBytes(): ByteArray {
            val secureRandom = SecureRandom()
            val randomBytes = ByteArray(16)
            secureRandom.nextBytes(randomBytes)
            return randomBytes
        }
    }
}
