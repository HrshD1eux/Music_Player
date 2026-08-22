/*
 * Copyright (c) 2021 Music Player Project
 * AboutFragment.kt is part of Music Player.
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
 
package com.HrshD1eux.musicplayer.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import androidx.core.net.toUri
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.HrshD1eux.musicplayer.BuildConfig
import com.HrshD1eux.musicplayer.R
import com.HrshD1eux.musicplayer.databinding.FragmentAboutBinding
import com.HrshD1eux.musicplayer.music.MusicViewModel
import com.HrshD1eux.musicplayer.playback.formatDurationMs
import com.HrshD1eux.musicplayer.ui.ViewBindingFragment
import com.HrshD1eux.musicplayer.util.collectImmediately
import com.HrshD1eux.musicplayer.util.openInBrowser
import com.HrshD1eux.musicplayer.util.startIntent
import com.HrshD1eux.musicplayer.util.systemBarInsetsCompat
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * A [ViewBindingFragment] that displays information about the app and the current music library.
 *
 * @author HrshD1eux
 */
@AndroidEntryPoint
class AboutFragment : ViewBindingFragment<FragmentAboutBinding>() {
    private val musicModel: MusicViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateBinding(inflater: LayoutInflater) = FragmentAboutBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentAboutBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // --- UI SETUP ---
        binding.aboutToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.aboutContents.setOnApplyWindowInsetsListener { view, insets ->
            view.updatePadding(bottom = insets.systemBarInsetsCompat.bottom)
            insets
        }
        binding.aboutVersion.text = BuildConfig.VERSION_NAME
        binding.aboutCheckUpdates.setOnClickListener { checkAppUpdates() }
        binding.aboutCode.setOnClickListener { requireContext().openInBrowser(LINK_SOURCE) }
        binding.aboutWiki.setOnClickListener { requireContext().openInBrowser(LINK_WIKI) }
        binding.aboutLicenses.setOnClickListener { requireContext().openInBrowser(LINK_LICENSES) }
        binding.aboutProfile.setOnClickListener { requireContext().openInBrowser(LINK_PROFILE) }
        binding.aboutFeedbackGithub.setOnClickListener {
            requireContext().openInBrowser(LINK_NEW_ISSUE)
        }
        binding.aboutFeedbackEmail.setOnClickListener {
            requireContext().sendEmail("feedback@auxio.app")
        }

        // VIEWMODEL SETUP
        collectImmediately(musicModel.statistics, ::updateStatistics)
    }

    private fun checkAppUpdates() {
        val binding = binding ?: return
        com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                R.string.msg_checking_updates,
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT,
            )
            .show()

        viewLifecycleOwner.lifecycleScope.launch {
            when (
                val result =
                    com.HrshD1eux.musicplayer.update.UpdateManager.checkForUpdates(
                        BuildConfig.VERSION_NAME
                    )
            ) {
                is com.HrshD1eux.musicplayer.update.UpdateResult.Available -> {
                    if (!isAdded) return@launch
                    com.google.android.material.dialog
                        .MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.msg_update_available, result.version))
                        .setMessage(result.releaseNotes)
                        .setPositiveButton(R.string.btn_download_install) { _, _ ->
                            val currentBinding = binding ?: return@setPositiveButton
                            com.google.android.material.snackbar.Snackbar.make(
                                    currentBinding.root,
                                    R.string.msg_downloading_update,
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG,
                                )
                                .show()
                            com.HrshD1eux.musicplayer.update.UpdateManager.startDownloadAndInstall(
                                requireContext(),
                                result.downloadUrl,
                                result.fileName,
                            )
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                is com.HrshD1eux.musicplayer.update.UpdateResult.UpToDate -> {
                    if (!isAdded) return@launch
                    val currentBinding = binding ?: return@launch
                    com.google.android.material.snackbar.Snackbar.make(
                            currentBinding.root,
                            getString(R.string.msg_latest_version, BuildConfig.VERSION_NAME),
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT,
                        )
                        .show()
                }
                is com.HrshD1eux.musicplayer.update.UpdateResult.Error -> {
                    if (!isAdded) return@launch
                    val currentBinding = binding ?: return@launch
                    com.google.android.material.snackbar.Snackbar.make(
                            currentBinding.root,
                            result.message,
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG,
                        )
                        .show()
                }
            }
        }
    }

    private fun updateStatistics(statistics: MusicViewModel.Statistics?) {
        val binding = requireBinding()
        binding.aboutSongCount.text = getString(R.string.fmt_lib_song_count, statistics?.songs ?: 0)
        requireBinding().aboutAlbumCount.text =
            getString(R.string.fmt_lib_album_count, statistics?.albums ?: 0)
        requireBinding().aboutArtistCount.text =
            getString(R.string.fmt_lib_artist_count, statistics?.artists ?: 0)
        requireBinding().aboutGenreCount.text =
            getString(R.string.fmt_lib_genre_count, statistics?.genres ?: 0)
        binding.aboutTotalDuration.text =
            getString(
                R.string.fmt_lib_total_duration,
                (statistics?.durationMs ?: 0).formatDurationMs(false),
            )

        binding.aboutTotalSize.text =
            getString(
                R.string.fmt_lib_total_size,
                Formatter.formatFileSize(context, statistics?.totalSizeBytes ?: 0L),
            )
    }

    private fun Context.sendEmail(recipient: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply { data = "mailto:$recipient".toUri() }
        startIntent(intent)
    }

    private companion object {
        const val LINK_SOURCE = "https://github.com/HrshD1eux/Music_Player"
        const val LINK_WIKI = "$LINK_SOURCE/wiki"
        const val LINK_LICENSES = "$LINK_WIKI/Licenses"
        const val LINK_NEW_ISSUE = "$LINK_SOURCE/issues/new"
        const val LINK_PROFILE = "https://github.com/HrshD1eux"
    }
}
