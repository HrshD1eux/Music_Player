/*
 * Copyright (c) 2023 Music Player Project
 * PlaylistListFragment.kt is part of Music Player.
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
import com.HrshD1eux.musicplayer.list.recycler.PlaylistViewHolder
import com.HrshD1eux.musicplayer.list.sort.Sort
import com.HrshD1eux.musicplayer.music.IndexingState
import com.HrshD1eux.musicplayer.music.MusicViewModel
import com.HrshD1eux.musicplayer.playback.PlaybackViewModel
import com.HrshD1eux.musicplayer.playback.formatDurationMsPopup
import com.HrshD1eux.musicplayer.util.collectImmediately
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.MusicParent
import org.oxycblt.musikr.Playlist
import org.oxycblt.musikr.Song

/**
 * A [ListFragment] that shows a list of [Playlist]s.
 *
 * @author HrshD1eux
 */
class PlaylistListFragment :
    ListFragment<Playlist, FragmentHomeListBinding>(),
    FastScrollRecyclerView.PopupProvider,
    FastScrollRecyclerView.Listener {
    private val homeModel: HomeViewModel by activityViewModels()
    private val detailModel: DetailViewModel by activityViewModels()
    override val listModel: ListViewModel by activityViewModels()
    override val musicModel: MusicViewModel by activityViewModels()
    override val playbackModel: PlaybackViewModel by activityViewModels()
    private val playlistAdapter = PlaylistAdapter(this)

    override fun onCreateBinding(inflater: LayoutInflater) =
        FragmentHomeListBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentHomeListBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        binding.homeSwipeRefresh.setupHomeSwipeRefresh(requireContext()) {
            musicModel.refresh()
        }

        binding.homeRecycler.apply {
            id = R.id.home_playlist_recycler
            adapter = playlistAdapter
            popupProvider = this@PlaylistListFragment
            listener = this@PlaylistListFragment
        }

        binding.homeNoMusicPlaceholder.apply {
            setImageResource(R.drawable.ic_playlist_48)
            contentDescription = getString(R.string.lbl_playlists)
        }
        binding.homeNoMusicMsg.text = getString(R.string.lng_empty_playlists)

        collectImmediately(homeModel.playlistList, ::updatePlaylists)
        collectImmediately(
            homeModel.empty,
            homeModel.playlistList,
            musicModel.indexingState,
            ::updateNoMusicIndicator,
        )
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
        val playlist = homeModel.playlistList.value.getOrNull(pos) ?: return null
        // Change how we display the popup depending on the current sort mode.
        return when (homeModel.playlistSort.mode) {
            // By Name -> Use Name
            is Sort.Mode.ByName ->
                FastScrollRecyclerView.PopupProvider.PopupData(playlist.name.thumb() ?: "?")

            // Duration -> Use compact bucket duration
            is Sort.Mode.ByDuration ->
                FastScrollRecyclerView.PopupProvider.PopupData(
                    playlist.durationMs.formatDurationMsPopup()
                )

            // Count -> Use song count
            is Sort.Mode.ByCount ->
                FastScrollRecyclerView.PopupProvider.PopupData(playlist.songs.size.toString())

            // Unsupported sort, error gracefully
            else -> null
        }
    }

    override fun onFastScrollingChanged(isFastScrolling: Boolean) {
        homeModel.setFastScrolling(isFastScrolling)
    }

    override fun onRealClick(item: Playlist) {
        detailModel.showPlaylist(item)
    }

    override fun onOpenMenu(item: Playlist) {
        listModel.openMenu(R.menu.playlist, item)
    }

    private fun updatePlaylists(playlists: List<Playlist>) {
        playlistAdapter.update(playlists, homeModel.playlistInstructions.consume())
    }

    private fun updateNoMusicIndicator(
        empty: Boolean,
        playlists: List<Playlist>,
        indexingState: IndexingState?,
    ) {
        val binding = requireBinding()
        binding.homeRecycler.isInvisible = empty
        binding.homeNoMusic.isInvisible = !empty && playlists.isNotEmpty()
        if (!empty && playlists.isEmpty()) {
            binding.homeNoMusicAction.isVisible = true
            binding.homeNoMusicAction.text = getString(R.string.lbl_new_playlist)
            binding.homeNoMusicAction.setOnClickListener { musicModel.createPlaylist() }
        } else {
            binding.homeNoMusicAction.isVisible =
                indexingState == null || (empty && indexingState is IndexingState.Completed)
            binding.homeNoMusicAction.text = getString(R.string.set_locations)
            binding.homeNoMusicAction.setOnClickListener { homeModel.startChooseMusicLocations() }
        }
        binding.homeSwipeRefresh.isRefreshing = indexingState is IndexingState.Indexing
    }

    private fun updateSelection(selection: List<Music>) {
        playlistAdapter.setSelected(selection.filterIsInstanceTo(mutableSetOf()))
    }

    private fun updatePlayback(song: Song?, parent: MusicParent?, isPlaying: Boolean) {
        // Only highlight the playlist if it is currently playing, and if the currently
        // playing song is also contained within.
        val playlist = (parent as? Playlist)?.takeIf { it.songs.contains(song) }
        playlistAdapter.setPlaying(playlist, isPlaying)
    }

    /**
     * A [SelectionIndicatorAdapter] that shows a list of [Playlist]s using [PlaylistViewHolder].
     *
     * @param listener An [SelectableListListener] to bind interactions to.
     */
    private class PlaylistAdapter(private val listener: SelectableListListener<Playlist>) :
        SelectionIndicatorAdapter<Playlist, PlaylistViewHolder>(PlaylistViewHolder.DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistViewHolder.from(parent)

        override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
            holder.bind(getItem(position), listener)
        }
    }
}
