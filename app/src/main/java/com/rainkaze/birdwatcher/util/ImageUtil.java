package com.rainkaze.birdwatcher.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ImageUtil {

    private static final String TAG = "ImageUtil";

    /**
     * 将给定的 Bitmap 缩放到最大尺寸限制内，并进行压缩。
     *
     * @param originalBitmap 原始 Bitmap.
     * @param maxDimension   图片的最长边允许的最大尺寸.
     * @param quality        JPEG 压缩质量 (0-100).
     * @return 处理后的 Bitmap.
     */
    public static Bitmap scaleAndCompressBitmap(Bitmap originalBitmap, int maxDimension, int quality) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            if (originalWidth > originalHeight) {
                newWidth = maxDimension;
                newHeight = (int) (originalHeight * ((float) maxDimension / originalWidth));
            } else {
                newHeight = maxDimension;
                newWidth = (int) (originalWidth * ((float) maxDimension / originalHeight));
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
            return scaledBitmap;
        }
        return originalBitmap;
    }

    /**
     * 从 URI 加载图片，转换为 Base64 编码字符串，并进行 URL Encode 以符合百度 API 要求。
     *
     * @param context  Context 对象.
     * @param imageUri 图片的 URI.
     * @return URL Encoded 的 Base64 图片字符串.
     * @throws IOException 如果文件读取或编码出错.
     */
    public static String uriToBase64UrlEncoded(Context context, Uri imageUri) throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                throw new FileNotFoundException("打开图片错误: " + imageUri);
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IOException("图片编码失败: " + imageUri);
            }

            bitmap = scaleAndCompressBitmap(bitmap, 4096, 85);

            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();

            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            return URLEncoder.encode(base64Image, "UTF-8");

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                }
            }
        }
    }


    /**
     * 从 URI 加载图片，转换为带MIME头的 Base64 编码字符串。
     *
     * @param context Context 对象.
     * @param imageUri 图片的 URI.
     * @return 带头的 Base64 图片字符串, 如果失败返回 null.
     */
    public static String uriToBase64WithHeader(Context context, Uri imageUri) {
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                throw new FileNotFoundException("引入图片错误:  " + imageUri);
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IOException("加载图片错误: " + imageUri);
            }

            bitmap = scaleAndCompressBitmap(bitmap, 1024, 80);

            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64Image;

        } catch (IOException e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}