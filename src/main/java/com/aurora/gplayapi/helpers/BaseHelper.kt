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
import com.aurora.gplayapi.data.builders.AppBuilder.build
import com.aurora.gplayapi.data.models.*
import com.aurora.gplayapi.data.models.editor.EditorChoiceBundle
import com.aurora.gplayapi.data.models.editor.EditorChoiceCluster
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.network.DefaultHttpClient
import com.aurora.gplayapi.network.IHttpClient
import java.io.IOException
import java.util.*

abstract class BaseHelper(protected var authData: AuthData) {

    var httpClient: IHttpClient = DefaultHttpClient

    abstract fun using(httpClient: IHttpClient): BaseHelper

    @Throws(IOException::class)
    fun getResponse(url: String, params: Map<String, String>, headers: Map<String, String>): PlayResponse {
        return httpClient.get(url, headers, params)
    }

    /*-------------------------------------------- COMMONS -------------------------------------------------*/
    fun getNextPageUrl(item: Item): String {
        return if (item.hasContainerMetadata() && item.containerMetadata.hasNextPageUrl()) item.containerMetadata.nextPageUrl else String()
    }

    fun getBrowseUrl(item: Item): String {
        return if (item.hasContainerMetadata() && item.containerMetadata.hasBrowseUrl()) item.containerMetadata.browseUrl else String()
    }

    @Throws(Exception::class)
    fun getPayLoadFromBytes(bytes: ByteArray?): Payload {
        val responseWrapper = ResponseWrapper.parseFrom(bytes)
        return responseWrapper!!.payload
    }

    @Throws(Exception::class)
    fun getUserProfileResponse(bytes: ByteArray?): PayloadApi {
        val responseWrapper = ResponseWrapperApi.parseFrom(bytes)
        return responseWrapper!!.payload
    }

    @Throws(Exception::class)
    fun getDetailsResponseFromBytes(bytes: ByteArray?): DetailsResponse {
        val payload = getPayLoadFromBytes(bytes)
        return payload.detailsResponse
    }

    @Throws(Exception::class)
    fun getListResponseFromBytes(bytes: ByteArray?): ListResponse {
        val payload = getPayLoadFromBytes(bytes)
        return payload.listResponse
    }

    @Throws(Exception::class)
    fun getBrowseResponseFromBytes(bytes: ByteArray?): BrowseResponse {
        val payload = getPayLoadFromBytes(bytes)
        return payload.browseResponse
    }

    @Throws(Exception::class)
    fun getPrefetchPayLoad(bytes: ByteArray?): Payload {
        val responseWrapper = ResponseWrapper.parseFrom(bytes)
        val payload = responseWrapper.payload
        return if (responseWrapper.preFetchCount > 0 && ((payload.hasSearchResponse()
                    && payload.searchResponse.itemCount == 0)
                    || payload.hasListResponse() && payload.listResponse.itemCount == 0
                    || payload.hasBrowseResponse())
        ) {
            responseWrapper.getPreFetch(0).response.payload
        } else payload
    }

    open fun getAppsFromItem(item: Item): MutableList<App> {
        val appList: MutableList<App> = mutableListOf()
        if (item.subItemCount > 0) {
            for (subItem in item.subItemList) {
                if (subItem.type == 1) {
                    val app = build(subItem)
                    appList.add(app)
                    //System.out.printf("%s -> %s\n", app.displayName, app.packageName);
                }
            }
        }
        return appList
    }

    fun getBulkDetailsBytes(packageList: List<String?>?): ByteArray {
        val bulkDetailsRequestBuilder = BulkDetailsRequest.newBuilder()
        bulkDetailsRequestBuilder.addAllDocId(packageList)
        return bulkDetailsRequestBuilder.build().toByteArray()
    }

    /*-------------------------------------- APP SEARCH & SUGGESTIONS ---------------------------------------*/
    @Throws(Exception::class)
    fun getSearchResponseFromBytes(bytes: ByteArray?): SearchResponse? {
        val payload = getPayLoadFromBytes(bytes)
        return if (payload.hasSearchResponse()) {
            payload.searchResponse
        } else null
    }

    @Throws(Exception::class)
    fun getSearchSuggestResponseFromBytes(bytes: ByteArray?): SearchSuggestResponse? {
        val payload = getPayLoadFromBytes(bytes)
        return if (payload.hasSearchSuggestResponse()) {
            payload.searchSuggestResponse
        } else null
    }

    /*--------------------------------------- GENERIC APP STREAMS --------------------------------------------*/
    @Throws(Exception::class)
    fun getNextStreamResponse(nextPageUrl: String): ListResponse {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val playResponse = httpClient.get(GooglePlayApi.URL_FDFE + "/" + nextPageUrl, headers)
        return if (playResponse.isSuccessful)
            getListResponseFromBytes(playResponse.responseBytes)
        else
            ListResponse.getDefaultInstance()
    }

    @Throws(Exception::class)
    fun getBrowseStreamResponse(browseUrl: String): BrowseResponse {
        val headers: Map<String, String> = getDefaultHeaders(authData)
        val playResponse = httpClient.get(GooglePlayApi.URL_FDFE + "/" + browseUrl, headers)
        return if (playResponse.isSuccessful)
            getBrowseResponseFromBytes(playResponse.responseBytes)
        else
            BrowseResponse.getDefaultInstance()
    }

    @Throws(Exception::class)
    fun getNextStreamCluster(nextPageUrl: String): StreamCluster {
        val listResponse = getNextStreamResponse(nextPageUrl)
        return getStreamCluster(listResponse)
    }

    open fun getStreamCluster(item: Item): StreamCluster {
        val title = if (item.hasTitle()) item.title else String()
        val subtitle = if (item.hasSubtitle()) item.subtitle else String()
        val browseUrl = getBrowseUrl(item)

        return StreamCluster().apply {
            id = browseUrl.hashCode()
            clusterTitle = title
            clusterSubtitle = subtitle
            clusterBrowseUrl = browseUrl
            clusterNextPageUrl = getNextPageUrl(item)
            clusterAppList = getAppsFromItem(item)
        }
    }

    fun getStreamCluster(payload: Payload): StreamCluster {
        return if (payload.hasListResponse())
            getStreamCluster(payload.listResponse)
        else StreamCluster()
    }

    fun getStreamCluster(listResponse: ListResponse): StreamCluster {
        if (listResponse.itemCount > 0) {
            val item = listResponse.getItem(0)
            if (item != null && item.subItemCount > 0) {
                val subItem = item.getSubItem(0)
                return getStreamCluster(subItem)
            }
        }
        return StreamCluster()
    }

    fun getStreamClusters(listResponse: ListResponse): List<StreamCluster> {
        val streamClusters: MutableList<StreamCluster> = ArrayList()
        if (listResponse.itemCount > 0) {
            val item = listResponse.getItem(0)
            if (item != null && item.subItemCount > 0) {
                for (subItem in item.subItemList) {
                    streamClusters.add(getStreamCluster(subItem))
                }
            }
        }
        return streamClusters
    }

    fun getStreamBundle(listResponse: ListResponse): StreamBundle {
        var nextPageUrl = String()
        var title = String()
        val streamClusterMap: MutableMap<Int, StreamCluster> = mutableMapOf()

        if (listResponse.itemCount > 0) {
            val item = listResponse.getItem(0)
            if (item != null && item.subItemCount > 0) {
                for (subItem in item.subItemList) {
                    val streamCluster = getStreamCluster(subItem)
                    streamClusterMap[streamCluster.id] = streamCluster
                }
                title = item.title
                nextPageUrl = getNextPageUrl(item)
            }
        }
        return StreamBundle().apply {
            streamTitle = title
            streamNextPageUrl = nextPageUrl
            streamClusters = streamClusterMap
        }
    }

    /*------------------------------------- SUBCATEGORY STREAMS & BUNDLES ------------------------------------*/

    fun getSubCategoryBundle(payload: Payload): StreamBundle {
        var streamBundle = StreamBundle()
        if (payload.hasListResponse() && payload.listResponse.itemCount > 0) {
            streamBundle = getStreamBundle(payload.listResponse)
        }
        return streamBundle
    }

    @Throws(Exception::class)
    fun getSubCategoryBundle(bytes: ByteArray?): StreamBundle {
        val responseWrapper = ResponseWrapper.parseFrom(bytes)
        var streamBundle = StreamBundle()

        if (responseWrapper.preFetchCount > 0) {
            responseWrapper.preFetchList.forEach {
                if (it.hasResponse() && it.response.hasPayload()) {
                    val payload = it.response.payload
                    val currentStreamBundle = getSubCategoryBundle(payload)
                    streamBundle.streamClusters.putAll(currentStreamBundle.streamClusters)
                }
            }
        } else if (responseWrapper.hasPayload()) {
            val payload = responseWrapper.payload
            streamBundle = getSubCategoryBundle(payload)
        }

        return streamBundle
    }

    /*------------------------------------- EDITOR'S CHOICE CLUSTER & BUNDLES ------------------------------------*/
    @Throws(Exception::class)

    private fun getEditorChoiceCluster(item: Item): EditorChoiceCluster {
        val title = if (item.hasTitle()) item.title else String()
        val artworkList: MutableList<Artwork> = ArrayList()
        val browseUrl = getBrowseUrl(item)
        if (item.imageCount > 0) {
            item.imageList.forEach {
                artworkList.add(Artwork().apply {
                    type = it.imageType
                    url = it.imageUrl
                    aspectRatio = it.dimension.aspectRatio
                    width = it.dimension.width
                    height = it.dimension.height
                })
            }
        }
        return EditorChoiceCluster().apply {
            id = browseUrl.hashCode()
            clusterTitle = title
            clusterBrowseUrl = browseUrl
            clusterArtwork = artworkList
        }
    }

    private fun getEditorChoiceBundles(item: Item): EditorChoiceBundle {
        val title = if (item.hasTitle()) item.title else String()
        val choiceClusters: MutableList<EditorChoiceCluster> = ArrayList()
        for (subItem in item.subItemList) {
            choiceClusters.add(getEditorChoiceCluster(subItem))
        }

        return EditorChoiceBundle().apply {
            id = title.hashCode()
            bundleTitle = title
            bundleChoiceClusters = choiceClusters
        }
    }

    fun getEditorChoiceBundles(listResponse: ListResponse?): List<EditorChoiceBundle> {
        val editorChoiceBundles: MutableList<EditorChoiceBundle> = ArrayList()

        listResponse?.let {
            it.itemList.forEach {
                it?.let {
                    it.subItemList.forEach {
                        it?.let {
                            val bundle = getEditorChoiceBundles(it)
                            if (bundle.bundleChoiceClusters.isNotEmpty()) {
                                editorChoiceBundles.add(bundle)
                            }
                        }
                    }
                }
            }
        }

        return editorChoiceBundles
    }
}