package org.listenbrainz.android.model

import com.google.gson.annotations.SerializedName

data class CurrentPins(
    @SerializedName("pinned_recording") val pinnedRecording: PinnedRecording? = null
)

data class PinnedRecording(
    @SerializedName("created"        ) val created: Long? = null,
    @SerializedName("row_id"         ) val rowId: Int? = null,
    @SerializedName("track_metadata" ) val trackMetadata: TrackMetadata? = null,
    
    // Only below fields are used for posting pins.
    @SerializedName("recording_msid" ) val recordingMsid : String? = null,
    @SerializedName("recording_mbid" ) val recordingMbid : String? = null,
    @SerializedName("blurb_content"  ) val blurbContent  : String? = null,
    @SerializedName("pinned_until"   ) val pinnedUntil   : Int?    = null
) {
    fun toMetadata() = Metadata(
        trackMetadata = trackMetadata,
        blurbContent = blurbContent,
        created = created,
    )
}