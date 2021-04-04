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

package com.aurora.gplayapi.data.builders

import com.aurora.gplayapi.AppDetails
import com.aurora.gplayapi.Constants
import com.aurora.gplayapi.DetailsResponse
import com.aurora.gplayapi.Item
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import com.aurora.gplayapi.data.models.details.Badge
import com.aurora.gplayapi.data.models.details.Chip
import com.aurora.gplayapi.data.models.editor.EditorChoiceReason
import com.aurora.gplayapi.utils.Util
import java.util.regex.Matcher
import java.util.regex.Pattern

object AppBuilder {

    fun build(detailsResponse: DetailsResponse): App {
        val item = detailsResponse.item
        val app = build(item)

        if (detailsResponse.hasUserReview()) {
            app.userReview = ReviewBuilder.build(detailsResponse.userReview)
        }

        app.detailsStreamUrl = detailsResponse.detailsStreamUrl
        app.detailsPostAcquireStreamUrl = detailsResponse.postAcquireDetailsStreamUrl
        app.footerHtml = detailsResponse.footerHtml

        return app
    }

    fun build(item: Item): App {
        val appDetails = item.details.appDetails
        val app = App(item.id)
        app.id = item.id.hashCode()
        app.categoryId = item.categoryId
        app.displayName = item.title
        app.description = item.descriptionHtml
        app.shortDescription = item.promotionalDescription
        app.shareUrl = item.shareUrl
        app.restriction = Constants.Restriction.forInt(item.availability.restriction)

        if (item.offerCount > 0) {
            app.offerType = item.getOffer(0).offerType
            app.isFree = item.getOffer(0).micros == 0L
            app.price = item.getOffer(0).formattedAmount
        }

        app.versionName = appDetails.versionString
        app.versionCode = appDetails.versionCode
        app.categoryName = appDetails.categoryName
        app.size = appDetails.infoDownloadSize
        app.installs = getInstalls(appDetails.infoDownload)
        app.downloadString = appDetails.downloadLabelAbbreviated
        app.changes = appDetails.recentChangesHtml
        app.permissions = appDetails.permissionList
        app.containsAds = appDetails.hasInstallNotes()
        app.inPlayStore = true
        app.earlyAccess = appDetails.hasEarlyAccessInfo()
        app.labeledRating = item.aggregateRating.ratingLabel
        app.developerName = appDetails.developerName
        app.developerEmail = appDetails.developerEmail
        app.developerAddress = appDetails.developerAddress
        app.developerWebsite = appDetails.developerWebsite
        app.targetSdk = appDetails.targetSdkVersion
        app.updatedOn = appDetails.infoUpdatedOn

        if (app.developerName.isEmpty())
            app.developerName = item.creator

        appDetails.instantLink?.let {
            app.instantAppLink = it
        }

        parseEditorReasons(app, item)
        parseAppInfo(app, item)
        parseChips(app, item)
        parseDisplayBadges(app, item)
        parseInfoBadges(app, item)
        parseStreamUrls(app, item)
        parseRating(app, item)
        parseArtwork(app, item)

        parseDependencies(app, appDetails)
        parseFiles(app, appDetails)
        parseTestingProgram(app, appDetails)

        return app
    }

    private fun parseEditorReasons(app: App, item: Item) {
        item.annotations?.let {
            if (it.hasReasons()) {
                app.editorReason = EditorChoiceReason(
                    it.reasons.bulletinList,
                    it.reasons.description
                )
            }
        }
    }

    private fun parseDisplayBadges(app: App, item: Item) {
        item.annotations?.let { annotations ->
            annotations.displayBadgeList?.let { badges ->
                badges.forEach {
                    app.displayBadges.add(
                        Badge().apply {
                            textMajor = it.major
                            textMinor = it.minor
                            textMinorHtml = it.minorHtml
                            textDescription = it.description
                            artwork = ArtworkBuilder.build(it.image)
                            link = it.link.toString()
                        }
                    )
                }
            }
        }
    }

    private fun parseInfoBadges(app: App, item: Item) {
        item.annotations?.let { annotations ->
            annotations.infoBadgeList?.let { badges ->
                badges.forEach {
                    app.infoBadges.add(BadgeBuilder.build(it))
                }
            }

            annotations.badgeForLegacyRating?.let {
                app.infoBadges.add(BadgeBuilder.build(it))
            }
        }
    }

    private fun parseFiles(app: App, appDetails: AppDetails) {
        app.fileList = appDetails.fileList
            .map {
                var fileType = File.FileType.BASE
                var fileName = "${app.packageName}.${app.versionCode}.apk"

                if (it.hasSplitId()) {
                    fileType = File.FileType.SPLIT
                    fileName = "${it.splitId}.${app.versionCode}.apk"
                } else {
                    if (it.fileType == 1) {
                        fileName = "${app.packageName}.${app.versionCode}.obb"
                        fileType = File.FileType.OBB
                    }
                }
                File().apply {
                    name = fileName
                    type = fileType
                    size = it.size
                }
            }
            .toMutableList()
    }

    private fun parseRating(app: App, item: Item) {
        app.rating = RatingBuilder.build(item.aggregateRating)
    }

    private fun parseDependencies(app: App, appDetails: AppDetails) {
        if (appDetails.hasDependencies()) {
            appDetails.dependencies.let {
                app.dependencies.apply {
                    it.dependencyList.forEach { dependency ->
                        dependentPackages.add(dependency.packageName)
                    }

                    it.splitApksList.forEach { splitId ->
                        dependentSplits.add(splitId)
                    }

                    totalSize = it.size
                    targetSDK = it.targetSdk
                }
            }
        }
    }

    private fun parseAppInfo(app: App, item: Item) {
        if (item.hasAppInfo()) {
            app.appInfo.apply {
                item.appInfo.sectionList.forEach {
                    if (it.hasLabel() && it.hasContainer() && it.container.hasDescription()) {
                        appInfoMap[it.label] = it.container.description
                    }
                }
                appInfoMap["DOWNLOAD"] = item.details.appDetails.infoDownload
                appInfoMap["UPDATED_ON"] = item.details.appDetails.infoUpdatedOn
            }
        }
    }

    private fun parseArtwork(app: App, item: Item) {
        for (image in item.imageList) {
            val artwork = ArtworkBuilder.build(image)
            when (image.imageType) {
                Constants.IMAGE_TYPE_CATEGORY_ICON -> app.categoryArtwork = artwork
                Constants.IMAGE_TYPE_APP_ICON -> app.iconArtwork = artwork
                Constants.IMAGE_TYPE_YOUTUBE_VIDEO_THUMBNAIL -> app.videoArtwork = artwork
                Constants.IMAGE_TYPE_PLAY_STORE_PAGE_BACKGROUND -> app.coverArtwork = artwork
                Constants.IMAGE_TYPE_APP_SCREENSHOT -> app.screenshots.add(artwork)
            }
        }

        if (app.screenshots.isEmpty()) {
            if (item.hasAnnotations() && item.annotations.hasSectionImage()) {
                for (imageContainer in item.annotations.sectionImage.imageContainerList) {
                    app.screenshots.add(ArtworkBuilder.build(imageContainer.image))
                }
            }
        }
    }

    private fun parseStreamUrls(app: App, item: Item) {
        app.categoryStreamUrl = item.annotations?.categoryStream?.link?.streamUrl
        app.liveStreamUrl = item.annotations?.liveStreamUrl
        app.promotionStreamUrl = item.annotations?.promotionStreamUrl
    }

    private fun parseTestingProgram(app: App, appDetails: AppDetails) {
        app.testingProgram = TestingProgramBuilder.build(appDetails)
    }

    private fun parseChips(app: App, item: Item) {
        item.annotations.chipList?.forEach {
            app.chips.add(
                Chip().apply {
                    title = it.title
                    streamUrl = it.stream?.link?.streamUrl
                }
            )
        }
    }

    private fun getInstalls(downloadInfo: String): Long {
        val matcher: Matcher = Pattern.compile("[\\d]+").matcher(downloadInfo.replace("[,.\\s]+".toRegex(), ""))
        return if (matcher.find()) Util.parseLong(matcher.group(0), 0) else 0
    }
}