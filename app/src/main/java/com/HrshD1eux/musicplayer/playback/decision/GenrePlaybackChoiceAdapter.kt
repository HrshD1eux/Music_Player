/*
 * Copyright (c) 2023 Music Player Project
 * GenrePlaybackChoiceAdapter.kt is part of Music Player.
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
 
package com.HrshD1eux.musicplayer.playback.decision

import android.view.View
import android.view.ViewGroup
import com.HrshD1eux.musicplayer.databinding.ItemPickerChoiceBinding
import com.HrshD1eux.musicplayer.list.ClickableListListener
import com.HrshD1eux.musicplayer.list.adapter.FlexibleListAdapter
import com.HrshD1eux.musicplayer.list.adapter.SimpleDiffCallback
import com.HrshD1eux.musicplayer.list.recycler.DialogRecyclerView
import com.HrshD1eux.musicplayer.music.resolve
import com.HrshD1eux.musicplayer.util.context
import com.HrshD1eux.musicplayer.util.inflater
import org.oxycblt.musikr.Genre

/**
 * A [FlexibleListAdapter] that displays a list of [Genre] playback choices, for use with
 * [PlayFromGenreDialog].
 *
 * @param listener A [ClickableListListener] to bind interactions to.
 */
class GenrePlaybackChoiceAdapter(private val listener: ClickableListListener<Genre>) :
    FlexibleListAdapter<Genre, GenrePlaybackChoiceViewHolder>(
        GenrePlaybackChoiceViewHolder.DIFF_CALLBACK
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        GenrePlaybackChoiceViewHolder.from(parent)

    override fun onBindViewHolder(holder: GenrePlaybackChoiceViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }
}

/**
 * A [DialogRecyclerView.ViewHolder] that displays a smaller variant of a typical [Genre] item, for
 * use [GenrePlaybackChoiceAdapter]. Use [from] to create an instance.
 *
 * @author HrshD1eux
 */
class GenrePlaybackChoiceViewHolder
private constructor(private val binding: ItemPickerChoiceBinding) :
    DialogRecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     *
     * @param artist The new [Genre] to bind.
     * @param listener A [ClickableListListener] to bind interactions to.
     */
    fun bind(artist: Genre, listener: ClickableListListener<Genre>) {
        listener.bind(artist, this)
        binding.pickerImage.bind(artist)
        binding.pickerName.text = artist.name.resolve(binding.context)
    }

    companion object {

        /**
         * Create a new instance.
         *
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            GenrePlaybackChoiceViewHolder(ItemPickerChoiceBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Genre>() {
                override fun areContentsTheSame(oldItem: Genre, newItem: Genre) =
                    oldItem.name == newItem.name
            }
    }
}
