package knf.kuma.commons

import knf.kuma.custom.TlsOnlySocketFactory
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.io.File
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object NoSSLOkHttpClient {
    private val appCache by lazy { Cache(File(safeContext.cacheDir, "okhttpcache"), 10 * 1024 * 1024) }
    fun get(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                    noCrash(false) {
                        chain.forEach {
                            it.checkValidity()
                        }
                    }
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                    noCrash(false) {
                        chain.forEach {
                            it.checkValidity()
                        }
                    }
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })
            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, java.security.SecureRandom())
            }
            val sslSocketFactory = if (isMIUI) {
                TlsOnlySocketFactory(sslContext.socketFactory)
            } else {
                sslContext.socketFactory
            }
            val builder = OkHttpClient.Builder().apply {
                connectTimeout(PrefsUtil.timeoutTime, TimeUnit.SECONDS)
                readTimeout(PrefsUtil.timeoutTime, TimeUnit.SECONDS)
                sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                cache(appCache)
                hostnameVerifier { _, _ -> /*isHostValid(hostName)*/ true }
                connectionSpecs(
                    listOf(
                        ConnectionSpec.CLEARTEXT,
                        ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                            .allEnabledTlsVersions()
                            .allEnabledCipherSuites()
                            .build()
                    )
                )
            }
            /*val dns = DnsOverHttps.Builder().client(builder).apply {
                url("https://dns.google/dns-query".toHttpUrl())
                bootstrapDnsHosts(InetAddress.getByName("8.8.4.4"), InetAddress.getByName("8.8.8.8"))
            }.build()
            return builder.newBuilder().dns(dns).build()*/
            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }
}
