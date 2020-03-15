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
import android.widget.ImageButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DisplayWeatherActivity extends AppCompatActivity {

    private LineChart chart;
    protected Typeface tfRegular;
    protected Typeface tfLight;

    private ImageButton notificationButton;
    private Boolean isNotificationEnabled;

    private WeatherLoader weatherLoader;
    private Forecast forecast;
    private SunriseSunset sunriseSunset;

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
        chart.setViewPortOffsets(0f, 0f, 0f, 100f);
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
        x.setTextColor(Color.BLACK);
        x.setCenterAxisLabels(true);
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
        //y.setAxisMinimum(-0.5f);
        //y.setAxisMaximum(5.5f);
        y.setGranularityEnabled(true);
        y.setGranularity(0.2f);
        y.setDrawGridLines(false);
        y.setTextColor(Color.BLACK);
    }



    private void setFont() {
        tfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
        tfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
    }


    private ArrayList<Entry> createData() {

        Date dateTime = DateStringConverter.stringToDate(forecast.getDateTime());
        Calendar calender = Calendar.getInstance();
        calender.setTime(dateTime);

        int startHour = calender.get(Calendar.HOUR);
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

        chartData.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        chartData.setCubicIntensity(0.2f);
        chartData.setDrawFilled(true);
        chartData.setDrawCircles(false);
        chartData.setDrawValues(false);
        chartData.setLineWidth(0f);
        chartData.setHighLightColor(Color.TRANSPARENT);
        chartData.setColor(Color.rgb(238, 230, 255));
        chartData.setFillAlpha(70);
        chartData.setDrawHorizontalHighlightIndicator(false);

        if (Utils.getSDKInt() >= 18) {
            // drawables only supported on api level 18 and above
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_chart);
            chartData.setFillDrawable(drawable);
        } else {
            chartData.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return chart.getAxisLeft().getAxisMinimum();
                }
            });
        }

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
        chart.animateXY(2000, 2000);
        chart.getLegend().setEnabled(false);
        chart.setVisibleXRangeMaximum(8);
        chart.invalidate();
    }

    private void setNotificationButton() {
        notificationButton = findViewById(R.id.notification_button);

        if (isNotificationEnabled) {
            notificationButton.setImageResource(R.drawable.bell);
        }
        else {
            notificationButton.setImageResource(R.drawable.bell_silent);
        }
    }

    public void enableDisableNotification(View view) {
        if (isNotificationEnabled) {
            FileManager.writeToFile("No", "shouldSetNotification.txt", this);
            isNotificationEnabled = false;
        }
        else {
            FileManager.writeToFile("Yes", "shouldSetNotification.txt", this);
            isNotificationEnabled = true;
        }
        setNotificationButton();
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
        if (shouldSetNotification.equals("Yes") || shouldSetNotification.equals("")) {
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
