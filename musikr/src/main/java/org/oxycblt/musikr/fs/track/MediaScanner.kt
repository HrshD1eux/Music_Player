/*
 * Copyright (c) 2026 Music Player Project
 * MediaScanner.kt is part of Music Player.
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
 
package org.oxycblt.musikr.fs.track

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File

/**
 * Helper to immediately invoke Android's MediaScanner on specific file paths,
 * avoiding the 1-2 minute operating system daemon lag.
 */
internal object MediaScanner {
    fun scanFiles(context: Context, paths: Collection<String>, onCompleted: () -> Unit) {
        val validPaths =
            paths
                .filter {
                    val f = File(it)
                    f.exists() && !f.isDirectory
                }
                .toTypedArray()

        if (validPaths.isEmpty()) {
            onCompleted()
            return
        }

        try {
            var remaining = validPaths.size
            MediaScannerConnection.scanFile(
                context.applicationContext,
                validPaths,
                null,
            ) { _, _ ->
                remaining--
                if (remaining <= 0) {
                    onCompleted()
                }
            }
        } catch (_: Exception) {
            onCompleted()
        }
    }
}
