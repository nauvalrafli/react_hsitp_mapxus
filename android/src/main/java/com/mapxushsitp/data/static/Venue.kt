package com.mapxushsitp.data.static

import com.mapxushsitp.data.model.Venue
import com.mapxushsitp.data.model.Venue.MultilingualText

val venues = listOf(
    Venue(
        buildingId = "996debebfddb4bc2895cdbeb70161d5a",
        name = MultilingualText(
            zhHans = "港深创新及科技园",
            zhHant = "港深創新及科技園",
            en = "Hong Kong-Shenzhen Innovation And Technology Park",
            default = "Hong Kong-Shenzhen Innovation And Technology Park 港深創新及科技園"
        ),
        buildingName = MultilingualText(
            zhHans = "11号大楼",
            zhHant = "11號大樓",
            en = "Building 11",
            default = "Building 11"
        ),
        address = Venue.MultilingualAddress(
            zhHans = Venue.Address("1", "落马洲河套地区"),
            zhHant = Venue.Address("1", "落馬洲河套地區"),
            en = Venue.Address("1", "Lok Ma Chau Loop"),
            default = Venue.Address("1", "Lok Ma Chau Loop")
        ),
        description = emptyMap(),
        venueId = "2506d124f4d049fb8b5019ed9d78c309",
        venueName = MultilingualText(
            zhHans = "港深创新及科技园",
            zhHant = "港深創新及科技園",
            en = "Hong Kong-Shenzhen Innovation And Technology Park",
            default = "Hong Kong-Shenzhen Innovation And Technology Park 港深創新及科技園"
        ),
        defaultFloor = "11cb3dd6af214a3e9cba6fd4718b145d",
        type = "commercial.office",
        buildingOutlineId = 14182589,
        bbox = Venue.Bbox(
            maxLat = 22.51711802912,
            maxLon = 114.0773124533,
            minLat = 22.51649600047,
            minLon = 114.07668277778
        ),
        labelCenter = Venue.LabelCenter(
            lat = 22.5168438581,
            lon = 114.07699837962
        ),
        country = "CN",
        region = "HK",
        visualMap = false,
        signalMap = true,
        floors = listOf(
            Venue.Floor(
                id = "ad24bdcb0698422f8c8ab53ad6bb2665",
                code = "MF",
                ordinal = 1,
                visualMap = false,
                signalMap = true
            )
        ),
        isPrivate = "yes",
        organization = ""
    )
)
