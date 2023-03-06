package me.tatarka.android.pngrs

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import me.tatarka.android.apngrs.ApngDecoder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val png = ApngDecoder.decodeDrawable(ApngDecoder.createSource(resources, R.drawable.test))
        png.start()

        findViewById<ImageView>(R.id.png_test)
            .setImageDrawable(png)
    }
}