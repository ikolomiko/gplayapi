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

import com.aurora.gplayapi.*
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.network.IHttpClient

class LibraryHelper(authData: AuthData) : BaseHelper(authData) {

    override fun using(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    fun getWishlistApps(): List<App> {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val params: Map<String, String> = mutableMapOf(
            "c" to "0",
            "dt" to "7",
            "libid" to "u-wl"
        )

        val playResponse = httpClient.get(GooglePlayApi.URL_LIBRARY, headers, params)

        val appList: MutableList<App> = mutableListOf()
        val listResponse: ListResponse = getListResponseFromBytes(playResponse.responseBytes)
        if (listResponse.itemCount > 0) {
            for (item in listResponse.itemList) {
                for (subItem in item.subItemList) {
                    if (item.subItemCount > 0) {
                        appList.addAll(getAppsFromItem(subItem))
                    }
                }
            }
        }

        return appList
    }

    fun wishlist(packageName: String, isAddRequest: Boolean = true): Boolean {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val builder = ModifyLibraryRequest.newBuilder()
            .setLibraryId("u-wl")

        if (isAddRequest)
            builder.addAddPackageName(packageName)
        else
            builder.addRemovePackageName(packageName)

        val playResponse = httpClient.post(GooglePlayApi.URL_MODIFY_LIBRARY, headers, builder.build().toByteArray())
        return playResponse.isSuccessful
    }
}