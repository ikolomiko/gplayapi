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

import com.aurora.gplayapi.GooglePlayApi
import com.aurora.gplayapi.UserProfileResponse
import com.aurora.gplayapi.data.builders.UserProfileBuilder
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.UserProfile
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.network.IHttpClient

class UserProfileHelper(authData: AuthData) : BaseHelper(authData) {

    override fun using(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    @Throws(Exception::class)
    fun getUserProfileResponse(): UserProfileResponse {
        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val playResponse = httpClient.get(GooglePlayApi.URL_USER_PROFILE, headers, mapOf())
        if (playResponse.isSuccessful) {
            return getUserProfileResponse(playResponse.responseBytes).userProfileResponse
        } else {
            throw Exception("Failed to fetch user profile")
        }
    }

    @Throws(Exception::class)
    fun getUserProfile(): UserProfile? {
        return try {
            UserProfileBuilder.build(
                getUserProfileResponse().userProfile
            )
        } catch (e: Exception) {
            null
        }
    }
}