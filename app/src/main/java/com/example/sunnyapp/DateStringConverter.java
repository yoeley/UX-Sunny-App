package com.example.sunnyapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * server class to convert between strings and dates throughout the app
 */
public class DateStringConverter {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat accuWeatherDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * converts date to string in dateFormat.
     * @param date
     * @return
     */
    public static String dateToString(Date date) {
        String dateTime = dateFormat.format(date);
        return dateTime;
    }

    /**
     * converts string in either dateFormat (the apps preferred format) or accuWeatherDateFormat,
     * the date format of accuWeather, to Date.
     * @param string
     * @return
     */
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
