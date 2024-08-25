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
import android.util.Log
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit


/**
 * [Drawable] for drawing animated PNGs.
 */
class ApngDrawable internal constructor(
    private val decoder: ApngDecoder,
    private val bitmap: Bitmap,
    private val initialDelay: Int,
) : Drawable(), Animatable2 {

    private var paint: Paint = Paint()
    private val drawFilter: DrawFilter =
        PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val animationCallbacks = CopyOnWriteArrayList<Animatable2.AnimationCallback>()

    private val animationHandler = Handler(Looper.getMainLooper())
    private var animationRunning = false
    private val animationRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.nanoTime()
            if (animationRunning) {
                try {
                    val delay = decoder.readNextFrame(bitmap)
                    invalidateSelf()
                    val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - currentTime)
                    animationHandler.postDelayed(this, delay - elapsed)
                } catch (e: IOException) {
                    Log.d("apngrs", "failed to decode frame", e)
                    stop()
                }
            }
        }
    }

    init {
        paint.isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        canvas.drawFilter = drawFilter
        canvas.drawBitmap(bitmap, null, bounds, paint)
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
        for (callback in animationCallbacks) {
            callback.onAnimationStart(this)
        }
        animationRunning = true
        animationHandler.postDelayed(animationRunnable, initialDelay.toLong())
    }

    override fun stop() {
        for (callback in animationCallbacks) {
            callback.onAnimationEnd(this)
        }
        animationRunning = false
        animationHandler.removeCallbacks(animationRunnable)
    }

    override fun isRunning(): Boolean {
        return animationRunning
    }

    override fun registerAnimationCallback(callback: Animatable2.AnimationCallback) {
        animationCallbacks.add(callback)
    }

    override fun unregisterAnimationCallback(callback: Animatable2.AnimationCallback): Boolean {
        return animationCallbacks.remove(callback)
    }

    override fun clearAnimationCallbacks() {
        animationCallbacks.clear()
    }
}