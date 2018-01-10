package com.mybigday.rnmediameta;

import android.content.Context;
import android.graphics.Bitmap;
import wseemann.media.FFmpegMediaMetadataRetriever;
import android.util.Base64;
import android.graphics.Matrix;

import com.facebook.react.bridge.Arguments;
// import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
// import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;


public class RNMediaMeta extends ReactContextBaseJavaModule {
  private Context context;

  public RNMediaMeta(ReactApplicationContext reactContext) {
    super(reactContext);

    this.context = (Context) reactContext;
  }

  @Override
  public String getName() {
    return "RNMediaMeta";
  }

  // Related to https://github.com/wseemann/FFmpegMediaMetadataRetriever/blob/master/gradle/fmmr-library/library/src/main/java/wseemann/media/FFmpegMediaMetadataRetriever.java#L632
  private final String[] metadatas = {
    "album",
    "album_artist",
    "comment",
    "copyright",
    "creation_time",
    "disc",
    "encoder",
    "encoded_by",
    "genre",
    "language",
    "performer",
    "publisher",
    "service_name",
    "service_provider",
    "track",
    "variant_bitrate",
    "icy_metadata",
    "framerate",
    "chapter_start_time",
    "chapter_end_time",
    "artist",
    "composer",
    "title",
    "date",
    "duration",
    "rotation"
  };

  private String convertToBase64(byte[] bytes) {
    return Base64.encodeToString(bytes, Base64.NO_WRAP);
  }

  private byte[] convertToBytes(Bitmap bmp) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bmp.compress(Bitmap.CompressFormat.PNG, 75, stream);
    return stream.toByteArray();
  }

  private void putString(WritableMap map, String key, String value) {
    if (value != null) map.putString(key, value);
  }

  private void getMetadata(String path, ReadableMap options, Promise promise) {
    File f = new File(path);
    if (!f.exists() || f.isDirectory()) {
      promise.reject("-15", "file not found");
      return;
    }

    FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
    WritableMap result = Arguments.createMap();
    try {
      mmr.setDataSource(path);

      // check is media
      String audioCodec = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC);
      String videoCodec = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_CODEC);

      if (audioCodec == null && videoCodec == null) {
        promise.resolve(result);
        mmr.release();
        return;
      }

      // get all metadata - TODO :: Loop through only values for audio or video metadata based on media type for a small performance boost
      for (String meta: metadatas) {
        putString(result, meta, mmr.extractMetadata(meta));
      }

      if (result.hasKey("framerate") && !result.hasKey("rotation")) {
        putString(result, "rotation", mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
      }

      // Legacy support & camelCase
      // Only get the createTime if it exists.
      // Otherwise an exception is thrown and thumb is never generated
      if (result.hasKey("createTime")) {
        result.putString("createTime", result.getString("creation_time"));
      }

      if (options.getBoolean("getThumb")) {
        // get thumb
        Bitmap bmp = mmr.getFrameAtTime();
        if (bmp != null) {
          // Bitmap bmp2 = mmr.getFrameAtTime((long) 4E6, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
          // if (bmp2 != null) bmp = bmp2;

          /*
          * The image returned seems to be always in landscape mode and does not follow
          * the rotation of the video.
          * get the rotation from the metadata and apply the correction so the image is straight.
          */

          if (result.hasKey("rotation")) {
            Bitmap rotatedBmp = RotateBitmap(bmp, Float.parseFloat(result.getString("rotation")));
            if (rotatedBmp != null) {
              bmp = rotatedBmp;
            }
          }

          byte[] bytes = convertToBytes(bmp);
          result.putInt("width", bmp.getWidth());
          result.putInt("height", bmp.getHeight());
          result.putString("thumb", convertToBase64(bytes));
        }
      }

    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      promise.resolve(result);
      mmr.release();
    }
  }

  private Bitmap RotateBitmap(Bitmap source, float angle)
  {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }


  @ReactMethod
  public void get(final String path, final ReadableMap options, final Promise promise) {

    new Thread() {
      @Override
      public void run() {
        getMetadata(path, options, promise);
      }
    }.start();
  }
}
