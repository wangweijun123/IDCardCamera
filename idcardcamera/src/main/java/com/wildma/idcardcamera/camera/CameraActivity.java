package com.wildma.idcardcamera.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.wildma.idcardcamera.R;
import com.wildma.idcardcamera.utils.ImageUtils;
import com.wildma.idcardcamera.utils.PermissionUtils;

import java.io.File;

public class CameraActivity extends Activity implements View.OnClickListener {
    private static final int PERMISSION_CODE_FIRST = 1;
    public static final String FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS";
    public static final String PHOTO_EXTENSION = ".jpg";

    private static final String KEY_CAMERA_TITLE = "camera_title";
    private static final String KEY_CAMERA_SUBTITLE = "camera_subtitle";
    private static final String KEY_IMG_URI = "img_uri";
    private static final String KEY_CAMERA_FILE_TYPE = "camera_file_type";

    private String cameraTitle;
    private String cameraSubtitle;
    private String targetImgUri;
    private String cameraFileType;

    private CameraPreview mCameraPreview;
    private ImageView mIvCameraCrop;
    private Bitmap mCropBitmap;

    private View leftMock;
    private View headerMock;

    private TextView cameraTitleTV;
    private TextView cameraSubTitleTV;
    private boolean isToast = true;//是否弹吐司，为了保证for循环只弹一次



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*动态请求需要的权限*/
        boolean checkPermissionFirst = PermissionUtils.checkPermissionFirst(this, PERMISSION_CODE_FIRST,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
        if (checkPermissionFirst) {
            init();
        }
    }

    private void init() {
        setContentView(R.layout.activity_camera);
        initView();
        initListener();
    }

    private void initView() {
        mCameraPreview = (CameraPreview) findViewById(R.id.camera_preview);
        mIvCameraCrop = (ImageView) findViewById(R.id.cropIv);
        leftMock = findViewById(R.id.leftMock);

        headerMock = findViewById(R.id.headerMock);

        cameraTitleTV = findViewById(R.id.camera_title);
        cameraSubTitleTV = findViewById(R.id.camera_subtitle);

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
        mCameraPreview.setOnClickListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            cameraTitle = intent.getStringExtra(KEY_CAMERA_TITLE);
            cameraSubtitle = intent.getStringExtra(KEY_CAMERA_SUBTITLE);
            targetImgUri = intent.getStringExtra(KEY_IMG_URI);
            cameraFileType = intent.getStringExtra(KEY_CAMERA_FILE_TYPE);
            cameraTitleTV.setText(cameraTitle);
            cameraSubTitleTV.setText(cameraSubtitle);
            if ("1".equals(cameraFileType)) {
                mIvCameraCrop.setImageResource(R.drawable.camera_passport_front);
            } else {
                mIvCameraCrop.setImageResource(R.drawable.camera_idcard_front);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.camera_preview) {
            mCameraPreview.focus();
        } else if (id == R.id.iv_camera_take) {
            takePhoto();
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
                        Log.d(CameraPreview.TAG, "获取预览大小 W=" + w + ", h=" + h);

                        Bitmap bigBitmap = ImageUtils.getBitmapFromByte(bytes, w, h);
                        Log.d(CameraPreview.TAG, "拍照返回的预览的字节数组生成bitmap，这是一张大的图片");
                        final File bigFile = ImageUtils.createFile(ImageUtils.getOutputDirectory(getApplicationContext()),
                                FILENAME, PHOTO_EXTENSION);
                        boolean success = ImageUtils.saveBigImage(bigFile, bigBitmap);
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
        Log.d(CameraPreview.TAG, "预览大小 previewWidth=" + previewWidth + ", previewHeight=" + previewHeight);

        // 扫描框区域位置
        int cropLeft = leftMock.getWidth();
        int cropTop = headerMock.getHeight();
        int cropRight = cropLeft + mIvCameraCrop.getWidth();
        int cropBottom = cropTop + mIvCameraCrop.getHeight();
        Log.d(CameraPreview.TAG, "裁剪区域位置 cropLeft=" + cropLeft + ", cropRight=" + cropRight +
                ", cropTop=" + cropTop + ", cropBottom=" + cropBottom);

        float leftProportion = cropLeft / (float) previewWidth;
        float topProportion = cropTop / (float) previewHeight;
        float rightProportion = cropRight / (float) previewWidth;
        float bottomProportion = cropBottom / (float) previewHeight;
        Log.d(CameraPreview.TAG, "计算扫描框坐标点占原图坐标点的比例 leftProportion:" + leftProportion
                + ", topProportion:" + topProportion + ", rightProportion:"
                + rightProportion + ", bottomProportion:" + bottomProportion);

        int x = (int) (leftProportion * bitmapRotate.getWidth());
        int y = (int) (topProportion * bitmapRotate.getHeight());

        int cropWidth = (int) ((rightProportion - leftProportion) * bitmapRotate.getWidth());
        int cropHeight = (int) ((bottomProportion - topProportion) * bitmapRotate.getHeight());
        Log.d(CameraPreview.TAG, "x=" + x + ", y=" + y + ", cropWidth=" + cropWidth + ", cropHeight=" + cropHeight);
        // 不裁剪
        mCropBitmap = Bitmap.createBitmap(bitmapRotate, x, y, cropWidth, cropHeight);

        File cropfile = new File(targetImgUri);
        Log.d(CameraPreview.TAG, "传过来的file exists ? " + cropfile.exists());
        if (!cropfile.exists()) {
            cropfile = ImageUtils.createFile(ImageUtils.getOutputDirectory(getApplicationContext()),
                    FILENAME, PHOTO_EXTENSION);
        }
        boolean success = ImageUtils.saveBigImage(cropfile, mCropBitmap);
        Log.d(CameraPreview.TAG, "保存裁剪后的图片 success ? " + success);
        final File finalCropfile = cropfile;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIvCameraCrop.setImageBitmap(mCropBitmap);
                Intent intent = new Intent();
                intent.putExtra("image_path", finalCropfile.getAbsolutePath());
                setResult(Activity.RESULT_OK, intent);
                finish();
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

    public static Bitmap rotateImage(Bitmap imageToOrient, int degreesToRotate) {
        Bitmap result = imageToOrient;
        try {
            if (degreesToRotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate((float) degreesToRotate);
                result = Bitmap.createBitmap(imageToOrient, 0, 0, imageToOrient.getWidth(), imageToOrient.getHeight(), matrix, true);
            }
        } catch (Exception var4) {
            Log.e("TransformationUtils", "Exception when trying to orient image", var4);
        }

        return result;
    }

    /**
     * 处理请求权限的响应
     *
     * @param requestCode  请求码
     * @param permissions  权限数组
     * @param grantResults 请求权限结果数组
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isPermissions = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                isPermissions = false;
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) { //用户选择了"不再询问"
                    if (isToast) {
                        Toast.makeText(this, "请手动打开该应用需要的权限", Toast.LENGTH_SHORT).show();
                        isToast = false;
                    }
                }
            }
        }
        isToast = true;
        if (isPermissions) {
            Log.d("onRequestPermission", "onRequestPermissionsResult: " + "允许所有权限");
            init();
        } else {
            Log.d("onRequestPermission", "onRequestPermissionsResult: " + "有权限不允许");
            finish();
        }
    }

}