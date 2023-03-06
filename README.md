[![CircleCI](https://circleci.com/gh/evant/android-apngrs.svg?style=svg&circle-token=8792fa19911be92d6a1d66dd45ece3bf6712f778)](https://circleci.com/gh/evant/android-apngrs)[![Maven
Central](https://img.shields.io/maven-central/v/me.tatarka.android/android-apngrs.svg)](https://search.maven.org/search?q=g:me.tatarka.android)
[![Sonatype Snapshot](https://img.shields.io/nexus/s/https/oss.sonatype.org/me.tatarka.android/android-apngrs.svg)](https://oss.sonatype.org/content/repositories/snapshots/me/tatarka/android/)

# android-apngrs

Bindings to [image-rs](https://github.com/image-rs/image) for APNG support on Android.

# Usage

You can include the decoder with

```kotlin
implementation("me.tatarka.android:anpngrs:0.1")
```

Then you can create a source and decode a drawable. The api is mirrored after
[ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder).

```kotlin
val source = ApngDecoder.source(resources, R.drawable.my_animated_png)
val drawable = ApngDecoder.decodeDrawable(source)
drawable.start() // to start the animation.
```

## Coil Integration

For easy [coil](https://coil-kt.github.io/coil/) integration, include

```kotlin
implementation("me.tatarka.android:anpngrs-coil:0.1")
```

and then add the decoder

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(ApngDecoderDecoder.Factory())
    }
    .build()
```

## Current Limitations/Possible Future Directions

- All animations are currently rendered as looping infinitely instead of respecting the num_plays
  value.
- All pixel data for all frames is loaded into memory, no scaling is done on this buffer. This
  library was designed with a focus on small images so this is fine but may be worth optimizing in
  the future for better memory usage on large images.
- Image data is loaded first into a byte array in memory before decoding, could do some sort of
  streaming support here if I could figure out the jni bits for that.
- image-rs actually supports a wide range of image formats but only APNGs are currently supported.
  May be expanded in the future if there's value.
- Source inputs are limited to byte arrays and resources, this could be expanded pretty easily.
