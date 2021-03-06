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
import com.aurora.gplayapi.ReviewResponse
import com.aurora.gplayapi.data.builders.ReviewBuilder
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.ReviewCluster
import com.aurora.gplayapi.data.providers.HeaderProvider.getDefaultHeaders
import com.aurora.gplayapi.network.IHttpClient
import java.util.*

class ReviewsHelper(authData: AuthData) : BaseHelper(authData) {

    companion object {
        const val DEFAULT_SIZE = 20
        var OFFSET = 0
    }

    override fun using(httpClient: IHttpClient) = apply {
        this.httpClient = httpClient
    }

    @Throws(Exception::class)
    private fun getReviewResponse(
        url: String,
        params: Map<String, String>,
        headers: Map<String, String>
    ): ReviewResponse? {
        val responseBody = getResponse(url, params, headers)
        val payload = getPayLoadFromBytes(responseBody.responseBytes)
        return if (payload.hasReviewResponse())
            payload.reviewResponse
        else
            null
    }

    @Throws(Exception::class)
    private fun getReviewSummaryResponse(
        url: String,
        params: Map<String, String>,
        headers: Map<String, String>
    ): ReviewResponse? {
        val responseBody = getResponse(url, params, headers)
        val payload = getPayLoadFromBytes(responseBody.responseBytes)
        return if (payload.hasReviewSummaryResponse())
            payload.reviewSummaryResponse
        else
            null
    }

    @Throws(Exception::class)
    private fun postReviewResponse(params: Map<String, String>, headers: Map<String, String>): ReviewResponse? {
        val playResponse = httpClient.post(GooglePlayApi.URL_REVIEW_ADD_EDIT, headers, params)
        val payload = getPayLoadFromBytes(playResponse.responseBytes)
        return if (payload.hasReviewResponse()) payload.reviewResponse else null
    }

    private fun getReviewCluster(reviewResponse: ReviewResponse?): ReviewCluster {
        val reviewCluster = ReviewCluster()
        reviewResponse?.let {
            val reviewList: MutableList<Review> = ArrayList()
            for (reviewProto in it.userReviewsResponse.reviewList) {
                reviewProto?.let { review ->
                    reviewList.add(ReviewBuilder.build(review))
                }
            }
            reviewCluster.reviewList.addAll(reviewList)
            reviewCluster.nextPageUrl = it.nextPageUrl
        }

        return reviewCluster
    }

    @Throws(Exception::class)
    fun getReviews(
        packageName: String,
        filter: Review.Filter,
        resultNum: Int = DEFAULT_SIZE
    ): ReviewCluster {
        val params: MutableMap<String, String> = HashMap()
        params["doc"] = packageName
        params["n"] = resultNum.toString()

        when (filter) {
            Review.Filter.ALL -> params["sfilter"] = filter.value
            Review.Filter.POSITIVE, Review.Filter.CRITICAL -> params["sent"] = filter.value
            else -> params["rating"] = filter.value
        }

        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val reviewResponse = getReviewResponse(GooglePlayApi.URL_REVIEWS, params, headers)
        return getReviewCluster(reviewResponse)
    }

    @Throws(Exception::class)
    fun getReviewSummary(
        packageName: String
    ): List<Review> {
        val params: MutableMap<String, String> = HashMap()
        params["doc"] = packageName

        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val reviewResponse = getReviewSummaryResponse("${GooglePlayApi.URL_FDFE}/reviewSummary", params, headers)
        return getReviewCluster(reviewResponse).reviewList
    }

    @Throws(Exception::class)
    fun getUserReview(packageName: String, testing: Boolean): Review? {
        val params: MutableMap<String, String> = HashMap()
        params["doc"] = packageName
        params["itpr"] = if (testing) "true" else "false"
        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val reviewResponse = getReviewResponse(GooglePlayApi.URL_REVIEWS, params, headers)

        reviewResponse?.let {
            if (it.userReviewsResponse.reviewCount > 0) {
                val review = it.userReviewsResponse.getReview(0)
                return ReviewBuilder.build(review)
            }
        }

        return null
    }

    @Throws(Exception::class)
    fun addOrEditReview(packageName: String, title: String, content: String, rating: Int, isBeta: Boolean): Review? {
        val params: MutableMap<String, String> = HashMap()
        params["doc"] = packageName
        params["title"] = title
        params["content"] = content
        params["rating"] = rating.toString()
        params["rst"] = "3"
        params["itpr"] = if (isBeta) "true" else "false"

        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val reviewResponse = postReviewResponse(params, headers)

        reviewResponse?.let {
            it.userReview?.let { review ->
                return ReviewBuilder.build(review)
            }
        }

        return null
    }

    fun next(nextPageUrl: String): ReviewCluster {
        val headers: MutableMap<String, String> = getDefaultHeaders(authData)
        val reviewResponse = getReviewResponse("${GooglePlayApi.URL_FDFE}/${nextPageUrl}", mapOf(), headers)
        return getReviewCluster(reviewResponse)
    }
}