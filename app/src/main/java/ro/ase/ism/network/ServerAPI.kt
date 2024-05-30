package ro.ase.ism.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ServerAPI {
    @GET("/api/getPublicKey")
    suspend fun getPublicKey(): Response<String>

    @POST("/api/postPublicKey")
    suspend fun postPublicKey(@Body publicKey: String): Response<Void>

    @POST("/api/postMessage")
    suspend fun postMessage(@Body message: String): Response<Void>
}