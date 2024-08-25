use std::io::Cursor;

use image::error::DecodingError;
use image::{AnimationDecoder, Frame, Frames, ImageDecoder, ImageError, Rgba};
use image::{EncodableLayout, Pixel};
use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::{jboolean, jint, jlong};
use jni::JNIEnv;
use ndk::bitmap::AndroidBitmap;

struct JNone;

macro_rules! unwrap_or_throw {
    ($env:ident, $result:expr) => {
        unwrap_or_throw!($env, $result, "java/lang/RuntimeException")
    };
    ($env:ident, $result:expr, $exception_type:literal) => {
        match $result {
            Ok(v) => v,
            Err(e) => {
                let exception = $env.find_class($exception_type).unwrap();
                $env.throw_new(exception, format!("{:?}", e)).unwrap();
                return JNone.into();
            }
        }
    };
}

impl From<JNone> for JObject<'_> {
    fn from(_value: JNone) -> Self {
        return JObject::null();
    }
}

impl From<JNone> for jboolean {
    fn from(_value: JNone) -> Self {
        return false as jboolean;
    }
}

impl From<JNone> for jint {
    fn from(_value: JNone) -> Self {
        return 0 as jint;
    }
}

#[no_mangle]
pub extern "system" fn Java_me_tatarka_android_apngrs_ApngDecoder_nCreate<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JByteArray<'local>,
) -> JObject<'local> {
    let data = unwrap_or_throw!(env, copy_jbyteArray_to_vec(&env, data));
    let decoder = Box::new(unwrap_or_throw!(
        env,
        NativeApngDecoder::decode(data),
        "java/io/IOException"
    ));
    let (width, height) = (decoder.width, decoder.height);
    let native_ptr = Box::into_raw(decoder);
    let png_decoder_class =
        unwrap_or_throw!(env, env.find_class("me/tatarka/android/apngrs/ApngDecoder"));
    unwrap_or_throw!(
        env,
        env.new_object(
            png_decoder_class,
            "(JII)V",
            &[
                (native_ptr as jlong).into(),
                (width as jint).into(),
                (height as jint).into(),
            ]
        )
    )
}

fn copy_jbyteArray_to_vec<'local>(
    env: &JNIEnv<'local>,
    array: JByteArray<'local>,
) -> jni::errors::Result<Vec<u8>> {
    let data_len = env.get_array_length(&array)?;
    let mut buff: Vec<i8> = Vec::with_capacity(data_len as usize);
    buff.resize(data_len as usize, 0);
    env.get_byte_array_region(array, 0, &mut *buff)?;
    // make sure buff's destructor doesn't free the data
    // it thinks it owns when it goes out of scope
    let mut buff = std::mem::ManuallyDrop::new(buff);
    let p = buff.as_mut_ptr();
    let len = buff.len();
    let cap = buff.capacity();
    Ok(unsafe { Vec::from_raw_parts(p as *mut u8, len, cap) })
}

#[no_mangle]
pub extern "system" fn Java_me_tatarka_android_apngrs_ApngDecoder_nClose<'local>(
    mut _env: JNIEnv<'local>,
    _class: JClass<'local>,
    ptr: jlong,
) {
    unsafe {
        drop(Box::from_raw(ptr as *mut NativeApngDecoder));
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_me_tatarka_android_apngrs_ApngDecoder_nNextFrame<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    decoder_ptr: jlong,
    bitmap: JObject<'local>,
    bitmap_size: jint,
) -> jint {
    let decoder: &mut NativeApngDecoder = unwrap_or_throw!(
        env,
        (decoder_ptr as *mut NativeApngDecoder)
            .as_mut()
            .ok_or("invalid pointer")
    );
    let bitmap = AndroidBitmap::from_jni(env.get_raw(), *bitmap);
    let bitmap_pixels = unwrap_or_throw!(env, bitmap.lock_pixels()) as *mut u8;
    let bitmap_slice = std::slice::from_raw_parts_mut(bitmap_pixels, bitmap_size as usize);
    let result = decoder.next_frame(bitmap_slice);
    unwrap_or_throw!(env, bitmap.unlock_pixels());
    unwrap_or_throw!(env, result, "java/io/IOException") as jint
}

struct NativeApngDecoder {
    width: u32,
    height: u32,
    current_frame: usize,
    frames: Frames<'static>,
    decoded_frames: Vec<Frame>,
}

impl NativeApngDecoder {
    fn decode(data: Vec<u8>) -> Result<NativeApngDecoder, ImageError> {
        let cursor = Cursor::new(data);
        let decoder = image::codecs::png::PngDecoder::new(cursor)?;
        let (width, height) = decoder.dimensions();
        let frames = decoder.apng()?.into_frames();
        Ok({
            NativeApngDecoder {
                width,
                height,
                current_frame: 0,
                frames,
                decoded_frames: vec![],
            }
        })
    }

    fn next_frame(&mut self, out: &mut [u8]) -> Result<u32, ImageError> {
        let frame = if let Some(next_frame) = self.frames.next() {
            let mut next_frame = next_frame?;
            // we need to premultiply for android to render correctly.
            for pixel in next_frame.buffer_mut().pixels_mut() {
                premultiply_alpha(pixel);
            }
            self.decoded_frames.push(next_frame);
            self.current_frame += 1;
            self.decoded_frames.last().unwrap()
        } else {
            if self.decoded_frames.is_empty() {
                return Err(ImageError::Decoding(DecodingError::new(
                    image::error::ImageFormatHint::Exact(image::ImageFormat::Png),
                    "missing frames, are you sure this is an APNG?",
                )));
            }
            self.current_frame = (self.current_frame + 1) % self.decoded_frames.len();
            self.decoded_frames.get(self.current_frame).unwrap()
        };
        let bytes = frame.buffer().as_bytes();
        if out.len() < bytes.len() {
            return Err(ImageError::IoError(std::io::Error::new(
                std::io::ErrorKind::Other,
                format!(
                    "provided output buffer is not large enough, expected: {} bytes but got {} bytes",
                    bytes.len(),
                    out.len()
                ),
            )));
        }
        out.copy_from_slice(frame.buffer().as_bytes());
        let (num, denom) = frame.delay().numer_denom_ms();
        Ok(num / denom)
    }
}

fn premultiply_alpha(pixel: &mut Rgba<u8>) {
    // assumes sRGB color space
    let alpha = pixel[3] as u16;
    pixel.apply_without_alpha(|c| {
        ((c as f32).powf(2.2) * (alpha as f32) / 255f32).powf(1f32 / 2.2f32) as u8
    })
}
