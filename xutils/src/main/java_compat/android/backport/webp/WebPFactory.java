package android.backport.webp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Factory to encode and decode WebP images into Android Bitmap
 *
 * @author Alexey Pelykh
 */
@SuppressWarnings("JniMissingFunction")
public final class WebPFactory {
    static {
        // 部分4.x的设备仍然不能很好的兼容webp, 需要借助libwebp.
        System.loadLibrary("webpbackport");
    }

    /**
     * Decodes byte array to bitmap
     *
     * @param data    Byte array with WebP bitmap data
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    public static native Bitmap nativeDecodeByteArray(byte[] data, BitmapFactory.Options options);

    /**
     * Decodes file to bitmap
     *
     * @param path    WebP file path
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    public static native Bitmap nativeDecodeFile(String path, BitmapFactory.Options options);

    /**
     * Encodes bitmap into byte array
     *
     * @param bitmap  Bitmap
     * @param quality Quality, should be between 0 and 100
     * @return Encoded byte array
     */
    public static native byte[] nativeEncodeBitmap(Bitmap bitmap, int quality);
}
