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
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import xyz.matteobattilana.library.WeatherView;

/**
 * displays the weather quality to the user, through both visual imagery and an informative graph
 */
public class DisplayWeatherActivity extends AppCompatActivity {

    private LineChart chart;
    protected Typeface tfRegular;
    protected Typeface tfLight;
    private TextView breakTimeText;

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

    private final static long ONE_HOUR = 3600000; // in millis
    private final static String BREAK_TIME_TEXT_FORMAT = "Take a break at: %s:%s";

    /**
     * sets up the activity on it's creation
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_weather);

        if (!getIntent().getExtras().getBoolean("isFirstDisplay")) {
            moveTaskToBack(true);
        }

        setWeather();
        setNotification();
        setDisplay();
    }

    /**
     * retrieves all weather-related data and prepares the app to use it
     */
    private void setWeather() {
        weatherLoader = WeatherLoader.getInstance();
        forecast = weatherLoader.getForecast();
        sunriseSunset = weatherLoader.getSunriseSunset();
    }

    /**
     * sets the elements on the screen for display
     */
    private void setDisplay() {
        setDisplayChart();
        setWeatherFeaturesOnActivity();
    }

    /**
     * sets the display to match the current weather
     */
    private void setWeatherFeaturesOnActivity(){
        extractCurrentWeather();
        setBackground(clouds);
        if (rain) { setRain(); }
        if (snow) { setSnow(); }
        if (lightning) { setLightning(); }
    }

    /**
     * get the current state of weather
     */
    private void extractCurrentWeather(){
        int idIcon = forecast.getWeatherIcon();

        if (cloudsIds.contains(idIcon)) { clouds = true; }
        if (rainIds.contains(idIcon)) { rain = true; }
        if (snowIds.contains(idIcon)) { snow = true; }
        if (lightningIds.contains(idIcon)) { lightning = true; }
    }

    /**
     * gets the time period of the day
     * @return
     */
    public int getTimeOfDayState() {
        long sunrise = DateStringConverter.stringToDate(sunriseSunset.getSunrise()).getTime();
        long sunset = DateStringConverter.stringToDate(sunriseSunset.getSunset()).getTime();
        long currTime = DateStringConverter.stringToDate(forecast.getDateTime()).getTime();

        if (sunrise - currTime > (ONE_HOUR * 0.5)) {
            return 4; // night
        }
        if (Math.abs(sunrise - currTime) < (ONE_HOUR * 0.5)) {
            return 1; // sunrise time
        }
        if (Math.abs(sunset - currTime) < (ONE_HOUR * 0.5)) {
            return 3; // sunset time
        }
        if (currTime - sunset > (ONE_HOUR * 0.5)) {
            return 4; // night
        }
        return 2; // midday
    }

    /**
     * sets the display's background to match the time of the day
     * @param withClouds
     */
    private void setBackground(Boolean withClouds){
        View view = this.getWindow().getDecorView();
        int timeOfDay = getTimeOfDayState();
        // variable of the hour.

        //assume it's early morning. all other cases are taken care of in switch statement:
        Drawable background = null;
        FrameLayout layout = null;

        switch (timeOfDay) {
            case 1:
                background = ContextCompat.getDrawable(this, R.drawable.sunrise);
                layout = (FrameLayout)findViewById(R.id.foggy_clouds);
                break;
            case 2:
                if (clouds == true) {
                    background = ContextCompat.getDrawable(this, R.drawable.midday);
                    layout = (FrameLayout) findViewById(R.id.day_clouds);
                }
                else {
                    background = ContextCompat.getDrawable(this, R.drawable.midday_cloudy);
                    layout = (FrameLayout) findViewById(R.id.foggy_clouds);
                }
                break;
            case 3:
                background = ContextCompat.getDrawable(this, R.drawable.sunset);
                layout = (FrameLayout)findViewById(R.id.night_clouds);
                setNight();
                break;
            case 4:
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

    /**
     * sets the display to show rain
     */
    private void setRain(){
        WeatherView weatherView = findViewById(R.id.rain);
        weatherView.setVisibility(View.VISIBLE);
        weatherView.startAnimation();
    }

    /**
     * sets the display to show snow
     */
    private void setSnow(){
        WeatherView weatherView = findViewById(R.id.snow);
        weatherView.setVisibility(View.VISIBLE);
        weatherView.startAnimation();
    }

    /**
     * sets the display to show lightning
     */
    private void setLightning(){
        LottieAnimationView lightning = findViewById(R.id.lightning);
        lightning.setVisibility(View.VISIBLE);
    }

    /**
     * sets the display to show night time
     */
    private void setNight(){
        LottieAnimationView moon = findViewById(R.id.moon);
        LottieAnimationView sun = findViewById(R.id.sun);
        sun.setVisibility(View.INVISIBLE);
        moon.setVisibility(View.VISIBLE);
    }

    /**
     * sets the chart that displays weather quality
     */
    private void setDisplayChart() {
        setChart();
        setXAxis();
        setYAxis();
        setData();
        setChartAfterDataSet();
        setFont();
    }

    /**
     * sets meta-attributes of the chart (no data yet)
     */
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
        chart.setHighlightPerTapEnabled(false);
        chart.setViewPortOffsets(50f, 5f, 35f, 100f);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setMaxHighlightDistance(100);

    }

    /**
     * sets the X axis of the chart
     */
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

        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int)value % 24) + ":00";
            }
        });
    }

    /**
     * sets the Y axis of the chart
     */
    private void setYAxis() {
        YAxis y = chart.getAxisLeft();
        y.setTypeface(tfLight);
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setGranularityEnabled(true);
        y.setGranularity(0.2f);
        y.setDrawGridLines(false);
    }

    /**
     * sets the font displayed in the chart
     */
    private void setFont() {
        tfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
        tfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
    }

    /**
     * gets the round hour (no minutes) of the best weather
     * @param hourOfMax
     * @return
     */
    private String hourOfMaxHour(float hourOfMax) {
        return String.valueOf((int)hourOfMax % 24);
    }

    /**
     * gets the minutes after the hour in the time of the best weather
     * @param hourOfMax
     * @return
     */
    private String hourOfMaxMinute(float hourOfMax) {
        int hourOfMaxMinuteInt = (int) ((hourOfMax % 1) * 60);
        String hourOfMaxMinute = String.valueOf(hourOfMaxMinuteInt);
        if (hourOfMaxMinuteInt < 10) {
            hourOfMaxMinute = "0" + hourOfMaxMinute;
        }
        return hourOfMaxMinute;
    }

    /**
     * gets the time of the best weather to take a break
     * @param values
     */
    private void setBestBreakTime(ArrayList<Entry> values) {
        ArrayList<Double> currForecast = forecast.getForeCast();
        Double maxForecast = Collections.max(currForecast);
        int indexOfMaxForecast = currForecast.indexOf(maxForecast);
        float hourOfMax = values.get(indexOfMaxForecast).getX();

        TextView breakTimeText = findViewById(R.id.break_time_text);
        breakTimeText.setText(String.format(BREAK_TIME_TEXT_FORMAT, hourOfMaxHour(hourOfMax), hourOfMaxMinute(hourOfMax)));
    }

    /**
     * creates the list of data that will fill the chart
     * @return
     */
    private ArrayList<Entry> createData() {

        Date dateTime = DateStringConverter.stringToDate(forecast.getDateTime());
        Calendar calender = Calendar.getInstance();
        calender.setTime(dateTime);

        int startHour = calender.get(Calendar.HOUR_OF_DAY);
        int startMinute = calender.get(Calendar.MINUTE);

        float startTime = startHour + startMinute / (float)60;

        ArrayList<Entry> values = new ArrayList<>();
        ArrayList<Double> forcast12Hours = forecast.getForeCast();

        values.add(new Entry(startTime, forcast12Hours.get(0).floatValue()));

        // update values array
        for (int hour = 1; hour < forcast12Hours.size(); ++hour) {
            Log.d("x value:", Float.toString(hour));
            values.add(new Entry(startHour + hour, forcast12Hours.get(hour).floatValue())); // add one entry per hour
        }
        setBestBreakTime(values);
        return values;
    }

    /**
     * sets the attribute of the chart's line
     */
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

    /**
     * fits the chart to the data after data is retrieved
     */
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

    /**
     * sets the notification (bell) button to show notifications are enabled/disabled
     */
    private void setNotificationButton() {
        ImageButton notificationButton = findViewById(R.id.notification_button);

        if (isNotificationEnabled) {
            notificationButton.setImageResource(R.drawable.notification_on);
        }
        else {
            notificationButton.setImageResource(R.drawable.notification_off);
        }
    }

    /**
     * track and remembers the user's choice of weather to enable or disable notifications
     * @param view
     */
    public void enableDisableNotification(View view) {
        if (isNotificationEnabled) {
            FileManager.writeToFile("No\n", "shouldSetNotification.txt", this);
            isNotificationEnabled = false;
            Toast.makeText(this, "Break-time notifications off",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            FileManager.writeToFile("Yes\n", "shouldSetNotification.txt", this);
            isNotificationEnabled = true;
            // No toast when switching notifications back on. this is the default state of the app,
            // the user can only return to it if they already canceled notification, which means they know the deal
        }
        setNotificationButton();
        setNotification();
    }

    /**
     * sets the "best time for a break" notification
     * @param pickWeatherTimeMillis
     */
    private void setPickWeatherNotificationScheduler(long pickWeatherTimeMillis) {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PickWeatherNotificationScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, pickWeatherTimeMillis, pendingIntent);
    }

    /**
     * cancells the "best time for a break" notification
     */
    private void cancelPickWeatherNotificationScheduler() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PickWeatherNotificationScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.cancel(pendingIntent);
    }

    /**
     * checks if notifications are enabled or disabled
     * @return
     */
    private Boolean checkNotificationsEnabled() {
        String shouldSetNotification = FileManager.readFromFile(getApplicationContext(), "shouldSetNotification.txt");
        if (shouldSetNotification.equals("Yes\n") || shouldSetNotification.equals("")) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * sets the "best time for a break" notification to the right time, if notifications are enabled
     */
    private void setNotification() {

        cancelPickWeatherNotificationScheduler();

        isNotificationEnabled = checkNotificationsEnabled();
        if (isNotificationEnabled) {
            long pickWeatherTimeMillis = weatherLoader.getPickWeatherTimeMillis();
            setPickWeatherNotificationScheduler(pickWeatherTimeMillis);
        }
        setNotificationButton();
    }

    /**
     * sets the back button to move the app to the background
     * (otherwise it would go back to the loading screen, which is undesirable.
     * the app updates automatically, and we want to allow the user to quit it naturally)
     */
    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }
}
