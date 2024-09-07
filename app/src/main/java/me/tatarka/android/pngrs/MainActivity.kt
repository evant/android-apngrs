package me.tatarka.android.pngrs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import coil.load
import me.tatarka.android.apngrs.ApngDecoder
import me.tatarka.android.apngrs.ApngDrawable
import me.tatarka.android.apngrs.coil.ApngDecoderDecoder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startStopButton: Button = findViewById(R.id.start_stop_button)
        val list: RecyclerView = findViewById(R.id.list)
        val adapter = ImageAdapter(this)
        adapter.submitList(
            listOf(
                ImageSource.Res(R.drawable.test),
                ImageSource.Res(R.drawable.test_2),
                ImageSource.Coil(R.drawable.test_3),
                ImageSource.Coil(R.drawable.test_4),
            )
        )
        list.adapter = adapter

        startStopButton.setOnClickListener {
            if (adapter.animating) {
                adapter.animating = false
                startStopButton.setText(R.string.start)
            } else {
                adapter.animating = true
                startStopButton.setText(R.string.stop)
            }
        }
    }
}

sealed class ImageSource {
    @get:DrawableRes
    abstract val id: Int

    data class Res(@DrawableRes override val id: Int) : ImageSource()
    data class Coil(@DrawableRes override val id: Int) : ImageSource()
}

class ImageAdapter(private val context: Context) : ListAdapter<ImageSource, ImageAdapter.Holder>(
    object : DiffUtil.ItemCallback<ImageSource>() {
        override fun areItemsTheSame(oldItem: ImageSource, newItem: ImageSource): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ImageSource, newItem: ImageSource): Boolean {
            return oldItem == newItem
        }
    }
) {

    private val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ApngDecoderDecoder.Factory())
        }
        .build()

    var animating: Boolean = true
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, Payload.Animating)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (Payload.Animating in payloads) {
            holder.updateAnimating()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class Holder(parent: ViewGroup) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
    ) {
        private val imageSize = itemView.context.resources.getDimensionPixelSize(R.dimen.image_size)
        private val image: ImageView = itemView.findViewById(R.id.image)
        private var drawable: ApngDrawable? = null

        fun bind(source: ImageSource) {
            when (source) {
                is ImageSource.Coil -> {
                    image.load(
                        data = source.id,
                        imageLoader = imageLoader,
                    ) {
                        listener { _, result ->
                            drawable = result.drawable as ApngDrawable
                            if (!animating) {
                                drawable?.stop()
                            }
                        }
                    }
                }

                is ImageSource.Res -> {
                    val png = ApngDecoder.decodeDrawable(
                        ApngDecoder.createSource(itemView.context.resources, source.id)
                    ) { decoder, info, _ ->
                        if (imageSize < info.size.width || imageSize < info.size.height) {
                            decoder.setTargetSize(imageSize, imageSize)
                        }
                    }
                    if (animating) {
                        png.start()
                    }
                    drawable = png
                    image.setImageDrawable(png)
                }
            }
        }

        fun updateAnimating() {
            if (animating) {
                drawable?.start()
            } else {
                drawable?.stop()
            }
        }
    }

    private enum class Payload {
        Animating
    }
}