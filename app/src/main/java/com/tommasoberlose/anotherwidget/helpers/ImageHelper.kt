package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import kotlin.math.min
import kotlin.math.roundToInt

object ImageHelper {
    fun ImageView.applyShadow(originalView: ImageView, factor: Float = 1f) {
        clearColorFilter()
        val shadowPref = if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow
        if (shadowPref == 0) {
            setImageDrawable(null)
            return
        }

        val cElevation = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            when (shadowPref) {
                1 -> 8f * factor
                2 -> 16f * factor
                else -> 8f * factor
            },
            resources.displayMetrics
        )

        val drawable = originalView.drawable
        if (drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            val targetDensity = resources.displayMetrics.densityDpi
            val source = drawable.toBitmap().copy(Bitmap.Config.ARGB_8888, false).apply {
                density = Bitmap.DENSITY_NONE
            }
            val shadowBitmap = generateShadowBitmap(context, cElevation, source, factor, shadowPref)
            if (shadowBitmap != null) {
                val combined = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).apply {
                    density = Bitmap.DENSITY_NONE
                }
                val canvas = Canvas(combined).apply {
                    density = Bitmap.DENSITY_NONE
                }
                val yOffset = (source.height * 0.025f).coerceAtLeast(1f)
                canvas.drawBitmap(shadowBitmap, 0f, yOffset, null)
                shadowBitmap.recycle()
                combined.density = targetDensity
                setImageBitmap(combined)
            } else {
                setImageDrawable(null)
            }
            source.recycle()
        } else {
            setImageDrawable(null)
        }
    }

    private fun generateShadowBitmap(
        context: Context,
        cElevation: Float,
        bitmap: Bitmap?,
        factor: Float,
        shadowPref: Int
    ): Bitmap? {
        bitmap ?: return null

        val opacity = when (shadowPref) {
            0 -> 0f
            1 -> 0.8f * factor
            2 -> 1f * factor
            else -> 0.8f * factor
        }.coerceIn(0f, 1f)

        if (opacity <= 0f) return null

        val shadow = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).apply {
            density = Bitmap.DENSITY_NONE
        }
        val blurRadius = getBlurRadius(context, cElevation)
        val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (blurRadius > 0f) {
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
        }
        val offset = IntArray(2)
        val alphaMask = bitmap.extractAlpha(blurPaint, offset).apply {
            density = Bitmap.DENSITY_NONE
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (255f * opacity).roundToInt().coerceIn(0, 255)
        }

        Canvas(shadow).apply {
            density = Bitmap.DENSITY_NONE
        }.drawBitmap(
            alphaMask,
            offset[0].toFloat(),
            offset[1].toFloat(),
            shadowPaint
        )
        alphaMask.recycle()
        return shadow
    }

    private fun getBlurRadius(context: Context, customElevation: Float): Float {
        val maxElevation = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24f,
            context.resources.displayMetrics
        )
        return min(25f * (customElevation / maxElevation), 25f)
    }
}
