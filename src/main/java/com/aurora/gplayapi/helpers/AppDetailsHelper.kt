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
import com.aurora.gplayapi.Payload
import com.aurora.gplayapi.TestingProgramRequest
import com.aurora.gplayapi.data.builders.AppBuilder
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.gplayapi.data.models.details.TestingProgramStatus
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.exceptions.ApiException
import com.aurora.gplayapi.network.IHttpClient
import java.io.IOException
import java.util.*


class AppDetailsHelper(authData: AuthData) : BaseHelper(authData) {

    override fun using(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    private fun getAppListMapFromPayload(payload: Payload): Map<String, List<App>> {
        val appListMap: MutableMap<String, List<App>> = mutableMapOf()
        val listResponse: ListResponse = payload.listResponse
        for (item in listResponse.itemList) {
            for (subItem in item.subItemList) {
                if (subItem.categoryId == 3) {
                    appListMap[subItem.title] = getAppsFromItem(subItem)
                }
            }
        }
        return appListMap
    }

    private fun getDevStream(payload: Payload): DevStream {
        val devStream = DevStream()
        val listResponse: ListResponse = payload.listResponse
        for (item in listResponse.itemList) {
            for (subItem in item.subItemList) {
                if (subItem.categoryId != 3) {
                    if (subItem.hasAnnotations() && subItem.annotations.hasOverlayMetaData()) {
                        if (subItem.annotations.overlayMetaData.hasOverlayTitle()) {
                            devStream.title = subItem.annotations.overlayMetaData.overlayTitle.title
                            devStream.imgUrl = subItem.annotations.overlayMetaData.overlayTitle.compositeImage.url
                        }
                        if (subItem.annotations.overlayMetaData.hasOverlayDescription()) {
                            devStream.description = subItem.annotations.overlayMetaData.overlayDescription.description
                        }
                    }
                }
            }
        }
        return devStream
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

    fun getDetailsStream(streamUrl: String): StreamBundle {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val params: MutableMap<String, String> = HashMap()

        val playResponse = httpClient.get(
            "${GooglePlayApi.URL_FDFE}/${streamUrl}",
            headers,
            params
        )

        if (playResponse.isSuccessful) {
            val payload = getPayLoadFromBytes(playResponse.responseBytes)
            return getStreamBundle(payload.listResponse)
        }

        return StreamBundle()
    }

    fun getDeveloperStream(devId: String): DevStream {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val params: MutableMap<String, String> = HashMap()

        val playResponse = httpClient.get(
            "${GooglePlayApi.URL_FDFE}/getDeveloperPageStream?docid=developer-$devId",
            headers,
            params
        )

        var devStream = DevStream()

        if (playResponse.isSuccessful) {
            val payload = getPayLoadFromBytes(playResponse.responseBytes)
            devStream = getDevStream(payload)
            devStream.streamBundle = getStreamBundle(payload.listResponse)
        }

        return devStream
    }

    @Throws(IOException::class)
    fun testingProgram(packageName: String?, subscribe: Boolean = true): TestingProgramStatus {
        val request = TestingProgramRequest.newBuilder()
            .setPackageName(packageName)
            .setSubscribe(subscribe)
            .build()

        val playResponse = httpClient.post(
            GooglePlayApi.URL_TESTING_PROGRAM,
            getDefaultHeaders(authData),
            request.toByteArray()
        )

        return if (playResponse.isSuccessful) {
            val payload = getPayLoadFromBytes(playResponse.responseBytes)
            payload.hasTestingProgramResponse()
            TestingProgramStatus().apply {
                if (payload.hasTestingProgramResponse()
                    && payload.testingProgramResponse.hasResult()
                    && payload.testingProgramResponse.result.hasDetails()
                ) {
                    val details = payload.testingProgramResponse.result.details
                    if (details.hasSubscribed())
                        subscribed = details.subscribed
                    if (details.hasUnsubscribed())
                        unsubscribed = details.unsubscribed
                }
            }
        } else {
            TestingProgramStatus()
        }
    }
}