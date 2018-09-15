package knf.kuma.commons

import android.annotation.SuppressLint
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

@SuppressLint("TrustAllX509TrustManager")
object SSLSkipper {
    fun skip() {
        val trustAllCertificates = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null // Not relevant.
            }

            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
                // Do nothing. Just allow them all.
            }

            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
                // Do nothing. Just allow them all.
            }
        })

        val trustAllHostnames = HostnameVerifier { _, _ -> true }

        try {
            System.setProperty("jsse.enableSNIExtension", "false")
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCertificates, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(trustAllHostnames)
        } catch (e: GeneralSecurityException) {
            throw ExceptionInInitializerError(e)
        }

        try {
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf<X509TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls(0)
                }
            }), SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
        } catch (e: Exception) {

        }

    }
}
