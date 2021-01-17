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

import com.aurora.gplayapi.AggregateRating
import com.aurora.gplayapi.data.models.Rating

object RatingBuilder {

    fun build(rating: AggregateRating): Rating {
        return Rating(
                rating.starRating,
                rating.oneStarRatings,
                rating.twoStarRatings,
                rating.threeStarRatings,
                rating.fourStarRatings,
                rating.fiveStarRatings,
                rating.thumbsUpCount,
                rating.thumbsDownCount,
                rating.ratingLabel,
                rating.ratingCountLabelAbbreviated
        )
    }
}