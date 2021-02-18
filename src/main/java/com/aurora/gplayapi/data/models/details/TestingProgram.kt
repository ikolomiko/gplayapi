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

class TestingProgram {
    var artwork: Artwork = Artwork()
    var displayName: String = String()
    var email: String = String()
    var isAvailable: Boolean = false
    var isSubscribed: Boolean = false
    var isSubscribedAndInstalled: Boolean = false
}

class TestingProgramStatus {
    var subscribed: Boolean = false
    var unsubscribed: Boolean = false
}