-dontskipnonpubliclibraryclasses
-verbose

-keepattributes *Annotation*

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}