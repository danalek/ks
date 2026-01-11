package com.example.blind;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.tensorflow.lite.Interpreter;

import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    static final int REQUEST_IMAGE_CAPTURE = 1;
    ImageView imageView;
    TextView label1;
    Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        label1 = findViewById(R.id.label1);
        imageView = findViewById(R.id.imageView);

        try {
            AssetFileDescriptor fileDescriptor = getAssets().openFd("best-fp16.tflite");


        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        tflite = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));
        }catch (Exception  e){}
    }

    public void openCamera(View view) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            new Thread(() -> {
                try {
                    float[][][][] input = new float[1][640][640][3];
                    for (int y = 0; y < 640; y++) {
                        for (int x = 0; x < 640; x++) {
                            int pixel = imageBitmap.getPixel(x, y);
                            input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f;
                            input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;
                            input[0][y][x][2] = (pixel & 0xFF) / 255.0f;
                        }
                    }

                    // Check output shape before running
                    int[] outShape = tflite.getOutputTensor(0).shape();
                    float[][][] output = new float[outShape[0]][outShape[1]][outShape[2]]; // adjust shape
                    tflite.run(input, output);

                    for (int j = 0; j < output.length; j++)
                    {
                        float[] max_confidence = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
                        for (int i = 0; i < output[j].length; i++) {
                            if (output[j][i][4] > max_confidence[4]) {
                                max_confidence = output[j][i];
                            }
                        }

                        float max_val = 0;
                        int classId = -1;
                        StringBuilder resultText = new StringBuilder();

                        for (int prob = 5; prob < outShape[2]; prob++)
                        {
                            if (prob != 5)
                                resultText.append("\n");
                            if (max_val < max_confidence[prob])
                            {
                                max_val = max_confidence[prob];
                                classId = prob - 5;
                            }
                        }

                        float[] cords = new float[]{
                                max_confidence[0] * 640 - (max_confidence[2] * 320),
                                max_confidence[1] * 640 - (max_confidence[3] * 320),
                                max_confidence[0] * 640 + (max_confidence[2] * 320),
                                max_confidence[1] * 640 + (max_confidence[3] * 320)
                        };
                        String image_label="";
                        switch(classId){
                            case 0:
                                image_label="banana";
                                break;
                            case 1:
                                image_label="jackfruit";
                                break;
                            case 2:
                                image_label="mango";
                                break;
                            case 3:
                                image_label="litchi";
                                break;
                            case 4:
                                image_label="hogplum";
                                break;
                            case 5:
                                image_label="papaya";
                                break;
                            case 6:
                                image_label="grapes";
                                break;
                            case 7:
                                image_label="apple";
                                break;
                            case 8:
                                image_label="orange";
                                break;
                            case 9:
                                image_label="guava";
                                break;
                            case 10:
                                image_label="melon";
                                break;
                        }
                        float confidence = max_confidence[4];

                        Bitmap mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);

                        Paint rectPaint = new Paint();
                        rectPaint.setColor(Color.RED);
                        rectPaint.setStyle(Paint.Style.STROKE);
                        rectPaint.setStrokeWidth(6);

                        canvas.drawRect(cords[0], cords[1], cords[2], cords[3], rectPaint);

                        Paint textPaint = new Paint();
                        textPaint.setColor(Color.WHITE);
                        textPaint.setTextSize(48);
                        textPaint.setStyle(Paint.Style.FILL);
                        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                        Paint bgPaint = new Paint();
                        bgPaint.setColor(Color.RED);
                        bgPaint.setStyle(Paint.Style.FILL);

                        Rect textBounds = new Rect();
                        textPaint.getTextBounds(image_label, 0, image_label.length(), textBounds);

                        float textX = cords[0];
                        float textY = Math.max(cords[1] - 10, textBounds.height() + 10);

                        canvas.drawRect(
                                textX,
                                textY - textBounds.height() - 10,
                                textX + textBounds.width() + 20,
                                textY + 10,
                                bgPaint
                        );

                        canvas.drawText(image_label, textX + 10, textY, textPaint);

                        String outputa=image_label+" - "+confidence;
                        runOnUiThread(() -> {
                            label1.setText(outputa);
                            imageView.setImageBitmap(mutableBitmap);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> label1.setText("Error: " + e.getMessage()));
                }
            }).start();
        }
    }
}