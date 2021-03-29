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

package com.aurora.gplayapi.helpers

import com.aurora.gplayapi.DeviceManager
import com.aurora.gplayapi.GooglePlayApi
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.DeviceInfoProvider
import com.aurora.gplayapi.network.DefaultHttpClient
import com.aurora.gplayapi.network.IHttpClient
import java.util.*

class AuthHelper private constructor() {

    companion object {

        var httpClient: IHttpClient = DefaultHttpClient

        fun using(httpClient: IHttpClient) = apply {
            this.httpClient = httpClient
        }

        fun build(email: String, aasToken: String): AuthData {
            val properties = DeviceManager.loadProperties("px_3a.properties")
            if (properties != null)
                return build(email, aasToken, properties)
            else
                throw Exception("Unable to read device config")
        }

        fun build(email: String, aasToken: String, deviceName: String): AuthData {
            val properties = DeviceManager.loadProperties(deviceName)
            if (properties != null)
                return build(email, aasToken, properties)
            else
                throw Exception("Unable to read device config")
        }

        fun build(email: String, aasToken: String, properties: Properties): AuthData {
            val deviceInfoProvider = DeviceInfoProvider(properties, Locale.getDefault().toString())

            val authData = AuthData(email, aasToken)
            authData.deviceInfoProvider = deviceInfoProvider
            authData.locale = Locale.getDefault()

            val api = GooglePlayApi(authData).via(httpClient)
            val gsfId = api.generateGsfId(deviceInfoProvider)
            authData.gsfId = gsfId

            val deviceConfigResponse = api.uploadDeviceConfig(deviceInfoProvider)
            authData.deviceConfigToken = deviceConfigResponse.uploadDeviceConfigToken

            val ac2dm = api.generateToken(aasToken, GooglePlayApi.Service.AC2DM)
            authData.ac2dmToken = ac2dm

            val gcmToken = api.generateToken(aasToken, GooglePlayApi.Service.GCM)
            authData.gcmToken = gcmToken

            val token = api.generateToken(aasToken, GooglePlayApi.Service.GOOGLE_PLAY)
            authData.authToken = token

            val tosResponse = api.toc()

            //Fetch UserProfile
            authData.userProfile = UserProfileHelper(authData).getUserProfile()

            return authData
        }

        fun buildInsecure(
            email: String,
            authToken: String,
            locale: Locale,
            deviceInfoProvider: DeviceInfoProvider
        ): AuthData {

            val authData = AuthData(email, authToken, true)

            authData.deviceInfoProvider = deviceInfoProvider
            authData.locale = locale

            val api = GooglePlayApi(authData)

            //Android GSF ID
            val gsfId = api.generateGsfId(deviceInfoProvider)
            authData.gsfId = gsfId

            //Upload Device Config
            val deviceConfigResponse = api.uploadDeviceConfig(deviceInfoProvider)
            authData.deviceConfigToken = deviceConfigResponse.uploadDeviceConfigToken

            //GooglePlay TOS
            val tosResponse = api.toc()

            //Fetch UserProfile
            authData.userProfile = UserProfileHelper(authData).getUserProfile()

            return authData
        }
    }
}