/*
 * Copyright (c) 2023 Music Player Project
 * PlaylistSortDialog.kt is part of Music Player.
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
 
package com.HrshD1eux.musicplayer.home.sort

import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import com.HrshD1eux.musicplayer.home.HomeViewModel
import com.HrshD1eux.musicplayer.list.sort.Sort
import com.HrshD1eux.musicplayer.list.sort.SortDialog

/**
 * A [SortDialog] that controls the [Sort] of [HomeViewModel.playlistList].
 *
 * @author HrshD1eux
 */
@AndroidEntryPoint
class PlaylistSortDialog : SortDialog() {
    private val homeModel: HomeViewModel by activityViewModels()

    override fun getInitialSort() = homeModel.playlistSort

    override fun applyChosenSort(sort: Sort) {
        homeModel.applyPlaylistSort(sort)
    }

    override fun getModeChoices() =
        listOf(Sort.Mode.ByName, Sort.Mode.ByDuration, Sort.Mode.ByCount)
}
