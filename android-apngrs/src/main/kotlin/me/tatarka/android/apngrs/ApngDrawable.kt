package me.tatarka.android.apngrs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.DrawFilter
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList


/**
 * [Drawable] for drawing animated PNGs.
 */
class ApngDrawable internal constructor(
    private val decoder: ApngDecoder,
    private val bitmap: Bitmap,
) : Drawable(), Animatable2 {

    private var paint: Paint = Paint()
    private val drawFilter: DrawFilter =
        PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var animationCallbacks: CopyOnWriteArrayList<Animatable2.AnimationCallback>? = null
    private val animationRunnable = Runnable { invalidateSelf() }
    private var starting = false
    private var animationRunning = false
    private val callbackHandler = Handler(Looper.getMainLooper())

    init {
        paint.isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        val frameOption: Int
        if (starting) {
            frameOption = FRAME_RESET
            starting = false
            postOnAnimationStart()
        } else if (isRunning) {
            frameOption = FRAME_ADVANCE
        } else {
            frameOption = FRAME_STAY
        }
        try {
            val delay = decoder.draw(bitmap, frameOption = frameOption)
            canvas.drawFilter = drawFilter
            canvas.drawBitmap(bitmap, null, bounds, paint)
            if (delay == -1) {
                stop()
            } else {
                scheduleSelf(animationRunnable, delay + SystemClock.uptimeMillis())
            }
        } catch (e: IOException) {
            Log.d("apngrs", "failed to decode frame", e)
            stop()
        }
    }

    private fun postOnAnimationStart() {
        animationCallbacks?.let {
            callbackHandler.post {
                for (callback in it) {
                    callback.onAnimationStart(this)
                }
            }
        }
    }

    private fun postOnAnimationEnd() {
        animationCallbacks?.let {
            callbackHandler.post {
                for (callback in it) {
                    callback.onAnimationEnd(this)
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getAlpha(): Int {
        return paint.alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return decoder.width
    }

    override fun getIntrinsicHeight(): Int {
        return decoder.height
    }

    override fun start() {
        if (!animationRunning) {
            starting = true
            animationRunning = true
            invalidateSelf()
        }
    }

    override fun stop() {
        animationRunning = false
        unscheduleSelf(animationRunnable)
        postOnAnimationEnd()
    }

    override fun isRunning(): Boolean {
        return animationRunning
    }

    override fun registerAnimationCallback(callback: Animatable2.AnimationCallback) {
        val callbacks = animationCallbacks
            ?: CopyOnWriteArrayList<Animatable2.AnimationCallback>().also {
                animationCallbacks = it
            }
        if (callback !in callbacks) {
            callbacks.add(callback)
        }
    }

    override fun unregisterAnimationCallback(callback: Animatable2.AnimationCallback): Boolean {
        val callbacks = animationCallbacks ?: return false
        if (!callbacks.remove(callback)) return false
        if (callbacks.isEmpty()) {
            animationCallbacks = null
        }
        return true
    }

    override fun clearAnimationCallbacks() {
        animationCallbacks = null
    }
}