package me.tatarka.android.pngrs

import android.graphics.drawable.Animatable2.AnimationCallback
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import me.tatarka.android.apngrs.ApngDecoder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val png = ApngDecoder.decodeDrawable(ApngDecoder.createSource(resources, R.drawable.test))

        val startStopButton = findViewById<Button>(R.id.start_stop_button)

        png.registerAnimationCallback(object : AnimationCallback() {
            override fun onAnimationStart(drawable: Drawable?) {
                Log.d("apngrs", "Animation start")
            }

            override fun onAnimationEnd(drawable: Drawable?) {
                Log.d("apngrs", "Animation end")
                startStopButton.setText(R.string.start)
            }
        })

        png.start()

        findViewById<ImageView>(R.id.png_test)
            .setImageDrawable(png)

        startStopButton.setOnClickListener {
            if (png.isRunning) {
                png.stop()
                startStopButton.setText(R.string.start)
            } else {
                png.start()
                startStopButton.setText(R.string.stop)
            }
        }
    }
}