package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.airbnb.lottie.LottieAnimationView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import xyz.matteobattilana.library.WeatherView;


public class DisplayWeatherActivity extends AppCompatActivity {

    private LineChart chart;
    protected Typeface tfRegular;
    protected Typeface tfLight;

    private ImageButton notificationButton;
    private Boolean isNotificationEnabled;

    private WeatherLoader weatherLoader;
    private Forecast forecast;
    private SunriseSunset sunriseSunset;
    protected final Set<Integer> cloudsIds = new HashSet<>(Arrays.asList(2,3,4,6,7,8,11,12,13,14,15
            ,16,17,18,19,20,21,22,23,25,26,29,32,34,35,36,38,39,40,41,42,43,44));
    protected final Set<Integer> rainIds = new HashSet<>(Arrays.asList(12,13,14,18,29,39,40));
    protected final Set<Integer> snowIds = new HashSet<>(Arrays.asList(19,20,21,22,23,25,26,29,43,44));
    protected final Set<Integer> lightningIds = new HashSet<>(Arrays.asList(15,16,17,41,42));
    protected boolean clouds = false;
    protected boolean rain = false;
    protected boolean snow = false;
    protected boolean lightning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_weather);

        setWeather();
        setNotification();
        setDisplay();
    }

    private void setWeather() {
        weatherLoader = WeatherLoader.getInstance();
        forecast = weatherLoader.getForecast();
        sunriseSunset = weatherLoader.getSunriseSunset();
    }

    private void setDisplay() {
        setDisplayChart();
        setWeatherFeaturesOnActivity();
    }

    private void setWeatherFeaturesOnActivity(){
        extractCurrentWeather();
        setBackground(clouds);
        if (rain) { setRain(); }
        if (snow) { setSnow(); }
        if (lightning) { setLightning(); }
    }

    private void extractCurrentWeather(){
        int idIcon = 1; //TODO: get the icon id.
        if (cloudsIds.contains(idIcon)) { clouds = true; }
        if (rainIds.contains(idIcon)) { rain = true; }
        if (snowIds.contains(idIcon)) { snow = true; }
        if (lightningIds.contains(idIcon)) { lightning = true; }
    }

    private void setBackground(Boolean withClouds){
        View view = this.getWindow().getDecorView();
        int hourOfDay = 6; // TODO: get the hour, and set the cases according the type of the
        // variable of the hour.

        //assume it's early morning. all other cases are taken care of in switch statement:
        Drawable background = ContextCompat.getDrawable(this, R.drawable.midday);
        FrameLayout layout = (FrameLayout) findViewById(R.id.day_clouds);
        switch (hourOfDay) {
            case 1:
                background = ContextCompat.getDrawable(this, R.drawable.sunrise);
                layout = (FrameLayout)findViewById(R.id.foggy_clouds);
                break;
            case 2:
                background = ContextCompat.getDrawable(this, R.drawable.early_morning);
                layout = (FrameLayout)findViewById(R.id.morning_clouds);
                break;
            case 3:
                background = ContextCompat.getDrawable(this, R.drawable.midday_cloudy);
                layout = (FrameLayout)findViewById(R.id.foggy_clouds);
                break;
            case 4:
                background = ContextCompat.getDrawable(this, R.drawable.sunset);
                layout = (FrameLayout)findViewById(R.id.night_clouds);
                setNight();
                break;
            case 5:
                background = ContextCompat.getDrawable(this, R.drawable.night);
                layout = (FrameLayout)findViewById(R.id.night_clouds);
                setNight();
                break;
            default:
                break;
        }
        view.setBackground(background);
        if (withClouds) {
            layout.setVisibility(View.VISIBLE);
        }
    }

    private void setRain(){
        WeatherView mWeatherView = findViewById(R.id.rain);
        mWeatherView.setVisibility(View.VISIBLE);
        mWeatherView.startAnimation();
    }

    private void setSnow(){
        WeatherView mWeatherView = findViewById(R.id.snow);
        mWeatherView.setVisibility(View.VISIBLE);
        mWeatherView.startAnimation();
    }

    private void setLightning(){
        LottieAnimationView lightning = findViewById(R.id.lightning);
        lightning.setVisibility(View.VISIBLE);
    }

    private void setNight(){
        LottieAnimationView moon = findViewById(R.id.moon);
        LottieAnimationView sun = findViewById(R.id.sun);
        sun.setVisibility(View.INVISIBLE);
        moon.setVisibility(View.VISIBLE);
    }

    private void setDisplayChart() {
        setChart();
        setXAxis();
        setYAxis();
        setData();
        setChartAfterDataSet();
        setFont();
    }

    private void setChart() {
        chart = findViewById(R.id.chart);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.enableScroll();
        chart.setScaleEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);
        chart.setViewPortOffsets(35f, 5f, 35f, 50f);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setMaxHighlightDistance(100);

    }

    private void setXAxis() {
        XAxis x = chart.getXAxis();
        x.setTypeface(tfLight);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        x.setDrawGridLines(false);
        x.setLabelCount(6);

        x.setTextSize(12f);
        x.setTextColor(Color.WHITE);
        x.setCenterAxisLabels(false);
        x.setGranularity(0.25f); // one hour

        // TODO: fix it so the first value is not written on the x axis (the first value is the current time, the only time that is not a round hour)
        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int)value % 24) + ":00";
            }
        });
    }

    private void setYAxis() {
        YAxis y = chart.getAxisLeft();
        y.setTypeface(tfLight);
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setGranularityEnabled(true);
        y.setGranularity(0.2f);
        y.setDrawGridLines(false);
    }



    private void setFont() {
        tfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
        tfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
    }


    private ArrayList<Entry> createData() {

        Date dateTime = DateStringConverter.stringToDate(forecast.getDateTime());
        Calendar calender = Calendar.getInstance();
        calender.setTime(dateTime);

        int startHour = calender.get(Calendar.HOUR_OF_DAY);
        int startMinute = calender.get(Calendar.MINUTE);

        float startTime = startHour + startMinute / (float)60;

        ArrayList<Entry> values = new ArrayList<>();
        ArrayList<Double> forcast12Hours = forecast.getForeCast();

        values.add(new Entry(startTime, forecast.getCurrCondition().floatValue()));

        // update values array
        for (int hour = 0; hour < 12; ++hour) {
            Log.d("x value:", Float.toString(hour));
            values.add(new Entry(startHour + 1 + hour, forcast12Hours.get(hour).floatValue())); // add one entry per hour
        }
        return values;
    }


    private void setData() {
        ArrayList<Entry> values = createData();

        // create a dataset and give it a type
        LineDataSet chartData = new LineDataSet(values, "chart_data");

        chartData.setMode(LineDataSet.Mode.LINEAR);
        chartData.setLineWidth(1.75f);
        chartData.setCircleRadius(5f);
        chartData.setCircleHoleRadius(2.5f);
        chartData.setColor(Color.YELLOW);
        chartData.setCircleColor(Color.YELLOW);
        chartData.setHighLightColor(Color.WHITE);
        chartData.setDrawValues(false);

        // create a data object with the data sets
        LineData data = new LineData(chartData);
        data.setValueTypeface(tfLight);
        data.setValueTextSize(11f);
        data.setDrawValues(false);

        // set data
        chart.setData(data);
    }

    private void setChartAfterDataSet() {
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.setBorderWidth(0f);
        chart.animateX(2500);
        chart.getLegend().setEnabled(false);
        chart.setVisibleXRangeMaximum(8);
        chart.invalidate();
    }

    private void setNotificationButton() {
        notificationButton = findViewById(R.id.notification_button);

        if (isNotificationEnabled) {
            notificationButton.setImageResource(R.drawable.notification_on);
        }
        else {
            notificationButton.setImageResource(R.drawable.notification_off);
        }
    }

    public void enableDisableNotification(View view) {
        if (isNotificationEnabled) {
            FileManager.writeToFile("No\n", "shouldSetNotification.txt", this);
            isNotificationEnabled = false;
        }
        else {
            FileManager.writeToFile("Yes\n", "shouldSetNotification.txt", this);
            isNotificationEnabled = true;
        }
        setNotificationButton();
        setNotification();
    }

    private void setPickWeatherNotificationScheduler(long pickWeatherTimeMillis) {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PickWeatherNotificationScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, pickWeatherTimeMillis, pendingIntent);
    }

    private void cancelPickWeatherNotificationScheduler() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PickWeatherNotificationScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.cancel(pendingIntent);
    }

    private Boolean checkNotificationsEnabled() {
        String shouldSetNotification = FileManager.readFromFile(getApplicationContext(), "shouldSetNotification.txt");
        if (shouldSetNotification.equals("Yes\n") || shouldSetNotification.equals("")) {
            return true;
        }
        else {
            return false;
        }
    }

    private void setNotification() {

        cancelPickWeatherNotificationScheduler();

        isNotificationEnabled = checkNotificationsEnabled();
        if (isNotificationEnabled) {
            long pickWeatherTimeMillis = weatherLoader.getPickWeatherTimeMillis();
            setPickWeatherNotificationScheduler(pickWeatherTimeMillis);
        }
        setNotificationButton();
    }

    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }
}
