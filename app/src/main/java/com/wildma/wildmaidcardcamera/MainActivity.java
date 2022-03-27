package com.wildma.wildmaidcardcamera;

import static com.wildma.idcardcamera.camera.CameraActivity.FILENAME;
import static com.wildma.idcardcamera.camera.CameraActivity.PHOTO_EXTENSION;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.wildma.idcardcamera.camera.CameraActivity;
import com.wildma.idcardcamera.camera.CameraPreview;
import com.wildma.idcardcamera.utils.ImageUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    public final static String IMAGE_PATH = "image_path";//图片路径标记

    private ImageView mIvFront;
    private ImageView mIvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvFront = (ImageView) findViewById(R.id.iv_front);
        mIvBack = (ImageView) findViewById(R.id.iv_back);
    }

    /**
     * 身份证正面
     */
    public void frontNew(View view) {
        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        intent.putExtra("camera_title", "this is title");
        intent.putExtra("camera_subtitle", "this is sub title");
        intent.putExtra("img_uri", "/storage/emulated/0/Android/media/com.wildma.wildmaidcardcamera/wangweijun/2000-03-27-16-11-59-111.jpg");
        intent.putExtra("camera_file_type", "2");
        startActivityForResult(intent, 1);
    }

    public void backNew(View view) {
        File cropfile = ImageUtils.createFile(ImageUtils.getOutputDirectory(getApplicationContext()),
                FILENAME, PHOTO_EXTENSION);
        if (!cropfile.exists()) {
            try {
                cropfile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(CameraPreview.TAG, "Main传过来的文件地址 exists?"+cropfile.exists() + " "+cropfile.getAbsolutePath());
        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        intent.putExtra("camera_title", "this is title");
        intent.putExtra("camera_subtitle", "this is sub title");
        intent.putExtra("img_uri", cropfile.getAbsolutePath());
        intent.putExtra("camera_file_type", "2");
        startActivityForResult(intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            //获取图片路径，显示图片
            if (data != null) {
                final String path = data.getStringExtra(IMAGE_PATH);
                if (!TextUtils.isEmpty(path)) {
                    mIvFront.setImageBitmap(BitmapFactory.decodeFile(path));
                }
            }
        }
    }
}
