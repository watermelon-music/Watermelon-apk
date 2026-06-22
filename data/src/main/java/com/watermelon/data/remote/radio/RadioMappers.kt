package com.watermelon.data.remote.radio

import com.watermelon.domain.model.RadioCountry as DomainRadioCountry
import com.watermelon.domain.model.RadioLanguage as DomainRadioLanguage
import com.watermelon.domain.model.RadioStation

fun RadioStationDto.toDomain(): RadioStation = RadioStation(
    stationuuid = stationuuid,
    name = name,
    url = url,
    urlResolved = url_resolved,
    homepage = homepage,
    favicon = favicon,
    country = country,
    countrycode = countrycode,
    language = language,
    tags = tags,
    bitrate = bitrate,
    votes = votes
)

fun RadioCountry.toDomain(): DomainRadioCountry = DomainRadioCountry(name = name, stationcount = stationcount)

fun RadioLanguage.toDomain(): DomainRadioLanguage = DomainRadioLanguage(name = name, stationcount = stationcount)
