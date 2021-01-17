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

package com.aurora.gplayapi.data.models

import com.aurora.gplayapi.Constants.Restriction
import com.aurora.gplayapi.Features
import com.aurora.gplayapi.data.models.details.AppInfo
import com.aurora.gplayapi.data.models.details.Badge
import com.aurora.gplayapi.data.models.details.Dependencies

class App(var packageName: String) {
    var id: Int = 0
    var appInfo: AppInfo = AppInfo()
    var displayBadges: MutableList<Badge> = mutableListOf()
    var infoBadges: MutableList<Badge> = mutableListOf()
    var screenshots: MutableList<Artwork> = mutableListOf()
    var permissions: MutableList<String> = mutableListOf()
    var offerDetails: MutableMap<String, String> = mutableMapOf()
    var relatedLinks: MutableMap<String, String> = mutableMapOf()
    var dependencies: Dependencies = Dependencies()
    var categoryArtwork: Artwork = Artwork()
    var categoryId: String = String()
    var categoryName: String = String()
    var changes: String = String()
    var description: String = String()
    var developerName: String = String()
    var developerEmail: String = String()
    var developerAddress: String = String()
    var developerWebsite: String = String()
    var displayName: String = String()
    var downloadString: String = String()
    var footerHtml: String = String()
    var iconArtwork: Artwork = Artwork()
    var coverArtwork: Artwork = Artwork()
    var instantAppLink: String = String()
    var labeledRating: String = String()
    var price: String = String()
    var shortDescription: String = String()
    var testingProgramEmail: String = String()
    var updated: String = String()
    var versionName: String = String()
    var videoArtwork: Artwork = Artwork()
    var containsAds = false
    var earlyAccess = false
    var inPlayStore = false
    var isFree = false
    var isInstalled = false
    var isSystem = false
    var testingProgramAvailable = false
    var testingProgramOptedIn = false
    var offerType = 0
    var versionCode: Int = 0
    var installs: Long = 0
    var size: Long = 0
    var rating: Rating = Rating()
    var restriction: Restriction = Restriction.NOT_RESTRICTED

    var userReview: Review = Review()

    @Transient
    var features: Features? = null

    var fileList: MutableList<File> = mutableListOf()

    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is App -> packageName == other.packageName
            else -> false
        }
    }
}