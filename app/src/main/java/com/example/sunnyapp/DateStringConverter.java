package com.example.sunnyapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateStringConverter {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat accuWeatherDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static String dateToString(Date date) {
        String dateTime = dateFormat.format(date);
        return dateTime;
    }

    public static Date stringToDate(String string) {
        Date date = null;
        try {
            date = dateFormat.parse(string);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            date = accuWeatherDateFormat.parse(string);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}
