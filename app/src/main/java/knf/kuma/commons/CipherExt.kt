package knf.kuma.commons

import android.util.Base64
import knf.kuma.BuildConfig
import java.security.InvalidAlgorithmParameterException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun String.encrypt(): String {
    return try {
        encrypt16(BuildConfig.CIPHER_PWD_16)
    } catch (e: InvalidAlgorithmParameterException) {
        encrypt12(BuildConfig.CIPHER_PWD_12)
    }
}

fun String.decrypt(): String {
    return try {
        decrypt16(BuildConfig.CIPHER_PWD_16)
    } catch (e: InvalidAlgorithmParameterException) {
        decrypt12(BuildConfig.CIPHER_PWD_12)
    }
}

fun String.encrypt12(password: String): String {
    val secretKeySpec = SecretKeySpec(password.toByteArray(), "AES")
    val iv = ByteArray(12)
    val charArray = password.toCharArray()
    for (i in charArray.indices) {
        iv[i] = charArray[i].toByte()
    }
    val gcmParameterSpec = GCMParameterSpec(128, iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec)

    val encryptedValue = cipher.doFinal(this.toByteArray())
    return Base64.encodeToString(encryptedValue, Base64.DEFAULT)
}

fun String.encrypt16(password: String): String {
    val secretKeySpec = SecretKeySpec(password.toByteArray(), "AES")
    val iv = ByteArray(16)
    val charArray = password.toCharArray()
    for (i in charArray.indices) {
        iv[i] = charArray[i].toByte()
    }
    val ivParameterSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

    val encryptedValue = cipher.doFinal(this.toByteArray())
    return Base64.encodeToString(encryptedValue, Base64.DEFAULT)
}

fun String.encrypt32(password: String): String {
    val secretKeySpec = SecretKeySpec(password.toByteArray(), "AES")
    val iv = ByteArray(12)
    val charArray = password.toCharArray()
    for (i in charArray.indices) {
        iv[i] = charArray[i].toByte()
    }
    val ivParameterSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

    val encryptedValue = cipher.doFinal(this.toByteArray())
    return Base64.encodeToString(encryptedValue, Base64.DEFAULT)
}

fun String.decrypt12(password: String): String {
    val secretKeySpec = SecretKeySpec(password.toByteArray(), "AES")
    val iv = ByteArray(12)
    val charArray = password.toCharArray()
    for (i in charArray.indices) {
        iv[i] = charArray[i].toByte()
    }
    val gcmParameterSpec = GCMParameterSpec(128, iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec)

    val decryptedByteValue = cipher.doFinal(Base64.decode(this, Base64.DEFAULT))
    return String(decryptedByteValue)
}

fun String.decrypt16(password: String): String {
    val secretKeySpec = SecretKeySpec(password.toByteArray(), "AES")
    val iv = ByteArray(16)
    val charArray = password.toCharArray()
    for (i in charArray.indices) {
        iv[i] = charArray[i].toByte()
    }
    val ivParameterSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

    val decryptedByteValue = cipher.doFinal(Base64.decode(this, Base64.DEFAULT))
    return String(decryptedByteValue)
}

fun String.decrypt32(password: String): String {
    val secretKeySpec = SecretKeySpec(password.toByteArray(), "AES")
    val iv = ByteArray(12)
    val charArray = password.toCharArray()
    for (i in charArray.indices) {
        iv[i] = charArray[i].toByte()
    }
    val ivParameterSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

    val decryptedByteValue = cipher.doFinal(Base64.decode(this, Base64.DEFAULT))
    return String(decryptedByteValue)
}