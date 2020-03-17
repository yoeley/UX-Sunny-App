package com.example.sunnyapp.future_possible_features;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.sunnyapp.WeatherLoader;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This files enables saving and loading data from the cache, does not check integrity.
 */


public class WeatherDataCache {

    private String fileName = "WeatherData";

    public void saveWeatherData(Context context, WeatherLoader fullWeatherData) {
        String fileName = "WeatherData";
        File directory;
        directory = context.getFilesDir();
        FileOutputStream fos = null;

        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(WeatherLoader.staticToString().getBytes());
            Toast.makeText(context, "Saved to " + directory + "/" + fileName, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("s");
            e.printStackTrace();
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public WeatherLoader loadWeatherData(Context context) {
        FileInputStream fis = null;
        WeatherLoader weatherLoaderData = null;

        try{
            fis = context.openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder sb = new StringBuilder();
            String data;

            while ((data = br.readLine()) != null){
                sb.append(data).append("\n");
            }

            JSONObject jsonData = new JSONObject(data);
            ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
            weatherLoaderData = mapper.convertValue(jsonData, WeatherLoader.class);
            Toast.makeText(context, "Loaded data into Object", Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return weatherLoaderData;
    }

}
