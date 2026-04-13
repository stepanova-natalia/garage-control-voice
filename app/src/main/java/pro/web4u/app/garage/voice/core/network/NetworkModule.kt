package pro.web4u.app.garage.voice.core.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import pro.web4u.app.garage.voice.BuildConfig
import pro.web4u.app.garage.voice.R
import pro.web4u.app.garage.voice.core.auth.AuthInterceptor
import pro.web4u.app.garage.voice.core.auth.TokenAuthenticator
import javax.inject.Singleton
import okhttp3.logging.HttpLoggingInterceptor
import pro.web4u.app.garage.voice.core.storage.ServerSettings
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ) : OkHttpClient {
        val host = ServerSettings.getServerUrl(context)
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')

        val certificatePinner = CertificatePinner.Builder()
            .add(host, "sha256/${BuildConfig.CERTIFICATE_FINGERPRINT}")
            .build()

        val trustedCertificate = context.resources.openRawResource(R.raw.caddy_root).use { stream ->
            val cf = CertificateFactory.getInstance("X.509")
            cf.generateCertificates(stream).first() as X509Certificate
        }

        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                chain?.forEach { cert ->
                    if (cert.issuerX500Principal == trustedCertificate.subjectX500Principal) return
                }
                throw CertificateException("Certificate not trusted")
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf(trustedCertificate)
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .certificatePinner(certificatePinner)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ) : Retrofit {
        return Retrofit.Builder()
            .baseUrl(ServerSettings.getServerUrl(context))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideGarageApi(retrofit: Retrofit): GarageApi = retrofit.create(GarageApi::class.java)
}