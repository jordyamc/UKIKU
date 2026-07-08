package knf.kuma.commons

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

object AllSSLOkHttpClient {
    fun get() = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionSpecs(
            listOf(
                ConnectionSpec.CLEARTEXT,
                ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                    .allEnabledTlsVersions()
                    .allEnabledCipherSuites()
                    .build()
            )
        ).build()

    fun enableTLS() {
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
            SSLContext.getInstance("TLSv1.3").apply {
                init(null, null, null)
                createSSLEngine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}