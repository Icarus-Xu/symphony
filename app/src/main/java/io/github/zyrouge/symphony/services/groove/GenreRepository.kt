package io.github.zyrouge.symphony.services.groove

import io.github.zyrouge.symphony.Symphony
import io.github.zyrouge.symphony.utils.ConcurrentSet
import io.github.zyrouge.symphony.utils.FuzzySearchOption
import io.github.zyrouge.symphony.utils.FuzzySearcher
import io.github.zyrouge.symphony.utils.subListNonStrict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

enum class GenreSortBy {
    CUSTOM,
    GENRE,
    TRACKS_COUNT,
}

class GenreRepository(private val symphony: Symphony) {
    private val cache = ConcurrentHashMap<String, Genre>()
    private val songIdsCache = ConcurrentHashMap<String, ConcurrentSet<Long>>()
    private val searcher = FuzzySearcher<String>(
        options = listOf(FuzzySearchOption({ get(it)?.name }))
    )

    val isUpdating get() = symphony.groove.mediaStore.isUpdating
    private val _all = MutableStateFlow<List<String>>(emptyList())
    val all = _all.asStateFlow()
    private val _count = MutableStateFlow(0)
    val count = _count.asStateFlow()

    private fun emitCount() = _count.update {
        cache.size
    }

    internal fun onSong(song: Song) {
        if (song.additional.genre == null) return
        songIdsCache.compute(song.additional.genre) { _, value ->
            value?.apply { add(song.id) }
                ?: ConcurrentSet(song.id)
        }
        cache.compute(song.additional.genre) { _, value ->
            value?.apply {
                numberOfTracks++
            } ?: run {
                _all.update {
                    it + song.additional.genre
                }
                emitCount()
                Genre(
                    name = song.additional.genre,
                    numberOfTracks = 1,
                )
            }
        }
    }

    internal fun onFinish() {}

    fun reset() {
        cache.clear()
        songIdsCache.clear()
        _all.update {
            emptyList()
        }
        emitCount()
    }

    fun search(genreIds: List<String>, terms: String, limit: Int? = 7) = searcher
        .search(terms, genreIds)
        .subListNonStrict(limit ?: genreIds.size)

    fun sort(
        genreIds: List<String>,
        by: GenreSortBy,
        reverse: Boolean
    ): List<String> {
        val sorted = when (by) {
            GenreSortBy.CUSTOM -> genreIds
            GenreSortBy.GENRE -> genreIds.sortedBy { get(it)?.name }
            GenreSortBy.TRACKS_COUNT -> genreIds.sortedBy { get(it)?.numberOfTracks }
        }
        return if (reverse) sorted.reversed() else sorted
    }

    fun count() = cache.size
    fun ids() = cache.keys.toList()
    fun values() = cache.values.toList()

    fun get(id: String) = cache[id]
    fun getSongIds(genre: String) = songIdsCache[genre]?.toList() ?: emptyList()
}
