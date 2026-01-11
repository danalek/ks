package com.example.blind;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FruitClassifier {

    private Interpreter tflite;
    private String[] labels = {"banana", "jackfruit", "mango", "litchi", "hogplum", "papaya", "grapes", "apple", "orange", "guava", "melon"};
    private final int INPUT_SIZE = 640;
    private final int PIXEL_SIZE = 3;
    private final float IMAGE_STD = 255.0f;

    public FruitClassifier(Context context) {
        try {
            tflite = new Interpreter(loadModelFile(context, "best-fp16.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String classifyImage(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        float[][][][] input = new float[1][640][640][3];
        for (int y = 0; y < 640; y++) {
            for (int x = 0; x < 640; x++) {
                int pixel = resized.getPixel(x, y);
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f;
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;
            }
        }

        int[] outShape = tflite.getOutputTensor(0).shape();
        float[][][] output = new float[outShape[0]][outShape[1]][outShape[2]]; // adjust shape
        tflite.run(input, output);

        float[] max_conf = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int j = 0; j < output.length; j++) {
            for (int i = 0; i < output[j].length; i++) {
                if (output[j][i][4] > max_conf[4]) {
                    max_conf = output[j][i];
                }
            }
        }

        float max_val = 0;
        int maxIndex = -1;
        for (int prob = 5; prob < outShape[2]; prob++) {
            if (max_val < max_conf[prob]) {
                max_val = max_conf[prob];
                maxIndex = prob - 5;
            }
        }

        if (maxIndex >= labels.length) return "Wrong " + maxIndex;
        if (maxIndex == -1) return "";
        return labels[maxIndex];
//        return "";
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            byteBuffer.putFloat(r / IMAGE_STD);
            byteBuffer.putFloat(g / IMAGE_STD);
            byteBuffer.putFloat(b / IMAGE_STD);
        }

        return byteBuffer;
    }

    public void close() {
        tflite.close();
    }

    // Utility method to load TFLite model from assets
    private ByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(context.getAssets().openFd(modelPath).getFileDescriptor())) {
            java.nio.channels.FileChannel fileChannel = fis.getChannel();
            long startOffset = context.getAssets().openFd(modelPath).getStartOffset();
            long declaredLength = context.getAssets().openFd(modelPath).getLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }
}

