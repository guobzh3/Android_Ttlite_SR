package com.example.hello_java;

// 开始自带的
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider; // 是Android CameraX库中的一个类，用于管理相机的生命周期和绑定相机用例。这是CameraX库提供的一个高级特性，使得相机的管理更加简单和高效，尤其是在处理Android应用的生命周期时。
/**
CameraX通过用例（UseCase）来管理相机的功能。常见的用例包括预览（Preview）、图像捕获（ImageCapture）和视频捕获（VideoCapture）等。
        ProcessCameraProvider负责将这些用例绑定到相机上，并确保它们与生命周期保持同步。
 **/
import androidx.camera.view.PreviewView; // 在应用中展示相机的基本功能
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// 自定义的相关推理工具
import com.example.hello_java.analysis.ImageAnalyse;
import com.example.hello_java.analysis.Inference;
import com.example.hello_java.analysis.InferenceInterpreter;
import com.example.hello_java.analysis.InferenceTFLite;
// 自定义的cameraProcess
import com.example.hello_java.utils.CameraProcess;

import com.google.common.util.concurrent.ListenableFuture;

import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
// 摄像头模组
//import androidx.camera.view.PreviewView;

public class MainActivity extends AppCompatActivity {

    private PreviewView cameraPreviewWrap; // 用于显示相机的实时预览，主要用于相机应用；主要与CameraX库一起使用
    private ImageView imageView; // 用于显示静态图像，可以加载本地资源或者网络图像；是标准Android视图组件，无需额外库
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView inferenceTimeTextView;
    private TextView frameSizeTextView;
    private CameraProcess cameraProcess = new CameraProcess();

    private boolean Is_Super_Resolution = false;
    private Switch immersive;
    private Inference srTFLiteInference;
    private InferenceInterpreter srTFLiteInterpreter;
    private InferenceTFLite srTFLite;

    public int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    private void initModel() {
        try {
//            this.srTFLiteInference = new Inference();
//            this.srTFLiteInference.initialModel(this);
//            this.srTFLiteInterpreter = new InferenceInterpreter();
//            this.srTFLiteInterpreter.addNNApiDelegate();
//            this.srTFLiteInterpreter.initialModel(this);
            this.srTFLite = new InferenceTFLite(); // 创建 InferenceTFLite 类
            this.srTFLite.addNNApiDelegate(); // 使用nnapi代理
            this.srTFLite.initialModel(this);
        } catch (Exception e) {
            Log.e("Error Exception", "MainActivity initial model error: " + e.getMessage() + e.toString());
        }
    }

    @Override // 重载OnCreate函数，其中 saveInstanceState 中是保存activity 状态的
    // Q： 视图和逻辑搞清楚
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // setContentView(R.layout.activity_main) 设置活动的布局文件为 activity_main.xml。
        // Q: R是什么
        // 打开app时候隐藏顶部状态栏
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // 全图画面 cameraPreviewWrap 是一个视图容器，用于显示相机预览画面——不需要进行裁剪
        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);

        // cameraProviderFuture 用于获取相机的提供者，它是 CameraX 库的一部分，用于管理摄像头的绑定和生命周期。
        cameraProviderFuture = ProcessCameraProvider.getInstance(this); //使用ProcessCameraProvider.getInstance(this)方法获取相机提供者的单例实例。这个方法返回一个ListenableFuture对象，确保相机提供者异步初始化完成。

        imageView = findViewById(R.id.box_label_canvas);

        //sr botton
        immersive = findViewById(R.id.immersive);

        //实时更新的 文本信息
        inferenceTimeTextView = findViewById(R.id.inference_time);
        frameSizeTextView = findViewById(R.id.frame_size);

        // 申请摄像头权限
        if (!cameraProcess.allPermissionsGranted(this)) {
            cameraProcess.requestPermissions(this);
        }

        // 获取手机摄像头拍照旋转参数
        // rotation = 0°
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i("image rotation", "rotation: " + rotation);

        initModel(); // 初始化模型

        // 创建分析器
        ImageAnalyse imageAnalyse = new ImageAnalyse(cameraPreviewWrap, imageView, srTFLiteInference, srTFLiteInterpreter, srTFLite,  inferenceTimeTextView, frameSizeTextView);

        //cameraProcess.showCameraSupportSize(MainActivity.this);
//    启动摄像头，同时分析器也传入这里（自动逐帧调用analyse函数）
        cameraProcess.startCamera(MainActivity.this, imageAnalyse, cameraPreviewWrap);

//        //监听botton
//        immersive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Is_Super_Resolution = isChecked;
//                ImageAnalyse imageAnalyse = new ImageAnalyse(imageView);
//
//                cameraProcess.startCamera(MainActivity.this, imageAnalyse, cameraPreviewWrap);
//
////                if (!isChecked) { // 普通模式，放一个摄像头预览画面
////                    cameraProcess.startCamera(MainActivity.this, cameraPreviewWrap);
////                }
//            }
//        });
    }
}