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
import com.aurora.gplayapi.Item
import com.aurora.gplayapi.ListResponse
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.builders.AppBuilder
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.gplayapi.data.models.SearchBundle.SubBundle
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.network.IHttpClient
import java.util.*

class SearchHelper(authData: AuthData) : BaseHelper(authData) {

    private val searchTypeExtra = "_-"

    private fun getSubBundle(item: Item): SubBundle {
        try {
            val nextPageUrl = item.containerMetadata.nextPageUrl
            if (nextPageUrl.isNotBlank()) {
                if (nextPageUrl.contains(searchTypeExtra)) {
                    if (nextPageUrl.startsWith("getCluster?enpt=CkC"))
                        return SubBundle(nextPageUrl, SearchBundle.Type.SIMILAR)
                    if (nextPageUrl.startsWith("getCluster?enpt=CkG"))
                        return SubBundle(nextPageUrl, SearchBundle.Type.RELATED_TO_YOUR_SEARCH)
                } else {
                    return SubBundle(nextPageUrl, SearchBundle.Type.GENERIC)
                }
            }
        } catch (ignored: Exception) {
        }
        return SubBundle("", SearchBundle.Type.BOGUS)
    }

    override fun using(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    private var query: String = String()

    @Throws(Exception::class)
    fun searchSuggestions(query: String): List<SearchSuggestEntry> {
        val header: MutableMap<String, String> = getDefaultHeaders(authData)
        val paramString = String.format(
            "?q=%s&sb=%d&sst=%d&sst=%d",
            query,
            5,
            2 /*Text Entry*/,
            3 /*Item Doc Id : 3 -> Apps*/
        )
        val responseBody = httpClient.get(GooglePlayApi.URL_SEARCH_SUGGEST, header, paramString)
        val searchSuggestResponse = getSearchSuggestResponseFromBytes(responseBody.responseBytes)
        return if (searchSuggestResponse != null && searchSuggestResponse.entryCount > 0) {
            searchSuggestResponse.entryList
        } else ArrayList()
    }

    @Throws(Exception::class)
    fun searchResults(query: String, nextPageUrl: String = ""): SearchBundle {
        this.query = query
        val header: MutableMap<String, String> = getDefaultHeaders(authData)
        val param: MutableMap<String, String> = HashMap()
        param["q"] = query
        param["c"] = "3"
        param["ksm"] = "1"

        val responseBody: PlayResponse = if (nextPageUrl.isNotEmpty()) {
            httpClient.get(GooglePlayApi.URL_SEARCH + "/" + nextPageUrl, header)
        } else {
            httpClient.get(GooglePlayApi.URL_SEARCH, header, param)
        }

        var searchBundle = SearchBundle()

        if (responseBody.isSuccessful) {
            val payload = getPrefetchPayLoad(responseBody.responseBytes)
            if (payload.hasListResponse()) {
                searchBundle = getSearchBundle(payload.listResponse)
                searchBundle.subBundles = searchBundle.subBundles
                    .filter { it.type == SearchBundle.Type.GENERIC }
                    .toMutableSet()
                return searchBundle
            }
        }

        return searchBundle
    }

    @Throws(Exception::class)
    fun next(bundleSet: MutableSet<SubBundle>): SearchBundle {
        val compositeSearchBundle = SearchBundle()

        bundleSet.forEach {
            val searchBundle = searchResults(query, it.nextPageUrl)
            compositeSearchBundle.appList.addAll(searchBundle.appList)
            compositeSearchBundle.subBundles.addAll(searchBundle.subBundles)
        }

        return compositeSearchBundle
    }

    private fun getSearchBundle(listResponse: ListResponse): SearchBundle {
        val searchBundle = SearchBundle()
        val appList: MutableList<App> = mutableListOf()
        val itemList = listResponse.itemList
        for (item in itemList) {
            if (item.subItemCount > 0) {
                for (subItem in item.subItemList) {
                    //Filter out only apps, discard other items (Music, Ebooks, Movies)
                    if (subItem.type == 45) {
                        if (subItem.title.isEmpty() || subItem.title == "Apps") {
                            appList.addAll(getAppsFromItem(subItem))
                        } else {
                            if (subItem.title.isNotEmpty())
                                continue // Filter out `You Might Also Like` & `Related Apps`
                            appList.add(AppBuilder.build(subItem))
                        }
                    }
                    searchBundle.subBundles.add(getSubBundle(subItem))
                }
                searchBundle.subBundles.add(getSubBundle(item))
            }
        }
        searchBundle.appList = appList
        return searchBundle
    }
}