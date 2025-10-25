package org.listenbrainz.android.ui.screens.feed.events

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.listenbrainz.android.model.Metadata
import org.listenbrainz.android.model.feed.FeedEvent
import org.listenbrainz.android.model.feed.FeedEventType
import org.listenbrainz.android.model.feed.FeedListenArtist
import org.listenbrainz.android.ui.components.ListenCardSmall
import org.listenbrainz.android.ui.screens.feed.BaseFeedLayout
import org.listenbrainz.android.ui.screens.feed.SocialDropdown
import org.listenbrainz.android.ui.theme.ListenBrainzTheme
import org.listenbrainz.android.util.PreviewSurface
import org.listenbrainz.android.util.Utils

@Composable
fun PinFeedLayout(
    event: FeedEvent,
    isHidden: Boolean,
    parentUser: String,
    onDeleteOrHide: () -> Unit,
    onDropdownClick: () -> Unit,
    onClick: () -> Unit,
    dropdownState: Int?,
    index: Int,
    onOpenInMusicBrainz: () -> Unit,
    onPin: () -> Unit,
    onRecommend: () -> Unit,
    onPersonallyRecommend: () -> Unit,
    onReview: () -> Unit,
    goToUserPage: (String) -> Unit,
    goToArtistPage: (String) -> Unit,
) {
    BaseFeedLayout(
        eventType = FeedEventType.RECORDING_PIN,
        event = event,
        isHidden = isHidden,
        parentUser = parentUser,
        onDeleteOrHide = onDeleteOrHide,
        goToUserPage = goToUserPage,
    ) {
        ListenCardSmall(
            trackName = event.metadata.trackMetadata?.trackName ?: "Unknown",
            artists = event.metadata.trackMetadata?.mbidMapping?.artists ?: listOf(FeedListenArtist(event.metadata.trackMetadata?.artistName ?: "" , null, "")),
            coverArtUrl = remember {
                Utils.getCoverArtUrl(
                    caaReleaseMbid = event.metadata.trackMetadata?.mbidMapping?.caaReleaseMbid,
                    caaId = event.metadata.trackMetadata?.mbidMapping?.caaId
                )
            },
            onDropdownIconClick = onDropdownClick,
            dropDown = {
                SocialDropdown(
                    isExpanded = dropdownState == index,
                    onDismiss = onDropdownClick,
                    metadata = event.metadata,
                    onOpenInMusicBrainz = onOpenInMusicBrainz,
                    onPin = onPin,
                    onRecommend = onRecommend,
                    onPersonallyRecommend = onPersonallyRecommend,
                    onReview = onReview
                )
            },
            blurbContent = if (!event.blurbContent.isNullOrBlank()) {
                { modifier ->
                    Column(modifier = modifier) {
                        Text(
                            text = event.blurbContent!!,
                            style = ListenBrainzTheme.textStyles.feedBlurbContent,
                            color = ListenBrainzTheme.colorScheme.text
                        )
                    }
                }
            } else null,
            goToArtistPage = goToArtistPage,
            onClick = onClick
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PinFeedLayoutPreview() {
    PreviewSurface {
        PinFeedLayout(
            event = FeedEvent(
                id = 0,
                created = 0,
                type = "like",
                hidden = false, metadata = Metadata(blurbContent = "Good song."),
                username = "JasjeetTest"
            ),
            onDeleteOrHide = {},
            onDropdownClick = {},
            parentUser = "Jasjeet",
            isHidden = false,
            onClick = {},
            dropdownState = null,
            index = 0,
            onOpenInMusicBrainz = {},
            onPin = {},
            onRecommend = {},
            onPersonallyRecommend = {},
            onReview = {},
            goToUserPage = {},
            goToArtistPage = {},
        )
    }
}