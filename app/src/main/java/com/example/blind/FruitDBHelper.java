package com.example.blind;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

public class FruitDBHelper extends SQLiteOpenHelper {
    public class FruitItem {
        public String imagePath;
        public String label;

        public FruitItem(String imagePath, String label) {
            this.imagePath = imagePath;
            this.label = label;
        }
    }

    private static final String DATABASE_NAME = "FruitDB";
    private static final int DATABASE_VERSION = 1;

    public FruitDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE fruits (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "imagePath TEXT, " +
                "result TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS fruits");
        onCreate(db);
    }

    public void insertFruit(String imagePath, String result) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("imagePath", imagePath);
        values.put("result", result);
        db.insert("fruits", null, values);
        db.close();
    }

    public List<FruitItem> getAllFruits() {
        List<FruitItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT imagePath, result FROM fruits ORDER BY id DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String path = cursor.getString(0);
                String label = cursor.getString(1);
                list.add(new FruitItem(path, label));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }
}

