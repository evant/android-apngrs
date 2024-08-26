package me.tatarka.android.apngrs

import android.content.res.Resources
import android.graphics.Bitmap
import me.tatarka.android.apngrs.ApngDecoder.Companion.decodeDrawable
import me.tatarka.android.apngrs.ApngDecoder.Source
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal const val FRAME_STAY = 0
internal const val FRAME_ADVANCE = 1
internal const val FRAME_RESET = 2

/**
 * A class for converting animated PNGs (APNG) into [android.graphics.drawable.Drawable] objects.
 *
 * TO use it, first create a [Source] using on of the createSource overloads and pass the result to
 * [decodeDrawable].
 *
 * This will return an [ApngDrawable] which can be animated. To start its animation,
 * call [ApngDrawable.start]:
 *
 * ```kotlin
 * val drawable = ApngDecoder.decodeDrawable(source)
 * drawable.start()
 * ```
 */
class ApngDecoder private constructor(
    private var nativePtr: Long,
    internal val width: Int,
    internal val height: Int,
) {

    private val closed = AtomicBoolean()

    abstract class Source internal constructor() {
        internal open val resources: Resources? = null
        internal abstract fun createPngDecoder(): ApngDecoder
    }

    companion object {
        init {
            System.loadLibrary("android_apngrs")
        }

        /**
         * Create a new [Source] from a byte array.
         *
         * Note: Decoding will continue while the [ApngDrawable] is being animated, so the contents
         * passed here must not be modified even after the drawable is returned.
         *
         * @param data byte array of compressed image data.
         * @return a new Source object, which can be passed to [decodeDrawable].
         */
        @JvmStatic
        fun createSource(data: ByteArray): Source {
            return ByteArraySource(data)
        }

        /**
         * Create a new [Source] form a resource. Warning: if you place your APNG in res/drawable
         * you must turn off png crunching or it will be optimized to a static PNG.
         *
         * @param res the [Resources] object containing the image data.
         * @param resId resource ID of the image Data.
         * @return a new Source object, which can be passed to [decodeDrawable].
         */
        @JvmStatic
        fun createSource(res: Resources, resId: Int): Source {
            return ResourceSource(res, resId)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun decodeDrawable(source: Source): ApngDrawable {
            val decoder = source.createPngDecoder()
            val bitmap =
                Bitmap.createBitmap(decoder.width, decoder.height, Bitmap.Config.ARGB_8888)
            return ApngDrawable(decoder, bitmap)
        }

        @JvmStatic
        private external fun nCreate(data: ByteArray): ApngDecoder

        @JvmStatic
        private external fun nDraw(
            nativePtr: Long,
            bitmap: Bitmap,
            bitmapSize: Int,
            frameOption: Int,
        ): Int

        @JvmStatic
        private external fun nClose(nativePtr: Long)
    }

    internal fun draw(bitmap: Bitmap, frameOption: Int): Int {
        //TODO: figure out if we should loop or not
        return nDraw(nativePtr, bitmap, bitmap.byteCount, frameOption)
    }

    private class ByteArraySource(private val data: ByteArray) : Source() {
        override fun createPngDecoder(): ApngDecoder {
            return nCreate(data)
        }
    }

    private class ResourceSource(override val resources: Resources, private val resId: Int) :
        Source() {

        override fun createPngDecoder(): ApngDecoder {
            val out = ByteArrayOutputStream()
            resources.openRawResource(resId).use { it.copyTo(out) }
            return nCreate(out.toByteArray())
        }
    }

    protected fun finalize() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        nClose(nativePtr)
        nativePtr = 0
    }
}