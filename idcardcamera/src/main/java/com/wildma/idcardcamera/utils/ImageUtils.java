package com.wildma.idcardcamera.utils;

import static com.wildma.idcardcamera.camera.CameraActivity.FILENAME;
import static com.wildma.idcardcamera.camera.CameraActivity.PHOTO_EXTENSION;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.wildma.idcardcamera.camera.CameraPreview;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ImageUtils {

    /**
     * 保存图片
     *
     * @param src      源图片
     * @param filePath 要保存到的文件路径
     * @param format   格式
     * @return {@code true}: 成功<br>{@code false}: 失败
     */
    public static boolean save(Bitmap src, String filePath, CompressFormat format) {
        return save(src, FileUtils.getFileByPath(filePath), format, false);
    }

    /**
     * 保存图片
     *
     * @param src    源图片
     * @param file   要保存到的文件
     * @param format 格式
     * @return {@code true}: 成功<br>{@code false}: 失败
     */
    public static boolean save(Bitmap src, File file, CompressFormat format) {
        return save(src, file, format, false);
    }

    /**
     * 保存图片
     *
     * @param src      源图片
     * @param filePath 要保存到的文件路径
     * @param format   格式
     * @param recycle  是否回收
     * @return {@code true}: 成功<br>{@code false}: 失败
     */
    public static boolean save(Bitmap src, String filePath, CompressFormat format, boolean recycle) {
        return save(src, FileUtils.getFileByPath(filePath), format, recycle);
    }

    /**
     * 保存图片
     *
     * @param src     源图片
     * @param file    要保存到的文件
     * @param format  格式
     * @param recycle 是否回收
     * @return {@code true}: 成功<br>{@code false}: 失败
     */
    public static boolean save(Bitmap src, File file, CompressFormat format, boolean recycle) {
        if (isEmptyBitmap(src) || !FileUtils.createOrExistsFile(file)) {
            return false;
        }
        System.out.println(src.getWidth() + ", " + src.getHeight());
        OutputStream os = null;
        boolean ret = false;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            ret = src.compress(format, 100, os);
            if (recycle && !src.isRecycled()) {
                src.recycle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeIO(os);
        }
        return ret;
    }

    /**
     * 判断bitmap对象是否为空
     *
     * @param src 源图片
     * @return {@code true}: 是<br>{@code false}: 否
     */
    private static boolean isEmptyBitmap(Bitmap src) {
        return src == null || src.getWidth() == 0 || src.getHeight() == 0;
    }

    /**
     * 将byte[]转换成Bitmap
     *
     * @param bytes
     * @param width
     * @param height
     * @return
     */
    public static Bitmap getBitmapFromByte(byte[] bytes, int width, int height) {
        final YuvImage image = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(bytes.length);
        if (!image.compressToJpeg(new Rect(0, 0, width, height), 100, os)) {
            return null;
        }
        byte[] tmp = os.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
        return bmp;
    }

    public static boolean saveBigImage(File file, Bitmap bitmap) {
        Log.d(CameraPreview.TAG, "图片地址::" + file.getAbsolutePath());
        boolean save = ImageUtils.save(bitmap, file.getAbsolutePath(), Bitmap.CompressFormat.JPEG);
        return save;
    }

    public static File createFile(File baseFolder, String format, String extension) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.US);
        return new File(baseFolder, simpleDateFormat.format(System.currentTimeMillis()) + extension);
    }

    public static File getOutputDirectory(Context context) {
        File[] externalMediaDirs = context.getExternalMediaDirs();
        if (externalMediaDirs != null && externalMediaDirs.length > 0) {
            File externalMediaDir = externalMediaDirs[0];
            Log.d(CameraPreview.TAG, "externalMediaDir= " + externalMediaDir.getAbsolutePath());
            File imageCacheDir = new File(externalMediaDir, "imagecache");
            if (!imageCacheDir.exists()) {
                imageCacheDir.mkdirs();
            }
            return imageCacheDir;
        } else {
            return context.getFilesDir();
        }
    }
}
