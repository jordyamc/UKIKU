package knf.kuma.iap

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import knf.kuma.BuildConfig
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec


object Security {
    private val TAG = "IABUtil/Security"

    private val KEY_FACTORY_ALGORITHM = "RSA"
    private val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Verifies that the data was signed with the given signature, and returns
     * the verified purchase. The data is in JSON format and signed
     * with a private key. The data also contains the [PurchaseState]
     * and product ID of the purchase.
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     */

    fun verifyPurchase(signedData: String, signature: String): Boolean {
        try {
            if (TextUtils.isEmpty(signedData) ||
                    TextUtils.isEmpty(signature)) {
                Log.e(TAG, "Purchase verification failed: missing data.")
                return false
            }

            val key = generatePublicKey()
            return verify(key, signedData, signature)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Generates a PublicKey instance from a string containing the
     * Base64-encoded public key.
     *
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    private fun generatePublicKey(): PublicKey {
        try {
            val decodedKey = Base64.decode(BuildConfig.IAB_KEY, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            Log.e(TAG, "Invalid key specification.")
            throw IllegalArgumentException(e)
        }

    }

    /**
     * Verifies that the signature from the server matches the computed
     * signature on the data.  Returns true if the data is correctly signed.
     *
     * @param publicKey public key associated with the developer account
     * @param signedData signed data from server
     * @param signature server signature
     * @return true if the data and signature match
     */
    fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes: ByteArray
        try {
            signatureBytes = Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decoding failed.")
            return false
        }

        try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray())
            if (!sig.verify(signatureBytes)) {
                Log.e(TAG, "Signature verification failed.")
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "NoSuchAlgorithmException.")
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Invalid key specification.")
        } catch (e: SignatureException) {
            Log.e(TAG, "Signature exception.")
        }

        return false
    }
}