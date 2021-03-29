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

package com.aurora.gplayapi

import com.aurora.gplayapi.GooglePlayApi.Service.*
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.DeviceInfoProvider
import com.aurora.gplayapi.data.providers.HeaderProvider.getAuthHeaders
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.data.providers.ParamProvider.getAASTokenParams
import com.aurora.gplayapi.data.providers.ParamProvider.getAuthParams
import com.aurora.gplayapi.data.providers.ParamProvider.getDefaultAuthParams
import com.aurora.gplayapi.exceptions.AuthException
import com.aurora.gplayapi.network.DefaultHttpClient
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.gplayapi.utils.Util
import java.io.IOException
import java.math.BigInteger
import java.util.*

class GooglePlayApi(private val authData: AuthData) {

    private var httpClient: IHttpClient = DefaultHttpClient

    fun via(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    @Throws(IOException::class)
    fun toc(): TocResponse {
        val playResponse = httpClient.get(URL_TOC, getDefaultHeaders(authData))
        val tocResponse = ResponseWrapper.parseFrom(playResponse.responseBytes).payload.tocResponse
        if (tocResponse.tosContent.isNotBlank() && tocResponse.tosToken.isNotBlank()) {
            acceptTos(tocResponse.tosToken)
        }
        if (tocResponse.cookie.isNotBlank()) {
            authData.dfeCookie = tocResponse.cookie
        }
        return tocResponse
    }

    @Throws(IOException::class)
    private fun acceptTos(tosToken: String): AcceptTosResponse {
        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val params: MutableMap<String, String> = HashMap()
        params["tost"] = tosToken
        params["toscme"] = "false"

        val playResponse = httpClient.post(URL_TOS_ACCEPT, headers, params)
        return ResponseWrapper.parseFrom(playResponse.responseBytes)
            .payload
            .acceptTosResponse
    }

    @Throws(IOException::class)
    fun uploadDeviceConfig(deviceInfoProvider: DeviceInfoProvider): UploadDeviceConfigResponse {
        val request = UploadDeviceConfigRequest.newBuilder()
            .setDeviceConfiguration(deviceInfoProvider.deviceConfigurationProto)
            .build()

        val headers: MutableMap<String, String> = getDefaultHeaders(authData)

        val playResponse = httpClient.post(URL_UPLOAD_DEVICE_CONFIG, headers, request.toByteArray())

        val configResponse = ResponseWrapper.parseFrom(playResponse.responseBytes)
            .payload
            .uploadDeviceConfigResponse

        if (configResponse.uploadDeviceConfigToken.isNotBlank()) {
            authData.deviceConfigToken = configResponse.uploadDeviceConfigToken
        }

        return configResponse
    }

    @Throws(IOException::class)
    fun generateGsfId(deviceInfoProvider: DeviceInfoProvider): String {
        val request = deviceInfoProvider.generateAndroidCheckInRequest()
        val checkInResponse = checkIn(request!!.toByteArray())
        val gsfId = BigInteger.valueOf(checkInResponse.androidId).toString(16)
        authData.gsfId = gsfId
        authData.deviceCheckInConsistencyToken = checkInResponse.deviceCheckinConsistencyToken
        return gsfId
    }

    @Throws(IOException::class)
    private fun checkIn(request: ByteArray): AndroidCheckinResponse {
        val headers: MutableMap<String, String> = getAuthHeaders(authData)
        headers["Content-Type"] = "application/x-protobuffer"
        headers["Host"] = "android.clients.google.com"
        val responseBody = httpClient.post(URL_CHECK_IN, headers, request)
        return AndroidCheckinResponse.parseFrom(responseBody.responseBytes)
    }

    @Throws(IOException::class)
    fun generateAASToken(oauthToken: String): String? {
        val params: MutableMap<String, String> = HashMap()
        params.putAll(getDefaultAuthParams(authData))
        params.putAll(getAASTokenParams(oauthToken))
        val headers: MutableMap<String, String> = getAuthHeaders(authData)
        headers["app"] = "com.android.vending"
        val playResponse = httpClient.post(URL_AUTH, headers, params)
        val hashMap = Util.parseResponse(playResponse.responseBytes)
        return if (hashMap.containsKey("Token")) {
            hashMap["Token"]
        } else {
            throw AuthException("Authentication failed : Could not generate AAS Token")
        }
    }

    @Throws(IOException::class)
    fun generateToken(aasToken: String, service: Service): String {
        val headers: MutableMap<String, String> = getAuthHeaders(authData)
        val params: MutableMap<String, String> = HashMap()
        params.putAll(getDefaultAuthParams(authData))
        params.putAll(getAuthParams(aasToken))

        when (service) {
            AC2DM -> {
                params["service"] = "ac2dm"
                params.remove("app")
            }
            ANDROID_CHECK_IN_SERVER -> {
                params["oauth2_foreground"] = "0"
                params["app"] = "com.google.android.gms"
                params["service"] = "AndroidCheckInServer"
            }
            EXPERIMENTAL_CONFIG -> {
                params["service"] = "oauth2:https://www.googleapis.com/auth/experimentsandconfigs"
            }
            NUMBERER -> {
                params["app"] = "com.google.android.gms"
                params["service"] = "oauth2:https://www.googleapis.com/auth/numberer"
            }
            GCM -> {
                params["app"] = "com.google.android.gms"
                params["service"] = "oauth2:https://www.googleapis.com/auth/gcm"
            }
            GOOGLE_PLAY -> {
                headers["app"] = "com.google.android.gms"
                params["service"] = "oauth2:https://www.googleapis.com/auth/googleplay"
            }
            OAUTHLOGIN -> {
                params["oauth2_foreground"] = "0"
                params["app"] = "com.google.android.googlequicksearchbox"
                params["service"] = "oauth2:https://www.google.com/accounts/OAuthLogin"
                params["callerPkg"] = "com.google.android.googlequicksearchbox"
            }
            ANDROID -> {
                params["service"] = "android"
            }
        }

        val playResponse = httpClient.post(URL_AUTH, headers, params)
        val hashMap = Util.parseResponse(playResponse.responseBytes)

        return if (hashMap.containsKey("Auth")) {
            hashMap.getOrDefault("Auth", "")
        } else {
            throw AuthException("Authentication failed : Could not generate OAuth Token")
        }
    }

    enum class Service {
        AC2DM,
        ANDROID,
        ANDROID_CHECK_IN_SERVER,
        EXPERIMENTAL_CONFIG,
        GCM,
        GOOGLE_PLAY,
        NUMBERER,
        OAUTHLOGIN
    }

    companion object {
        const val URL_BASE = "https://android.clients.google.com"
        const val URL_FDFE = "$URL_BASE/fdfe"
        const val CATEGORIES_URL = "$URL_FDFE/categoriesList"
        const val CATEGORIES_URL_2 = "$URL_FDFE/allCategoriesList"
        const val DELIVERY_URL = "$URL_FDFE/delivery"
        const val PURCHASE_URL = "$URL_FDFE/purchase"
        const val PURCHASE_HISTORY_URL = "$URL_FDFE/purchaseHistory"
        const val TOP_CHART_URL = "$URL_FDFE/listTopChartItems"
        const val URL_AUTH = "$URL_BASE/auth"
        const val URL_BULK_DETAILS = "$URL_FDFE/bulkDetails"
        const val URL_BULK_PREFETCH = "$URL_FDFE/bulkPrefetch"
        const val URL_CHECK_IN = "$URL_BASE/checkin"
        const val URL_DETAILS = "$URL_FDFE/details"
        const val URL_DETAILS_DEVELOPER = "$URL_FDFE/browseDeveloperPage"
        const val URL_MY_APPS = "$URL_FDFE/myApps"
        const val URL_REVIEW_ADD_EDIT = "$URL_FDFE/addReview"
        const val URL_REVIEW_USER = "$URL_FDFE/userReview"
        const val URL_REVIEWS = "$URL_FDFE/rev"
        const val URL_SEARCH = "$URL_FDFE/search"
        const val URL_SEARCH_SUGGEST = "$URL_FDFE/searchSuggest"
        const val URL_TESTING_PROGRAM = "$URL_FDFE/apps/testingProgram"
        const val URL_TOC = "$URL_FDFE/toc"
        const val URL_TOS_ACCEPT = "$URL_FDFE/acceptTos"
        const val URL_UPLOAD_DEVICE_CONFIG = "$URL_FDFE/uploadDeviceConfig"
        const val URL_SYNC = "$URL_FDFE/apps/contentSync"
        const val URL_SELF_UPDATE = "$URL_FDFE/selfUpdate"
        const val URL_USER_PROFILE = "$URL_FDFE/api/userProfile"
        const val URL_LIBRARY = "$URL_FDFE/library"
        const val URL_MODIFY_LIBRARY = "$URL_FDFE/modifyLibrary"

        //Not part of Google's API
        const val SALES_URL = "https://www.bestappsale.com/api/android/getsale.php"
    }
}