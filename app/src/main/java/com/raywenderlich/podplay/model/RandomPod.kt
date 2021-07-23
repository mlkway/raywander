package com.raywenderlich.podplay.model

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class RandomPod(
    @Json(name = "audio")
    val audio: String?,
    @Json(name = "audio_length_sec")
    val audioLengthSec: Int?,
    @Json(name = "description")
    val description: String?,
    @Json(name = "explicit_content")
    val explicitContent: Boolean?,
    @Json(name = "guid_from_rss")
    val guidFromRss: String?,
    @Json(name = "id")
    val id: String?,
    @Json(name = "image")
    val image: String?,
    @Json(name = "link")
    val link: String?,
    @Json(name = "listennotes_edit_url")
    val listennotesEditUrl: String?,
    @Json(name = "listennotes_url")
    val listennotesUrl: String?,
    @Json(name = "maybe_audio_invalid")
    val maybeAudioInvalid: Boolean?,
    @Json(name = "podcast")
    val podcast: Podcast?,
    @Json(name = "pub_date_ms")
    val pubDateMs: Long?,
    @Json(name = "thumbnail")
    val thumbnail: String?,
    @Json(name = "title")
    val title: String?
)