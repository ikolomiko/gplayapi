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
import com.aurora.gplayapi.data.models.details.TestingProgram

object TestingProgramBuilder {

    fun build(appDetails: AppDetails): TestingProgram? {
        return if (appDetails.hasTestingProgramInfo()) {
            TestingProgram().apply {
                appDetails.testingProgramInfo?.let {
                    artwork = ArtworkBuilder.build(it.image)
                    displayName = it.displayName
                    email = it.email
                    isAvailable = true
                    isSubscribed = it.subscribed
                    isSubscribedAndInstalled = it.subscribedAndInstalled
                }
            }
        } else {
            null
        }
    }
}