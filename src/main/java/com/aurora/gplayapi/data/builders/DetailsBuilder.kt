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

import com.aurora.gplayapi.Image
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.gplayapi.data.models.details.Badge

object DetailsBuilder {

    fun build(image: Image): Artwork {
        return Artwork().apply {
            type = image.imageType
            url = image.imageUrl
            urlAlt = image.imageUrlAlt
            aspectRatio = image.dimension.aspectRatio
            width = image.dimension.width
            height = image.dimension.height
        }
    }

    fun build(badge: com.aurora.gplayapi.Badge): Badge {
        return Badge().apply {
            textMajor = badge.major
            textMinor = badge.minor
            textMinorHtml = badge.minorHtml
            textDescription = badge.description
            artwork = build(badge.image)
            link = badge.link.toString()
        }
    }
}