package com.example.blind;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    FruitDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        dbHelper = new FruitDBHelper(this);

        // 🔹 Get all DB entries
        List<FruitDBHelper.FruitItem> fruitList = dbHelper.getAllFruits();

        LinearLayout container = findViewById(R.id.container); // vertical LinearLayout

        for (FruitDBHelper.FruitItem item : fruitList) {

            // 🔹 Horizontal row
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            row.setLayoutParams(rowParams);

            // 🔹 Image
            ImageView imageView = new ImageView(this);
            File imgFile = new File(item.imagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
            }

            int sizeDp = 150;
            float scale = getResources().getDisplayMetrics().density;
            int sizePx = (int) (sizeDp * scale + 0.5f);

            LinearLayout.LayoutParams imageParams =
                    new LinearLayout.LayoutParams(sizePx, sizePx);
            imageParams.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // 🔹 Text
            TextView textView = new TextView(this);
            textView.setText(item.label);
            textView.setTextSize(18);

            // 🔹 Add views
            row.addView(imageView);
            row.addView(textView);

            container.addView(row);
        }
    }

    public void back(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}