package com.wildma.wildmaidcardcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import com.wildma.idcardcamera.camera.CameraPortraitActivity;
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
        Intent intent = new Intent(getApplicationContext(), CameraPortraitActivity.class);
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
