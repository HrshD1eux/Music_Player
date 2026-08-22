/*
 * Copyright (c) 2023 Music Player Project
 * PlaylistDragCallback.kt is part of Music Player.
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
 
package com.HrshD1eux.musicplayer.detail.list

import androidx.recyclerview.widget.RecyclerView
import com.HrshD1eux.musicplayer.detail.DetailViewModel
import com.HrshD1eux.musicplayer.list.recycler.MaterialDragCallback

/**
 * A [MaterialDragCallback] extension for playlist-specific item editing.
 *
 * @author HrshD1eux
 */
class PlaylistDragCallback(private val detailModel: DetailViewModel) : MaterialDragCallback() {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ) =
        detailModel.movePlaylistSongs(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition,
        )

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        detailModel.removePlaylistSong(viewHolder.bindingAdapterPosition)
    }
}
