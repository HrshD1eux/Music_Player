/*
 * Copyright (c) 2024 Music Player Project
 * WidgetBitmapTransformation.kt is part of Music Player.
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
 
package com.HrshD1eux.musicplayer.widgets

import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.sqrt

class WidgetBitmapTransformation(private val maxDimension: Int = 480) : Transformation() {
    private val maxBitmapArea = maxDimension * maxDimension

    override val cacheKey: String
        get() = "WidgetBitmapTransformation:$maxDimension"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (size !== Size.ORIGINAL) {
            throw IllegalArgumentException("WidgetBitmapTransformation requires original size.")
        }
        val inputArea = input.width * input.height
        if (inputArea > maxBitmapArea) {
            val scale = sqrt(maxBitmapArea.toDouble() / inputArea.toDouble())
            val newWidth = (input.width * scale).toInt().coerceAtLeast(1)
            val newHeight = (input.height * scale).toInt().coerceAtLeast(1)
            return input.scale(newWidth, newHeight)
        }
        return input
    }
}
