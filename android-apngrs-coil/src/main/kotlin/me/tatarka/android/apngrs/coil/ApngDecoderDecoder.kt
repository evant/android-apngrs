package me.tatarka.android.apngrs.coil

import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import coil.size.Size
import me.tatarka.android.apngrs.ApngDecoder
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

/**
 * A [Decoder] that uses [ApngDecoder] to decode APNGs. Note: Currently, this will _only_ apply to
 * PNG's that are animated. Use another decoder for static PNGs.
 */
class ApngDecoderDecoder(private val source: ImageSource, private val options: Options) : Decoder {

    override suspend fun decode(): DecodeResult {
        val source = source.use { ApngDecoder.createSource(it.source().readByteArray()) }
        val drawable = ApngDecoder.decodeDrawable(source) { decoder, info, _ ->
            if (options.size != Size.ORIGINAL) {
                val (width, height) = options.size
                if (width is Dimension.Pixels && height is Dimension.Pixels) {
                    if (!options.allowInexactSize ||
                        (width.px < info.size.width || height.px < info.size.height)
                    ) {
                        decoder.setTargetSize(width = width.px, height = height.px)
                    }
                } else if (width is Dimension.Pixels) {
                    // keep aspect ratio
                    if (!options.allowInexactSize || width.px < info.size.width) {
                        decoder.setTargetSize(
                            width = width.px,
                            height = (width.px * info.size.height / info.size.width)
                        )
                    }
                } else if (height is Dimension.Pixels) {
                    // keep aspect ratio
                    if (!options.allowInexactSize || height.px < info.size.height) {
                        decoder.setTargetSize(
                            width = (height.px * info.size.width / info.size.height),
                            height = height.px
                        )
                    }
                } else {
                    // keep source dimensions
                }
            }
        }
        return DecodeResult(
            drawable = drawable,
            isSampled = false
        )
    }

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            if (result.mimeType != null && result.mimeType != "image/png") {
                return null
            }
            return if (isApng(result.source.source())) {
                return ApngDecoderDecoder(result.source, options)
            } else {
                null
            }
        }
    }
}

private val PNG_HEADER = ByteString.of(
    0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
)

private val CHUNK_ID = okio.Options.of(
    "acTL".encodeUtf8(),
    "IDAT".encodeUtf8(),
)

private fun isApng(source: BufferedSource): Boolean {
    if (!source.rangeEquals(0, PNG_HEADER)) return false
    val source = source.peek()
    source.skip(8)
    while (!source.exhausted()) {
        val size = source.readInt()
        val chunkId = source.select(CHUNK_ID)
        if (chunkId == 0) {
            // found the acTL chuck, must be an APNG
            return true
        }
        if (chunkId == 1) {
            // found the IDAT chunk, acTL must come before this so it's not an APNG
            return false
        }
        // skip chunk (+4 chunkID +4 checksum)
        source.skip(size.toLong() + 8)
    }
    return false
}