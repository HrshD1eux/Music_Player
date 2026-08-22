/*
 * Copyright (c) 2026 Music Player Project
 * DirectoryFileObserver.kt is part of Music Player.
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

import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * A multi-directory recursive [FileObserver] that monitors target folders for real-time
 * filesystem modifications, bypassing Android's sluggish background MediaStore polling.
 */
internal class DirectoryFileObserver(
    private val rootPaths: List<String>,
    private val onFilesChanged: (changedAudioPaths: Set<String>, hasDeletions: Boolean) -> Unit,
) {
    private val observers = ConcurrentHashMap<String, SingleDirObserver>()
    private val handler = Handler(Looper.getMainLooper())
    private val pendingChangedPaths = mutableSetOf<String>()
    private var pendingHasDeletions = false
    private var isWatching = false

    private val debounceRunnable = Runnable {
        val changed: Set<String>
        val deletions: Boolean
        synchronized(pendingChangedPaths) {
            changed = pendingChangedPaths.toSet()
            deletions = pendingHasDeletions
            pendingChangedPaths.clear()
            pendingHasDeletions = false
        }
        if (changed.isNotEmpty() || deletions) {
            onFilesChanged(changed, deletions)
        }
    }

    fun startWatching() {
        if (isWatching) return
        isWatching = true
        for (path in rootPaths) {
            watchDirectoryRecursively(File(path))
        }
    }

    fun stopWatching() {
        if (!isWatching) return
        isWatching = false
        handler.removeCallbacks(debounceRunnable)
        synchronized(pendingChangedPaths) {
            pendingChangedPaths.clear()
            pendingHasDeletions = false
        }
        for ((_, observer) in observers) {
            try {
                observer.stopWatching()
            } catch (_: Exception) {}
        }
        observers.clear()
    }

    private fun watchDirectoryRecursively(dir: File) {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return
        if (File(dir, ".nomedia").exists()) return
        val canonicalPath =
            try {
                dir.canonicalPath
            } catch (_: Exception) {
                dir.absolutePath
            }
        if (observers.containsKey(canonicalPath)) return

        val observer = SingleDirObserver(canonicalPath)
        observers[canonicalPath] = observer
        try {
            observer.startWatching()
        } catch (_: Exception) {
            observers.remove(canonicalPath)
            return
        }

        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory && !child.name.startsWith(".") && !File(child, ".nomedia").exists()) {
                watchDirectoryRecursively(child)
            }
        }
    }

    private fun handleEvent(parentPath: String, event: Int, name: String?) {
        if (name == null || name.startsWith(".")) return
        val fullPath =
            if (parentPath.endsWith(File.separator)) {
                parentPath + name
            } else {
                parentPath + File.separator + name
            }
        val file = File(fullPath)

        val isDir = (event and 0x40000000) != 0 || (file.exists() && file.isDirectory)

        if (isDir) {
            when (event and FileObserver.ALL_EVENTS) {
                FileObserver.CREATE,
                FileObserver.MOVED_TO -> {
                    watchDirectoryRecursively(file)
                }
                FileObserver.DELETE,
                FileObserver.MOVED_FROM -> {
                    observers.remove(fullPath)?.stopWatching()
                    scheduleDebounced(emptySet(), true)
                }
            }
            return
        }

        if (!isAudioFile(name)) return

        when (event and FileObserver.ALL_EVENTS) {
            FileObserver.CLOSE_WRITE,
            FileObserver.MOVED_TO,
            FileObserver.CREATE -> {
                scheduleDebounced(setOf(fullPath), false)
            }
            FileObserver.DELETE,
            FileObserver.MOVED_FROM -> {
                scheduleDebounced(emptySet(), true)
            }
        }
    }

    private fun scheduleDebounced(paths: Set<String>, hasDeletions: Boolean) {
        synchronized(pendingChangedPaths) {
            pendingChangedPaths.addAll(paths)
            if (hasDeletions) {
                pendingHasDeletions = true
            }
        }
        handler.removeCallbacks(debounceRunnable)
        handler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS)
    }

    private inner class SingleDirObserver(private val dirPath: String) :
        FileObserver(dirPath, CLOSE_WRITE or MOVED_TO or MOVED_FROM or DELETE or CREATE) {
        override fun onEvent(event: Int, path: String?) {
            handleEvent(dirPath, event, path)
        }
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 500L

        private val AUDIO_EXTENSIONS =
            setOf(
                "mp3",
                "flac",
                "ogg",
                "opus",
                "m4a",
                "aac",
                "wav",
                "wma",
                "aiff",
                "aif",
                "dsf",
                "dff",
                "oga",
            )

        fun isAudioFile(name: String): Boolean {
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in AUDIO_EXTENSIONS
        }
    }
}
