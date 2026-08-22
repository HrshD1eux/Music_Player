/*
 * Copyright (c) 2022 Music Player Project
 * PlaybackBottomSheetBehavior.kt is part of Music Player.
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
 
package com.HrshD1eux.musicplayer.playback

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.R as MR
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.HrshD1eux.musicplayer.R
import com.HrshD1eux.musicplayer.ui.BaseBottomSheetBehavior
import com.HrshD1eux.musicplayer.ui.UISettings
import com.HrshD1eux.musicplayer.util.getAttrColorCompat
import com.HrshD1eux.musicplayer.util.getDimenPixels
import com.HrshD1eux.musicplayer.util.replaceSystemBarInsetsCompat
import com.HrshD1eux.musicplayer.util.systemBarInsetsCompat

/**
 * The [BaseBottomSheetBehavior] for the playback bottom sheet. This bottom sheet
 *
 * @author HrshD1eux
 */
class PlaybackBottomSheetBehavior<V : View>(context: Context, attributeSet: AttributeSet?) :
    BaseBottomSheetBehavior<V>(context, attributeSet) {
    lateinit var sheetBackgroundDrawable: MaterialShapeDrawable

    fun makeBackgroundDrawable(context: Context) {
        sheetBackgroundDrawable =
            MaterialShapeDrawable.createWithElevationOverlay(context).apply {
                fillColor = context.getAttrColorCompat(MR.attr.colorSurfaceContainerLow)
                shapeAppearanceModel =
                    if (uiSettings.roundMode) {
                        ShapeAppearanceModel.builder(
                                context,
                                R.style.ShapeAppearance_MusicPlayer_BottomSheet,
                                MR.style.ShapeAppearanceOverlay_Material3_Corner_Top,
                            )
                            .build()
                    } else {
                        ShapeAppearanceModel.Builder().build()
                    }
            }
    }

    init {
        isHideable = true
    }

    override fun getIdealBarHeight(context: Context) =
        context.getDimenPixels(R.dimen.size_touchable_large)

    // Hack around issue where the playback sheet will try to intercept nested scrolling events
    // before the queue sheet.
    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent) =
        super.onInterceptTouchEvent(parent, child, event) && state != STATE_EXPANDED

    // Note: This is an extension to Auxio's vendored BottomSheetBehavior
    override fun isHideableWhenDragging() = false

    override fun createBackground(context: Context, uiSettings: UISettings) =
        LayerDrawable(
            arrayOf(
                // Add another colored background so that there is always an obscuring
                // element even as the actual "background" element is faded out.
                MaterialShapeDrawable(sheetBackgroundDrawable.shapeAppearanceModel).apply {
                    fillColor = sheetBackgroundDrawable.fillColor
                },
                sheetBackgroundDrawable,
            )
        )

    override fun applyWindowInsets(child: View, insets: WindowInsets): WindowInsets {
        super.applyWindowInsets(child, insets)
        // Offset our expanded panel by the size of the playback bar, as that is shown when
        // we slide up the panel.
        val bars = insets.systemBarInsetsCompat
        expandedOffset = bars.top
        return insets.replaceSystemBarInsetsCompat(
            bars.left,
            bars.top,
            bars.right,
            expandedOffset + bars.bottom,
        )
    }
}
