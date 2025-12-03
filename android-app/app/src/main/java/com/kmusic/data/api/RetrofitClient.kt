package com.kmusic.data.api

import android.content.Context
import com.google.gson.GsonBuilder
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.local.UserSessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var sessionManager: UserSessionManager? = null
    private var serverConfigManager: ServerConfigManager? = null

    fun initialize(context: Context) {
        sessionManager = UserSessionManager(context)
        serverConfigManager = ServerConfigManager(context)
    }

    private fun getRetrofit(): Retrofit {
        if (retrofit == null || serverConfigManager == null) {
            throw IllegalStateException("RetrofitClient not initialized. Call initialize() first.")
        }

        val baseUrl = serverConfigManager!!.getBaseUrl()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor {
            sessionManager?.getToken()
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit!!
    }

    fun recreateRetrofit() {
        retrofit = null
    }

    val api: MusicApiService
        get() = getRetrofit().create(MusicApiService::class.java)
}
