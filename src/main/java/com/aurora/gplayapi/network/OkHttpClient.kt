/*
 *     GPlayApi
 *     Copyright (C) 2023  Aurora OSS
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 */

package com.aurora.gplayapi.network

import com.aurora.gplayapi.data.models.PlayResponse
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit

object OkHttpClient : IHttpClient {

    private const val POST = "POST"
    private const val GET = "GET"

    private var okHttpClient =
        OkHttpClient()
            .newBuilder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    override fun setProxy(proxy: Proxy, proxyUser: String?, proxyPassword: String?) {
        var clientBuilder =
            OkHttpClient()
                .newBuilder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .proxy(proxy)

        if (proxyUser != null) {
            val proxyAuthenticator =
                object : Authenticator {
                    @Throws(IOException::class)
                    override fun authenticate(route: Route?, response: Response): Request? {
                        val credential = Credentials.basic(proxyUser, proxyPassword.orEmpty())
                        return response.request
                            .newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build()
                    }
                }

            clientBuilder = clientBuilder.proxyAuthenticator(proxyAuthenticator)
        }

        okHttpClient = clientBuilder.build()
    }

    @Throws(IOException::class)
    fun post(url: String, headers: Map<String, String>, requestBody: RequestBody): PlayResponse {
        val request =
            Request.Builder()
                .url(url)
                .headers(headers.toHeaders())
                .method(POST, requestBody)
                .build()
        return processRequest(request)
    }

    @Throws(IOException::class)
    override fun post(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>,
    ): PlayResponse {
        val request =
            Request.Builder()
                .url(buildUrl(url, params))
                .headers(headers.toHeaders())
                .method(POST, "".toRequestBody(null))
                .build()
        return processRequest(request)
    }

    override fun postAuth(url: String, body: ByteArray): PlayResponse {
        return PlayResponse().apply {
            isSuccessful = false
            code = 444
        }
    }

    @Throws(IOException::class)
    override fun post(url: String, headers: Map<String, String>, body: ByteArray): PlayResponse {
        val requestBody = body.toRequestBody("application/x-protobuf".toMediaType(), 0, body.size)
        return post(url, headers, requestBody)
    }

    @Throws(IOException::class)
    override fun get(url: String, headers: Map<String, String>): PlayResponse {
        return get(url, headers, mapOf())
    }

    @Throws(IOException::class)
    override fun get(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>,
    ): PlayResponse {
        val request =
            Request.Builder()
                .url(buildUrl(url, params))
                .headers(headers.toHeaders())
                .method(GET, null)
                .build()
        return processRequest(request)
    }

    override fun getAuth(url: String): PlayResponse {
        return PlayResponse().apply {
            isSuccessful = false
            code = 444
        }
    }

    @Throws(IOException::class)
    override fun get(url: String, headers: Map<String, String>, paramString: String): PlayResponse {
        val request =
            Request.Builder()
                .url(url + paramString)
                .headers(headers.toHeaders())
                .method(GET, null)
                .build()
        return processRequest(request)
    }

    private fun processRequest(request: Request): PlayResponse {
        val call = okHttpClient.newCall(request)
        return buildPlayResponse(call.execute())
    }

    private fun buildUrl(url: String, params: Map<String, String>): HttpUrl {
        val urlBuilder = url.toHttpUrl().newBuilder()
        params.forEach { urlBuilder.addQueryParameter(it.key, it.value) }
        return urlBuilder.build()
    }

    private fun buildPlayResponse(response: Response): PlayResponse {
        return PlayResponse().apply {
            isSuccessful = response.isSuccessful
            code = response.code

            if (response.body != null) {
                responseBytes = response.body!!.bytes()
            }

            if (!isSuccessful) {
                errorString = response.message
            }
        }
    }
}
