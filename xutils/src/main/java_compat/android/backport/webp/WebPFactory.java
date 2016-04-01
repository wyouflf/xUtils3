package android.backport.webp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * Factory to encode and decode WebP images into Android Bitmap
 *
 * @author Alexey Pelykh
 */
@SuppressWarnings("JniMissingFunction")
public final class WebPFactory {

    private static boolean loadSoLibError = false;

    static {
        // 部分4.x的设备仍然不能很好的兼容webp, 需要借助libwebp.
        try {
            System.loadLibrary("webpbackport");
        } catch (Throwable ex) {
            loadSoLibError = true;
        }
    }

    private WebPFactory() {
    }

    public static boolean available() {
        return !loadSoLibError;
    }

    /**
     * Decodes byte array to bitmap
     *
     * @param data    Byte array with WebP bitmap data
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    public static Bitmap decodeByteArray(byte[] data, BitmapFactory.Options options) {
        if (available()) {
            return nativeDecodeByteArray(data, options);
        } else {
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }
    }

    /**
     * Decodes file to bitmap
     *
     * @param path    WebP file path
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    public static Bitmap decodeFile(String path, BitmapFactory.Options options) {
        if (available()) {
            return nativeDecodeFile(path, options);
        } else {
            return BitmapFactory.decodeFile(path, options);
        }
    }

    /**
     * Encodes bitmap into byte array
     *
     * @param bitmap  Bitmap
     * @param quality Quality, should be between 0 and 100
     * @return Encoded byte array
     */
    public static byte[] encodeBitmap(Bitmap bitmap, int quality) {
        if (available()) {
            return nativeEncodeBitmap(bitmap, quality);
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out);
            return out.toByteArray();
        }
    }

    /**
     * Decodes byte array to bitmap
     *
     * @param data    Byte array with WebP bitmap data
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    private static native Bitmap nativeDecodeByteArray(byte[] data, BitmapFactory.Options options);

    /**
     * Decodes file to bitmap
     *
     * @param path    WebP file path
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    private static native Bitmap nativeDecodeFile(String path, BitmapFactory.Options options);

    /**
     * Encodes bitmap into byte array
     *
     * @param bitmap  Bitmap
     * @param quality Quality, should be between 0 and 100
     * @return Encoded byte array
     */
    private static native byte[] nativeEncodeBitmap(Bitmap bitmap, int quality);
}
