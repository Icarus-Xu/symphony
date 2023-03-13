package io.github.zyrouge.symphony.ui.components

import androidx.compose.runtime.Composable
import io.github.zyrouge.symphony.services.groove.Playlist
import io.github.zyrouge.symphony.ui.helpers.ViewContext

@Composable
fun PlaylistInformationDialog(
    context: ViewContext,
    playlist: Playlist,
    onDismissRequest: () -> Unit
) {
    InformationDialog(
        context,
        content = {
            InformationKeyValue(
                context.symphony.t.Title,
                playlist.title
            )
            InformationKeyValue(
                context.symphony.t.TrackCount,
                playlist.numberOfTracks.toString()
            )
            InformationKeyValue(
                context.symphony.t.IsLocalPlaylist,
                when {
                    playlist.isLocal() -> context.symphony.t.Yes
                    else -> context.symphony.t.No
                },
            )
            playlist.local?.let { local ->
                InformationKeyValue(
                    context.symphony.t.Path,
                    local.path,
                )
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
