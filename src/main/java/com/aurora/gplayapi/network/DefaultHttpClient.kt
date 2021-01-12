/*
 *     GPlayApi
 *     Copyright (C) 2020  Aurora OSS
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
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import java.nio.charset.Charset

object DefaultHttpClient : IHttpClient {

    override fun get(url: String, headers: Map<String, String>): PlayResponse {
        return get(url, headers, hashMapOf())
    }

    override fun get(
            url: String,
            headers: Map<String, String>,
            params: Map<String, String>
    ): PlayResponse {
        val parameters = params
                .map { it.key to it.value }
                .toList()
        val (request, response, result) = Fuel.get(url, parameters)
                .header(headers)
                .response()
        return buildPlayResponse(response, request)
    }

    override fun get(
            url: String,
            headers: Map<String, String>,
            paramString: String
    ): PlayResponse {
        val (request, response, result) = Fuel.get(url + paramString)
                .header(headers)
                .response()
        return buildPlayResponse(response, request)
    }

    override fun getAuth(url: String): PlayResponse {
        return PlayResponse().apply {
            isSuccessful = false
            code = 444
        }
    }

    override fun postAuth(url: String, body: ByteArray): PlayResponse {
        return PlayResponse().apply {
            isSuccessful = false
            code = 444
        }
    }

    override fun post(url: String, headers: Map<String, String>, body: ByteArray): PlayResponse {
        val (request, response, result) = Fuel.post(url)
                .header(headers)
                .appendHeader(Headers.CONTENT_TYPE, "application/x-protobuf")
                .body(body, Charset.defaultCharset())
                .response()
        return buildPlayResponse(response, request)
    }

    override fun post(
            url: String,
            headers: Map<String, String>,
            params: Map<String, String>
    ): PlayResponse {
        val parameters = params
                .map { it.key to it.value }
                .toList()
        val (request, response, result) = Fuel.post(url, parameters)
                .header(headers)
                .response()
        return buildPlayResponse(response, request)
    }

    @JvmStatic
    private fun buildPlayResponse(response: Response, request: Request): PlayResponse {
        return PlayResponse().apply {

            if (response.isSuccessful) {
                responseBytes = response.body().toByteArray()
            }

            if (response.isClientError || response.isServerError) {
                errorBytes = response.responseMessage.toByteArray()
                errorString = String(errorBytes)
            }
            isSuccessful = response.isSuccessful
            code = response.statusCode
        }
    }
}