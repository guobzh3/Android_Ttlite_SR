package com.example.hello_java.analysis;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;

import com.example.hello_java.utils.ImageProcess;

import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.metadata.MetadataExtractor;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.common.ops.QuantizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class InferenceTFLite {
    private Interpreter tflite; // 解释器
    // option 理解为模型的一个配置；可以加nnapi代理加速、gpu加速、或者多线程等东西
    Interpreter.Options options = new Interpreter.Options();
    private String MODEL_FILE = "quicsr_float32_epoch_200.tflite"; // 使用float32的模型
    private Boolean IS_INT8 = false;
    private final Size INPNUT_SIZE = new Size(960, 540); // 输入写死了
    private final int[] OUTPUT_SIZE = new int[] {1, 1080, 1920, 3}; // 输出也写死了
    private long startTime;
    private long endTime;
    private long costTime;
    // 量化参数
    MetadataExtractor.QuantizationParams input5SINT8QuantParams = new MetadataExtractor.QuantizationParams(0.003921567928045988f, 0);
    MetadataExtractor.QuantizationParams output5SINT8QuantParams = new MetadataExtractor.QuantizationParams(0.003921568859368563f, 0);
    public void initialModel(Context activity) {
        try {
            // 要tflite 2.16.1 版本才支持 Transpose version 6操作
            // 读入到buffer中
            ByteBuffer tfliteModel = FileUtil.loadMappedFile(activity, MODEL_FILE);

            tflite = new Interpreter(tfliteModel, options); //

            Log.i("Debug tfliteSupport", "Success loading model");

        } catch (IOException e){
            Log.e("tflite Support", "Error loading model: ", e);
            Toast.makeText(activity, "load model error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    public int[] getOUTPUT_SIZE() {
        return OUTPUT_SIZE;
    }
    public int[] superResolution(Bitmap bitmap) {
        /**
         * 使用tensorimage 加载bitmap，然后使用imageProcessor 进行处理，最后将Bytebuffer传给 tensorBuffer
         * A typical use case of TensorImage is to first load a Bitmap image, then process it using ImageProcessor, and finally get the underlying ByteBuffer of the TensorBuffer and feed it into the TFLite interpreter.
         *
         * tensorImage 不拷贝数据
         * IMPORTANT: to achieve the best performance, TensorImage avoids copying data whenever it's possible. Therefore, it doesn't own its data. Callers should not modify data objects those are passed to load(Bitmap) or load(TensorBuffer, ColorSpaceType).
         */
        TensorImage modelInput; // 输入model 的tensorImage

        /**
         * 将一个tensorImage 转化成另外的一个tensorimage
         * ImageProcessor is a helper class for preprocessing and postprocessing TensorImage. It could transform a TensorImage to another by executing a chain of ImageOperator.
         * ImageProcessor.Builder: The Builder to create an ImageProcessor, which could be executed later.
         */
        ImageProcessor imageProcessor;

        if (IS_INT8) {
            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(INPNUT_SIZE.getHeight(), INPNUT_SIZE.getWidth(), ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(0, 255))
                    .add(new QuantizeOp(input5SINT8QuantParams.getZeroPoint(), input5SINT8QuantParams.getScale()))
                    .add(new CastOp(DataType.UINT8))
                    .build();
            modelInput = new TensorImage(DataType.UINT8);
        } else {
            imageProcessor = new ImageProcessor.Builder()
                                .add(new ResizeOp(INPNUT_SIZE.getHeight(), INPNUT_SIZE.getWidth(), ResizeOp.ResizeMethod.BILINEAR)) // 调整输入图片的size
                                .add(new NormalizeOp(0, 255)) // 归一化: (input - mean) / stddev.
                                .build();
            modelInput = new TensorImage(DataType.FLOAT32);
        }
        startTime = System.currentTimeMillis();
        modelInput.load(bitmap); // 将bitmap 载入到input中
        // 对输入做预处理，摄像头读取到的画面不一定正好是模型的输入，需要做缩放，并归一化到0，1
        modelInput = imageProcessor.process(modelInput);

        /** getTensorBuffer() :
         *      Returns a reference to a TensorBuffer which holds the image data
         *
         * tensorBuffer : Represents the data buffer for either a model's input or its output.
         */

        TensorBuffer hwcTensorBuffer = modelInput.getTensorBuffer(); // （感觉是内存的拷贝等工作） ?
//        Q: tensorBuffer 和 modelInput 的区别和联系是啥
        int[] shape = hwcTensorBuffer.getShape(); // 都是要声明一个数组来接住返回值
        // [h,w,c] = [1920, 1080, 3]
        for (int i = 0; i < shape.length; i++) {
            Log.i("TFLite input TensorBuffer shape", i + " " + shape[i]);
        }

// 创建输出的tensorBuffer，设定其SIZE 和 DATA_TYPE
        TensorBuffer hwcOutputTensorBuffer;
        if (IS_INT8) {
            hwcOutputTensorBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, DataType.UINT8);
        } else {
            hwcOutputTensorBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, DataType.FLOAT32);
        }

        endTime = System.currentTimeMillis();
        costTime = endTime - startTime;
        Log.i("TFLite pre process time:", Long.toString(costTime) + "ms");

        startTime = System.currentTimeMillis();
        if (tflite != null) {
            tflite.run(hwcTensorBuffer.getBuffer(), hwcOutputTensorBuffer.getBuffer());
        }
        endTime = System.currentTimeMillis();
        costTime = endTime - startTime;
        Log.i("TFLite inference time:", Long.toString(costTime) + "ms");

        // 如果是int8，需要对输出进行反量化
        if (IS_INT8) {
            TensorProcessor tensorProcessor = new TensorProcessor.Builder()
                    .add(new DequantizeOp(output5SINT8QuantParams.getZeroPoint(), output5SINT8QuantParams.getScale()))
                    .build();
            hwcOutputTensorBuffer = tensorProcessor.process(hwcOutputTensorBuffer);
        }

        startTime = System.currentTimeMillis();
        int[] outshape = hwcOutputTensorBuffer.getShape(); // 是一个数组
        // [b, h, w, c]
        int outHeight = outshape[1];
        int outWidth = outshape[2];
        for (int i = 0; i < outshape.length; i++) {
            // 都通过log.i 来进行打印
            Log.i("TFlite output TensorBuffer shape", i + " " + outshape[i]);
        }
        /**
         * 以浮点数的方式返回buffer中存储的数据；如果是int类型的，也会转换成float再返回
         * Returns a float array of the values stored in this buffer. If the buffer is of different types than float, the values will be converted into float. For example, values in TensorBufferUint8 will be converted from uint8 to float.
         */
        float[] hwcOutputData = hwcOutputTensorBuffer.getFloatArray();
        Log.i("array length" , String.valueOf(hwcOutputData.length));
        int[] pixels = new int[outHeight * outWidth];
        int yp = 0;
        for (int h = 0; h < outHeight; h++) {
            for (int w = 0; w < outWidth; w++) {
                int r = (int) (hwcOutputData[h * outWidth * 3 + w * 3] * 255);
                int g = (int) (hwcOutputData[h * outWidth * 3 + w * 3 + 1] * 255);
                int b = (int) (hwcOutputData[h * outWidth * 3 + w * 3 + 2] * 255);
                r = r > 255 ? 255 : (Math.max(r, 0));
                g = g > 255 ? 255 : (Math.max(g, 0));
                b = b > 255 ? 255 : (Math.max(b, 0));
                // int[] 是32位的，而像素是8位的，因此可以只用一个int[] 数组就能存下RGB了
                // A 是全1，就是完全不透明的
                pixels[yp++] = 0xff000000 | (r << 16 & 0xff0000) | (g << 8 & 0xff00) | (b & 0xff);
            }
        }
        endTime = System.currentTimeMillis();
        costTime = endTime - startTime;
        Log.i("TFLite after process time:", Long.toString(costTime) + "ms");

        return pixels;
    }

    public void addNNApiDelegate() {
        NnApiDelegate nnApiDelegate = null;
        // Initialize interpreter with NNAPI delegate for Android Pie or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            NnApiDelegate.Options nnApiOptions = new NnApiDelegate.Options();
//            nnApiOptions.setAllowFp16(true);
//            nnApiOptions.setUseNnapiCpu(true);
            //ANEURALNETWORKS_PREFER_LOW_POWER：倾向于以最大限度减少电池消耗的方式执行。这种设置适合经常执行的编译。
            //ANEURALNETWORKS_PREFER_FAST_SINGLE_ANSWER：倾向于尽快返回单个答案，即使这会耗费更多电量。这是默认值。
            //ANEURALNETWORKS_PREFER_SUSTAINED_SPEED：倾向于最大限度地提高连续帧的吞吐量，例如，在处理来自相机的连续帧时。
//            nnApiOptions.setExecutionPreference(NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED);
//            nnApiDelegate = new NnApiDelegate(nnApiOptions);
            nnApiDelegate = new NnApiDelegate(); // 创建一个nnapi代理
            options.addDelegate(nnApiDelegate); // 使用nnapi代理
            Log.i("Debug tfliteSupport", "using nnapi delegate.");
        }
        else {
            addThread(4);
        }
    }

    //    public void addGPUDelegate() {
//        CompatibilityList compatibilityList = new CompatibilityList();
//        if(compatibilityList.isDelegateSupportedOnThisDevice()){
//            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
//            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
//            options.addDelegate(gpuDelegate);
//            Log.i("Debug tfliteSupport", "using gpu delegate.");
//        } else {
//            addThread(4);
//        }
//    }

    public void addThread(int thread) {
        options.setNumThreads(thread); // 使用多线程
        Log.i("Debug tfliteSupport", "using addThread: " + thread);
    }
}


