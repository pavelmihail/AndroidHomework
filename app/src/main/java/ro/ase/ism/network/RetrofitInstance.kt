package com.androiddevs.mvvmnewsapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ro.ase.ism.network.ServerAPI

class RetrofitInstance {
    companion object {

        private val retrofit by lazy {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
            Retrofit.Builder()
                .baseUrl("https://10.0.2.2:443/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

        val api: ServerAPI by lazy {
            retrofit.create(ServerAPI::class.java)
        }
    }
}