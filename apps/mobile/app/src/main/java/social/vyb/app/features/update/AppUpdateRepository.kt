package social.vyb.app.features.update

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import social.vyb.app.BuildConfig
import java.util.concurrent.TimeUnit

class AppUpdateRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trim().let { if (it.endsWith("/")) it else "$it/" })
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
                    }
                )
                .build()
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AppUpdateApi::class.java)

    suspend fun check(): AndroidUpdateManifest = api.androidUpdate(
        versionCode = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME
    )
}
