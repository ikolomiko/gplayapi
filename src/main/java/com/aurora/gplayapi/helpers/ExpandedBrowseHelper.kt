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

import com.aurora.gplayapi.Item
import com.aurora.gplayapi.data.builders.AppBuilder
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.network.IHttpClient

class ExpandedBrowseHelper(authData: AuthData) : BaseHelper(authData) {

    override fun using(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    fun getExpandedBrowseClusters(expandedBrowseUrl: String): StreamCluster {
        val listResponse = getNextStreamResponse(expandedBrowseUrl)
        return getStreamCluster(listResponse.getItem(0))
    }

    override fun getStreamCluster(item: Item): StreamCluster {
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

    override fun getAppsFromItem(item: Item): MutableList<App> {
        val appList: MutableList<App> = mutableListOf()
        item.subItemList.forEach {
            it?.let {
                it.subItemList.forEach {
                    it?.let {
                        appList.add(AppBuilder.build(it))
                    }
                }
            }
        }
        return appList
    }
}