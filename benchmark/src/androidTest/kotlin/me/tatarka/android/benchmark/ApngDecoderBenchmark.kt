package me.tatarka.android.benchmark

import android.graphics.ImageDecoder
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.tatarka.android.benchmark.test.R
import me.tatarka.android.apngrs.ApngDecoder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApngDecoderBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun first_frame_system_png_decoder() {
        val source = ImageDecoder.createSource(context.resources, R.drawable.test)
        benchmarkRule.measureRepeated {
            ImageDecoder.decodeDrawable(source)
        }

    }

    @Test
    fun first_frame_apng_decoder() {
        val source = ApngDecoder.createSource(context.resources, R.drawable.test)
        benchmarkRule.measureRepeated {
            ApngDecoder.decodeDrawable(source)
        }
    }
}