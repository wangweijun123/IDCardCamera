package com.wildma.idcardcamera.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.wildma.idcardcamera.R;
import com.wildma.idcardcamera.cropper.CropImageView;
import com.wildma.idcardcamera.cropper.CropListener;
import com.wildma.idcardcamera.utils.CommonUtils;
import com.wildma.idcardcamera.utils.FileUtils;
import com.wildma.idcardcamera.utils.ImageUtils;
import com.wildma.idcardcamera.utils.PermissionUtils;
import com.wildma.idcardcamera.utils.ScreenUtils;

import java.io.File;


/**
 * Author       wildma
 * Github       https://github.com/wildma
 * Date         2018/6/24
 * Desc	        ${拍照界面}
 */
public class CameraPortraitActivity extends Activity implements View.OnClickListener {

    private CameraPreview mCameraPreview;
    private ImageView     mIvCameraCrop;
    private Bitmap        mCropBitmap;

    private View leftMock;
    private View headerMock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*动态请求需要的权限*/
        boolean checkPermissionFirst = PermissionUtils.checkPermissionFirst(this, IDCardCamera.PERMISSION_CODE_FIRST,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
        if (checkPermissionFirst) {
            init();
        }
    }


    private void init() {
        setContentView(R.layout.activity_camera_portrait);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        initView();
        initListener();
    }

    private void initView() {
        mCameraPreview = (CameraPreview) findViewById(R.id.camera_preview);
        mIvCameraCrop = (ImageView) findViewById(R.id.iv_camera_crop);
        leftMock = findViewById(R.id.leftMock);
        headerMock = findViewById(R.id.headerMock);
        /*增加0.5秒过渡界面，解决个别手机首次申请权限导致预览界面启动慢的问题*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraPreview.setVisibility(View.VISIBLE);
                    }
                });
            }
        }, 500);
    }

    private void initListener() {
        findViewById(R.id.iv_camera_take).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.camera_preview) {
            mCameraPreview.focus();
        } else if (id == R.id.iv_camera_close) {
            finish();
        } else if (id == R.id.iv_camera_take) {
            if (!CommonUtils.isFastClick()) {
                takePhoto();
            }
        } else {

        }

    }

    /**
     * 拍照
     */
    private void takePhoto() {
        mCameraPreview.setEnabled(false);
        CameraUtils.getCamera().setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] bytes, Camera camera) {
                final Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小 height:1440 width:3200

                camera.stopPreview();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final int w = size.width;
                        final int h = size.height;
                        Log.d(CameraPreview.TAG, "获取预览大小 W=" +w + ", h="+h);

                        Bitmap bigBitmap = ImageUtils.getBitmapFromByte(bytes, w, h);
                        Log.d(CameraPreview.TAG, "拍照返回的预览的字节数组生成bitmap，这是一张大的图片");
                        boolean success = ImageUtils.saveBigImage(getApplicationContext(), bigBitmap);
                        Log.d(CameraPreview.TAG, "保存big bitmap success ? " + success);
                        cropImage(bigBitmap);
                    }
                }).start();
            }
        });
    }

    /**
     * 裁剪图片
     */
    private void cropImage(Bitmap bitmap) {
        Log.d(CameraPreview.TAG, "大图大小 width=" + bitmap.getWidth() + ", height=" + bitmap.getHeight());
        Bitmap bitmapRotate = rotateImage(bitmap, 90);
        Log.d(CameraPreview.TAG, "旋转后大图大小 width=" + bitmapRotate.getWidth() + ", height=" + bitmapRotate.getHeight());

        int previewWidth = mCameraPreview.getWidth();
        int previewHeight = mCameraPreview.getHeight();
        Log.d(CameraPreview.TAG,"预览大小 previewWidth="+previewWidth+", previewHeight="+previewHeight);

        // 扫描框区域位置
        int cropLeft = leftMock.getWidth();
        int cropTop = headerMock.getHeight();
        int cropRight = cropLeft + mIvCameraCrop.getWidth();
        int cropBottom = cropTop + mIvCameraCrop.getHeight();
        Log.d(CameraPreview.TAG,"裁剪区域位置 cropLeft="+cropLeft+", cropRight="+cropRight+
                ", cropTop="+cropTop+", cropBottom="+cropBottom);

        float leftProportion = cropLeft / (float)previewWidth;
        float topProportion = cropTop / (float)previewHeight;
        float rightProportion = cropRight / (float)previewWidth;
        float bottomProportion = cropBottom / (float)previewHeight;
        Log.d(CameraPreview.TAG, "计算扫描框坐标点占原图坐标点的比例 leftProportion:"+leftProportion
                +", topProportion:"+topProportion+", rightProportion:"
                +rightProportion+", bottomProportion:"+bottomProportion);

        int x = (int)(leftProportion * bitmapRotate.getWidth());
        int y = (int)(topProportion * bitmapRotate.getHeight());

        int cropWidth = (int) ((rightProportion - leftProportion) * bitmapRotate.getWidth());
        int cropHeight = (int) ((bottomProportion - topProportion) * bitmapRotate.getHeight());
        Log.d(CameraPreview.TAG,"x="+x+", y="+y+", cropWidth="+cropWidth+", cropHeight="+cropHeight);
        // 不裁剪
        mCropBitmap = Bitmap.createBitmap(bitmapRotate,x,y,cropWidth,cropHeight);
        boolean success = ImageUtils.saveBigImage(getApplicationContext(), mCropBitmap);
        Log.d(CameraPreview.TAG, "保存裁剪后的图片 success ? " + success);
        // 设置成手动裁剪模式
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //将手动裁剪区域设置成与扫描框一样大

                mIvCameraCrop.setImageBitmap(mCropBitmap);
            }
        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        if (mCameraPreview != null) {
            mCameraPreview.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCameraPreview != null) {
            mCameraPreview.onStop();
        }
    }

    public static Bitmap rotateImage( Bitmap imageToOrient, int degreesToRotate) {
        Bitmap result = imageToOrient;

        try {
            if (degreesToRotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate((float)degreesToRotate);
                result = Bitmap.createBitmap(imageToOrient, 0, 0, imageToOrient.getWidth(), imageToOrient.getHeight(), matrix, true);
            }
        } catch (Exception var4) {
            Log.e("TransformationUtils", "Exception when trying to orient image", var4);
        }

        return result;
    }
}