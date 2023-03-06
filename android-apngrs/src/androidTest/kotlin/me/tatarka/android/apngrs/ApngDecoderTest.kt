package me.tatarka.android.apngrs

import androidx.test.platform.app.InstrumentationRegistry
import me.tatarka.android.apngrs.test.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ApngDecoderTest {
    @Test
    fun png_decoder_smoketest() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val drawable =
            ApngDecoder.decodeDrawable(ApngDecoder.createSource(context.resources, R.drawable.test))

        assertEquals(100, drawable.intrinsicWidth)
        assertEquals(100, drawable.intrinsicHeight)
    }
}