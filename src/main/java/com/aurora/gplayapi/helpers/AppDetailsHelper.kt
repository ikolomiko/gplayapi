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
import com.aurora.gplayapi.ListResponse
import com.aurora.gplayapi.data.builders.AppBuilder
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.exceptions.ApiException
import com.aurora.gplayapi.network.IHttpClient
import java.util.*

class AppDetailsHelper(authData: AuthData) : BaseHelper(authData) {

    override fun using(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    @Throws(Exception::class)
    fun getAppByPackageName(packageName: String): App {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val params: MutableMap<String, String> = HashMap()
        params["doc"] = packageName

        val playResponse = httpClient.get(GooglePlayApi.URL_DETAILS, headers, params)

        if (playResponse.isSuccessful) {
            val detailsResponse = getDetailsResponseFromBytes(playResponse.responseBytes)
            return AppBuilder.build(detailsResponse)
        } else {
            throw ApiException.AppNotFound(playResponse.errorString)
        }
    }

    @Throws(Exception::class)
    fun getAppByPackageName(packageList: List<String>): List<App> {
        val appList: MutableList<App> = ArrayList()
        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val request = getBulkDetailsBytes(packageList)

        if (!headers.containsKey("Content-Type")) {
            headers["Content-Type"] = "application/x-protobuf"
        }

        val playResponse = httpClient.post(GooglePlayApi.URL_BULK_DETAILS, headers, request)

        if (playResponse.isSuccessful) {
            val payload = getPayLoadFromBytes(playResponse.responseBytes)
            if (payload.hasBulkDetailsResponse()) {
                val bulkDetailsResponse = payload.bulkDetailsResponse
                for (entry in bulkDetailsResponse.entryList) {
                    val app = AppBuilder.build(entry.item)
                    //System.out.printf("%s -> %s\n", app.displayName, app.packageName);
                    appList.add(app)
                }
            }
            return appList
        } else
            throw ApiException.Server(playResponse.code, playResponse.errorString)
    }

    fun getDetailsStream(streamUrl: String): Map<String, List<App>> {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val params: MutableMap<String, String> = HashMap()

        val playResponse = httpClient.get(
            "${GooglePlayApi.URL_FDFE}/${streamUrl}",
            headers,
            params
        )

        val appListMap: MutableMap<String, List<App>> = mutableMapOf()

        if (playResponse.isSuccessful) {
            val payload = getPayLoadFromBytes(playResponse.responseBytes)
            val listResponse: ListResponse = payload.listResponse
            for (item in listResponse.itemList) {
                for (subItem in item.subItemList) {
                    appListMap[subItem.title] = getAppsFromItem(subItem)
                }
            }
        }

        return appListMap
    }
}