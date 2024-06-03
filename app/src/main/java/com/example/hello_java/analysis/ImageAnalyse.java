package com.example.hello_java.analysis;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import android.util.Log;
import android.widget.TextView;

import com.example.hello_java.utils.ImageProcess;

// 继承 ImageAnalysis.Analyzer 类，并重写 analyse 方法：对摄像头传入的每帧进行处理
public class ImageAnalyse implements ImageAnalysis.Analyzer{
    ImageView imageView;
    ImageProcess imageProcess;
    PreviewView previewView;
    Inference srTFLiteInference;
    InferenceInterpreter srTFLiteInterpreter;
    InferenceTFLite srTFLite;
    TextView inferenceTimeTextView;
    TextView frameSizeTextView;
    // 初始胡相关的变量
    public ImageAnalyse(PreviewView previewView,
                        ImageView imageView,
                        Inference srTFLiteInference,
                        InferenceInterpreter srTFLiteInterpreter,
                        InferenceTFLite srTFLite,
                        TextView inferenceTimeTextView,
                        TextView frameSizeTextView) {
        this.imageView = imageView;
        this.previewView = previewView;
        this.srTFLiteInference = srTFLiteInference;
        this.srTFLiteInterpreter = srTFLiteInterpreter;
        this.srTFLite = srTFLite;
        this.inferenceTimeTextView = inferenceTimeTextView;
        this.frameSizeTextView = frameSizeTextView;
        this.imageProcess = new ImageProcess();
    }
     // 重载这个方法，基于这个方法进行推理
    @Override
    public void analyze(@NonNull ImageProxy image) {
        long startTime = System.currentTimeMillis();

        int previewHeight = previewView.getHeight();
        System.out.println("previewHeight" + (previewHeight));
        int previewWidth = previewView.getWidth();
// 对接收到的数据进行处理：从YUV转成RGB
        byte[][] yuvBytes = new byte[3][]; // 这是个二维数组
        ImageProxy.PlaneProxy[] planes = image.getPlanes(); // 获取低层图像的所有平面，每个平面都包含一个imageProxy.planceProxy 对象
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();

        // (480, 640)
        // 这里的height, weight 是 竖直拿手机的绝对视图对应的height, weight
        // 在CameraProcess 里setTargetResolution(new Size(1080, 1920)) 设置的分辨率是摄像头出来的(w,h)
        // 直接放到ImageView中显示会旋转90°
        Log.i("ImageProxy image size & Preview size:", imageHeight + " " + imageWidth + " & " + previewHeight + " " + previewWidth);
//        int rotationDegrees = image.getImageInfo().getRotationDegrees();
//        Log.i("image rotation2", "rotation: " + rotationDegrees);

        // 将 planes 中的数据拷贝到yuvBytes中
        imageProcess.fillBytes(planes, yuvBytes);

        int yRowStride = planes[0].getRowStride(); // Y通道一行数据所占的字节数目，由于可能有填充，大小接近weight
        final int uvRowStride = planes[1].getRowStride(); //U通道一行数据所占的字节数，由于可能有填充，大小接近weight/2
        final int uvPixelStride = planes[1].getPixelStride(); //U通道相邻两个像素之间的距离，为1表示紧密排序

        // 这里用int做存储，int是4个字节，所以表示的是ARGB四个属性了，每个属性只占一个字节
        int[] rgbBytes = new int[imageHeight * imageWidth];
        // 将yuv数据转化成RGB数据
        imageProcess.YUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                imageWidth,
                imageHeight,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes
        );
        // 暂时不理解上面的转化了，后面需要再说
        // 原图bitmap，转成Bitmap格式方便在Android上进行处理和显示图像（位图的格式）
        // 首先创建了一个这样的bitmap
        Bitmap imageBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        // 将对应的rgbBytes填充到bitmap 中 ？？？
        /**
         * setPixels
         * Added in API level 1
         * void setPixels (int[] pixels,  // 设置像素数组，对应点的像素被放在数组中的对应位置，像素的argb值全包含在该位置中
         *                 int offset,    // 设置偏移量，我们截图的位置就靠此参数的设置
         *                 int stride,    // 设置一行打多少像素，通常一行设置为bitmap的宽度，
         *                 int x,         // 设置开始绘图的x坐标
         *                 int y,         // 设置开始绘图的y坐标
         *                 int width,     // 设置绘制出图片的宽度
         *                 int height)    // 设置绘制出图片的高度
         */
        // (x ,y ) = (0,0) 对应的是画布左上角的坐标
        imageBitmap.setPixels(rgbBytes, 0, imageWidth, 0, 0, imageWidth, imageHeight);

        // 变化矩阵
        // 旋转图像，Analyse 出来的每一帧图像都是默认相机传感器方向（横向的），在竖屏模式下图像会自动旋转90度，需要手动调整
//        Matrix postTransformMatrix = imageProcess.getTransformationMatrix(
//                imageWidth, imageHeight,
//                targetWidth, targetHeight,
//                90,
//                false
//        );
        //Matrix matrix = new Matrix();
//        matrix.postRotate(90);
//        Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, matrix, false);
//        // 应用该变化矩阵
//        Bitmap postTransformImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, postTransformMatrix, false);
//        // 裁剪大小
//        Bitmap cropImageBitmap = Bitmap.createBitmap(postTransformImageBitmap, 0, 0, previewWidth, previewHeight);


//        srTFLiteInference.superResolution(cropImageBitmap);
        //int[] pixels = srTFLiteInference.superResolution(imageBitmap);
//        int[] pixels = srTFLiteInterpreter.superResolution(imageBitmap);
        // 对模型进行推理（输入的是整理后的bitmap ）
        //
        int[] pixels = srTFLite.superResolution(imageBitmap);

        int[] outputSize = srTFLite.getOUTPUT_SIZE();
        int outWidth = outputSize[2];
        int outHeight = outputSize[1];

        // 又新建了一个新的bitmap，来存储输出结果
        Bitmap outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        outBitmap.setPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight);

        // 旋转90°（matrix 是啥 ）
        Matrix matrix = new Matrix();
        matrix.postRotate(90); // 创建一个旋转90度的矩阵
        // 将该矩阵应用于bitmap上，获得旋转90度之后的bitmap
        Bitmap postTransformImageBitmap = Bitmap.createBitmap(outBitmap, 0, 0, outWidth, outHeight, matrix, false);

        // 裁剪到适配输出画布 （只保留到preViewWidth 和 PreviewHeight 的部分）
        Bitmap cropImageBitmap = Bitmap.createBitmap(postTransformImageBitmap, 0, 0, previewWidth, previewHeight);

        // 设置为imageView 的背景
        imageView.setImageBitmap(cropImageBitmap);
        image.close();
        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;
        inferenceTimeTextView.setText(Long.toString(costTime) + "ms");
        frameSizeTextView.setText(outWidth + "x" + outHeight);
    }
}
