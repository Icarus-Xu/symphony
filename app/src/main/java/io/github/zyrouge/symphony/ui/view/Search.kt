package io.github.zyrouge.symphony.ui.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zyrouge.symphony.services.groove.GrooveKinds
import io.github.zyrouge.symphony.ui.components.AlbumArtistDropdownMenu
import io.github.zyrouge.symphony.ui.components.AlbumDropdownMenu
import io.github.zyrouge.symphony.ui.components.ArtistDropdownMenu
import io.github.zyrouge.symphony.ui.components.GenericGrooveCard
import io.github.zyrouge.symphony.ui.components.IconTextBody
import io.github.zyrouge.symphony.ui.components.NowPlayingBottomBar
import io.github.zyrouge.symphony.ui.components.PlaylistDropdownMenu
import io.github.zyrouge.symphony.ui.components.SongCard
import io.github.zyrouge.symphony.ui.helpers.RoutesBuilder
import io.github.zyrouge.symphony.ui.helpers.ViewContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SearchResult(
    val songIds: List<Long>,
    val artistIds: List<String>,
    val albumIds: List<Long>,
    val albumArtistIds: List<String>,
    val genreIds: List<String>,
    val playlistIds: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(context: ViewContext, initialChip: GrooveKinds?) {
    val coroutineScope = rememberCoroutineScope()
    var terms by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<SearchResult?>(null) }

    var selectedChip by rememberSaveable { mutableStateOf(initialChip) }
    fun isChipSelected(kind: GrooveKinds) = selectedChip == null || selectedChip == kind

    var currentTermsRoutine: Job? = null
    fun setTerms(nTerms: String) {
        terms = nTerms
        isSearching = true
        currentTermsRoutine?.cancel()
        currentTermsRoutine = coroutineScope.launch {
            withContext(Dispatchers.Default) {
                delay(250)
                val songIds = mutableListOf<Long>()
                val artistIds = mutableListOf<String>()
                val albumIds = mutableListOf<Long>()
                val albumArtistIds = mutableListOf<String>()
                val genreIds = mutableListOf<String>()
                val playlistIds = mutableListOf<String>()

                if (nTerms.isNotEmpty()) {
                    if (isChipSelected(GrooveKinds.SONG)) {
                        songIds.addAll(
                            context.symphony.groove.song
                                .search(context.symphony.groove.song.ids(), terms)
                                .map { it.entity }
                        )
                    }
                    if (isChipSelected(GrooveKinds.ARTIST)) {
                        artistIds.addAll(
                            context.symphony.groove.artist
                                .search(context.symphony.groove.artist.ids(), terms)
                                .map { it.entity }
                        )
                    }
                    if (isChipSelected(GrooveKinds.ALBUM)) {
                        albumIds.addAll(
                            context.symphony.groove.album
                                .search(context.symphony.groove.album.ids(), terms)
                                .map { it.entity }
                        )
                    }
                    if (isChipSelected(GrooveKinds.ALBUM_ARTIST)) {
                        albumArtistIds.addAll(
                            context.symphony.groove.albumArtist
                                .search(context.symphony.groove.albumArtist.ids(), terms)
                                .map { it.entity }
                        )
                    }
                    if (isChipSelected(GrooveKinds.GENRE)) {
                        genreIds.addAll(
                            context.symphony.groove.genre
                                .search(context.symphony.groove.genre.ids(), terms)
                                .map { it.entity }
                        )
                    }
                    if (isChipSelected(GrooveKinds.PLAYLIST)) {
                        playlistIds.addAll(
                            context.symphony.groove.playlist
                                .search(context.symphony.groove.playlist.ids(), terms)
                                .map { it.entity }
                        )
                    }

                    results = SearchResult(
                        songIds = songIds,
                        artistIds = artistIds,
                        albumIds = albumIds,
                        albumArtistIds = albumArtistIds,
                        genreIds = genreIds,
                        playlistIds = playlistIds,
                    )
                }
                isSearching = false
            }
        }
    }

    val textFieldFocusRequester = FocusRequester()
    val configuration = LocalConfiguration.current
    LaunchedEffect(LocalContext.current) {
        textFieldFocusRequester.requestFocus()
        snapshotFlow { configuration.orientation }.collect {
            setTerms(terms)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .clipToBounds()
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(textFieldFocusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    singleLine = true,
                    value = terms,
                    onValueChange = { setTerms(it) },
                    placeholder = {
                        Text(context.symphony.t.SearchYourMusic)
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                context.navController.popBackStack()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    trailingIcon = {
                        if (terms.isNotEmpty()) {
                            IconButton(
                                onClick = { setTerms("") }
                            ) {
                                Icon(Icons.Filled.Close, null)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = selectedChip == null,
                        label = {
                            Text(context.symphony.t.All)
                        },
                        onClick = {
                            selectedChip = null
                            setTerms(terms)
                        }
                    )
                    GrooveKinds.entries.map {
                        FilterChip(
                            selected = selectedChip == it,
                            label = {
                                Text(it.label(context))
                            },
                            onClick = {
                                selectedChip = it
                                setTerms(terms)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        content = { contentPadding ->
            results?.run {
                val hasSongs = isChipSelected(GrooveKinds.SONG) && songIds.isNotEmpty()
                val hasArtists = isChipSelected(GrooveKinds.ARTIST) && artistIds.isNotEmpty()
                val hasAlbums = isChipSelected(GrooveKinds.ALBUM) && albumIds.isNotEmpty()
                val hasAlbumArtists =
                    isChipSelected(GrooveKinds.ALBUM_ARTIST) && albumArtistIds.isNotEmpty()
                val hasPlaylists =
                    isChipSelected(GrooveKinds.PLAYLIST) && playlistIds.isNotEmpty()
                val hasGenres = isChipSelected(GrooveKinds.GENRE) && genreIds.isNotEmpty()
                val hasNoResults =
                    !hasSongs && !hasArtists && !hasAlbums && !hasAlbumArtists && !hasPlaylists && !hasGenres

                Box(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                ) {
                    if (terms.isNotEmpty()) {
                        when {
                            isSearching -> {
                                Box(modifier = Modifier.align(Alignment.Center)) {
                                    IconTextBody(
                                        icon = { modifier ->
                                            Icon(
                                                Icons.Filled.Search,
                                                null,
                                                modifier = modifier
                                            )
                                        },
                                        content = {
                                            Text(context.symphony.t.FilteringResults)
                                        }
                                    )
                                }
                            }

                            hasNoResults -> {
                                Box(modifier = Modifier.align(Alignment.Center)) {
                                    IconTextBody(
                                        icon = { modifier ->
                                            Icon(
                                                Icons.Filled.PriorityHigh,
                                                null,
                                                modifier = modifier
                                            )
                                        },
                                        content = {
                                            Text(context.symphony.t.NoResultsFound)
                                        }
                                    )
                                }
                            }

                            else -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    if (hasSongs) {
                                        SideHeading(context, GrooveKinds.SONG)
                                        songIds.forEach { songId ->
                                            context.symphony.groove.song.get(songId)?.let { song ->
                                                SongCard(context, song) {
                                                    context.symphony.radio.shorty.playQueue(song.id)
                                                }
                                            }
                                        }
                                    }
                                    if (hasArtists) {
                                        SideHeading(context, GrooveKinds.ARTIST)
                                        artistIds.forEach { artistId ->
                                            context.symphony.groove.artist.get(artistId)
                                                ?.let { artist ->
                                                    GenericGrooveCard(
                                                        image = artist
                                                            .createArtworkImageRequest(context.symphony)
                                                            .build(),
                                                        title = {
                                                            Text(artist.name)
                                                        },
                                                        options = { expanded, onDismissRequest ->
                                                            ArtistDropdownMenu(
                                                                context,
                                                                artist,
                                                                expanded = expanded,
                                                                onDismissRequest = onDismissRequest,
                                                            )
                                                        },
                                                        onClick = {
                                                            context.navController.navigate(
                                                                RoutesBuilder.buildArtistRoute(
                                                                    artist.name
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                    if (hasAlbums) {
                                        SideHeading(context, GrooveKinds.ALBUM)
                                        albumIds.forEach { albumId ->
                                            context.symphony.groove.album.get(albumId)
                                                ?.let { album ->
                                                    GenericGrooveCard(
                                                        image = album
                                                            .createArtworkImageRequest(context.symphony)
                                                            .build(),
                                                        title = {
                                                            Text(album.name)
                                                        },
                                                        subtitle = album.artist?.let { { Text(it) } },
                                                        options = { expanded, onDismissRequest ->
                                                            AlbumDropdownMenu(
                                                                context,
                                                                album,
                                                                expanded = expanded,
                                                                onDismissRequest = onDismissRequest,
                                                            )
                                                        },
                                                        onClick = {
                                                            context.navController.navigate(
                                                                RoutesBuilder.buildAlbumRoute(album.id)
                                                            )
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                    if (hasAlbumArtists) {
                                        SideHeading(context, GrooveKinds.ALBUM_ARTIST)
                                        albumArtistIds.forEach { albumArtistId ->
                                            context.symphony.groove.albumArtist.get(albumArtistId)
                                                ?.let { albumArtist ->
                                                    GenericGrooveCard(
                                                        image = albumArtist
                                                            .createArtworkImageRequest(context.symphony)
                                                            .build(),
                                                        title = {
                                                            Text(albumArtist.name)
                                                        },
                                                        options = { expanded, onDismissRequest ->
                                                            AlbumArtistDropdownMenu(
                                                                context,
                                                                albumArtist,
                                                                expanded = expanded,
                                                                onDismissRequest = onDismissRequest,
                                                            )
                                                        },
                                                        onClick = {
                                                            context.navController.navigate(
                                                                RoutesBuilder.buildAlbumArtistRoute(
                                                                    albumArtist.name
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                    if (hasPlaylists) {
                                        SideHeading(context, GrooveKinds.PLAYLIST)
                                        playlistIds.forEach { playlistId ->
                                            context.symphony.groove.playlist.get(playlistId)
                                                ?.let { playlist ->
                                                    GenericGrooveCard(
                                                        image = playlist
                                                            .createArtworkImageRequest(context.symphony)
                                                            .build(),
                                                        title = {
                                                            Text(playlist.title)
                                                        },
                                                        options = { expanded, onDismissRequest ->
                                                            PlaylistDropdownMenu(
                                                                context,
                                                                playlist,
                                                                expanded = expanded,
                                                                onDismissRequest = onDismissRequest,
                                                            )
                                                        },
                                                        onClick = {
                                                            context.navController.navigate(
                                                                RoutesBuilder.buildPlaylistRoute(
                                                                    playlist.id
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                    if (hasGenres) {
                                        SideHeading(context, GrooveKinds.GENRE)
                                        genreIds.forEach { genreId ->
                                            context.symphony.groove.genre.get(genreId)
                                                ?.let { genre ->
                                                    GenericGrooveCard(
                                                        image = null,
                                                        title = { Text(genre.name) },
                                                        subtitle = {
                                                            Text(
                                                                context.symphony.t.XSongs(
                                                                    genre.numberOfTracks.toString()
                                                                )
                                                            )
                                                        },
                                                        options = null,
                                                        onClick = {
                                                            context.navController.navigate(
                                                                RoutesBuilder.buildGenreRoute(genre.name)
                                                            )
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NowPlayingBottomBar(context)
        }
    )
}

@Composable
private fun SideHeading(context: ViewContext, kind: GrooveKinds) {
    SideHeading(kind.label(context))
}

@Composable
private fun SideHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 4.dp)
    )
}

private fun GrooveKinds.label(context: ViewContext) = when (this) {
    GrooveKinds.SONG -> context.symphony.t.Songs
    GrooveKinds.ALBUM -> context.symphony.t.Albums
    GrooveKinds.ARTIST -> context.symphony.t.Artists
    GrooveKinds.ALBUM_ARTIST -> context.symphony.t.AlbumArtists
    GrooveKinds.GENRE -> context.symphony.t.Genres
    GrooveKinds.PLAYLIST -> context.symphony.t.Playlists
}
