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

package com.aurora.gplayapi.data.models.details

import com.aurora.gplayapi.data.models.Artwork
import java.util.*

class Badge {
    var id: String = UUID.randomUUID().toString()
    var textMajor: String = String()
    var textMinor: String = String()
    var textMinorHtml: String? = String()
    var textDescription: String? = String()
    var artwork: Artwork? = null
    var link: String = String()

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Badge -> other.id == id
            else -> false
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}