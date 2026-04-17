package com.syncbridge.demo.data.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID
import javax.inject.Inject

class MockServerInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        Thread.sleep(LATENCY_MS)

        val (code, message, body) = when {
            path.endsWith("force-conflict") -> Triple(
                409,
                "Conflict",
                """{"error":"stock_exhausted","message":"Producto sin stock disponible"}"""
            )
            else -> Triple(
                200,
                "OK",
                """{"id":"${UUID.randomUUID()}","status":"created","message":"Pedido recibido"}"""
            )
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    companion object {
        private const val LATENCY_MS = 1500L
    }
}
