/*
 * Copyright (c) 2021 Music Player Project
 * GenreListFragment.kt is part of Music Player.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.HrshD1eux.musicplayer.home.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.HrshD1eux.musicplayer.R
import com.HrshD1eux.musicplayer.databinding.FragmentHomeListBinding
import com.HrshD1eux.musicplayer.detail.DetailViewModel
import com.HrshD1eux.musicplayer.home.HomeViewModel
import com.HrshD1eux.musicplayer.list.ListFragment
import com.HrshD1eux.musicplayer.list.ListViewModel
import com.HrshD1eux.musicplayer.list.SelectableListListener
import com.HrshD1eux.musicplayer.list.adapter.SelectionIndicatorAdapter
import com.HrshD1eux.musicplayer.list.recycler.FastScrollRecyclerView
import com.HrshD1eux.musicplayer.list.recycler.GenreViewHolder
import com.HrshD1eux.musicplayer.list.sort.Sort
import com.HrshD1eux.musicplayer.music.IndexingState
import com.HrshD1eux.musicplayer.music.MusicViewModel
import com.HrshD1eux.musicplayer.playback.PlaybackViewModel
import com.HrshD1eux.musicplayer.playback.formatDurationMsPopup
import com.HrshD1eux.musicplayer.util.collectImmediately
import dagger.hilt.android.AndroidEntryPoint
import org.oxycblt.musikr.Genre
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.MusicParent
import org.oxycblt.musikr.Song

/**
 * A [ListFragment] that shows a list of [Genre]s.
 *
 * @author HrshD1eux
 */
@AndroidEntryPoint
class GenreListFragment :
    ListFragment<Genre, FragmentHomeListBinding>(),
    FastScrollRecyclerView.PopupProvider,
    FastScrollRecyclerView.Listener {
    private val homeModel: HomeViewModel by activityViewModels()
    private val detailModel: DetailViewModel by activityViewModels()
    override val listModel: ListViewModel by activityViewModels()
    override val musicModel: MusicViewModel by activityViewModels()
    override val playbackModel: PlaybackViewModel by activityViewModels()
    private val genreAdapter = GenreAdapter(this)

    override fun onCreateBinding(inflater: LayoutInflater) =
        FragmentHomeListBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentHomeListBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        binding.homeSwipeRefresh.setupHomeSwipeRefresh(requireContext()) { musicModel.refresh() }

        binding.homeRecycler.apply {
            id = R.id.home_genre_recycler
            adapter = genreAdapter
            popupProvider = this@GenreListFragment
            listener = this@GenreListFragment
        }

        binding.homeNoMusicPlaceholder.apply {
            setImageResource(R.drawable.ic_genre_48)
            contentDescription = getString(R.string.lbl_genres)
        }
        binding.homeNoMusicMsg.text = getString(R.string.lng_empty_genres)

        binding.homeNoMusicAction.setOnClickListener { homeModel.startChooseMusicLocations() }

        collectImmediately(homeModel.genreList, ::updateGenres)
        collectImmediately(homeModel.empty, musicModel.indexingState, ::updateNoMusicIndicator)
        collectImmediately(listModel.selected, ::updateSelection)
        collectImmediately(
            playbackModel.song,
            playbackModel.parent,
            playbackModel.isPlaying,
            ::updatePlayback,
        )
    }

    override fun onDestroyBinding(binding: FragmentHomeListBinding) {
        super.onDestroyBinding(binding)
        binding.homeRecycler.apply {
            adapter = null
            popupProvider = null
            listener = null
        }
    }

    override fun getPopupData(pos: Int): FastScrollRecyclerView.PopupProvider.PopupData? {
        val genre = homeModel.genreList.value.getOrNull(pos) ?: return null
        // Change how we display the popup depending on the current sort mode.
        return when (homeModel.genreSort.mode) {
            // By Name -> Use Name
            is Sort.Mode.ByName ->
                FastScrollRecyclerView.PopupProvider.PopupData(genre.name.thumb() ?: "?")

            // Duration -> Use compact bucket duration
            is Sort.Mode.ByDuration ->
                FastScrollRecyclerView.PopupProvider.PopupData(
                    genre.durationMs.formatDurationMsPopup()
                )

            // Count -> Use song count
            is Sort.Mode.ByCount ->
                FastScrollRecyclerView.PopupProvider.PopupData(genre.songs.size.toString())

            // Unsupported sort, error gracefully
            else -> null
        }
    }

    override fun onFastScrollingChanged(isFastScrolling: Boolean) {
        homeModel.setFastScrolling(isFastScrolling)
    }

    override fun onRealClick(item: Genre) {
        detailModel.showGenre(item)
    }

    override fun onOpenMenu(item: Genre) {
        listModel.openMenu(R.menu.parent, item)
    }

    private fun updateGenres(genres: List<Genre>) {
        genreAdapter.update(genres, homeModel.genreInstructions.consume())
    }

    private fun updateNoMusicIndicator(empty: Boolean, indexingState: IndexingState?) {
        val binding = requireBinding()
        binding.homeRecycler.isInvisible = empty
        binding.homeNoMusic.isInvisible = !empty
        binding.homeNoMusicAction.isVisible =
            indexingState == null || (empty && indexingState is IndexingState.Completed)
        binding.homeSwipeRefresh.isRefreshing = indexingState is IndexingState.Indexing
    }

    private fun updateSelection(selection: List<Music>) {
        genreAdapter.setSelected(selection.filterIsInstanceTo(mutableSetOf()))
    }

    private fun updatePlayback(song: Song?, parent: MusicParent?, isPlaying: Boolean) {
        // Only highlight the genre if it is currently playing, and if the currently
        // playing song is also contained within.
        val genre = (parent as? Genre)?.takeIf { song?.run { genres.contains(it) } ?: false }
        genreAdapter.setPlaying(genre, isPlaying)
    }

    /**
     * A [SelectionIndicatorAdapter] that shows a list of [Genre]s using [GenreViewHolder].
     *
     * @param listener An [SelectableListListener] to bind interactions to.
     */
    private class GenreAdapter(private val listener: SelectableListListener<Genre>) :
        SelectionIndicatorAdapter<Genre, GenreViewHolder>(GenreViewHolder.DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            GenreViewHolder.from(parent)

        override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
            holder.bind(getItem(position), listener)
        }
    }
}
