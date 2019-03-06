package com.affirm.android.utils;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

class RawJson {

    private Gson gson;

    public RawJson(@NonNull Gson gson) {
        this.gson = gson;
    }

    public <T> T readFrom(@RawRes int resId, Class<T> clazz, @NonNull Context ctx) {
        final InputStream inputStream = ctx.getResources().openRawResource(resId);

        try {
            final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer();

            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }

            reader.close();

            return readFrom(sb.toString(), clazz);
        } catch (IOException e) {
            return null;
        }
    }

    public <T> T readFrom(@NonNull String text, Class<T> clazz) {
        return gson.fromJson(text, clazz);
    }
}
