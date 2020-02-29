package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

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
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DisplayWeatherActivity extends AppCompatActivity {

    private LineChart chart;
    protected Typeface tfRegular;
    protected Typeface tfLight;

    private WeatherLoader weatherLoader;
    private Forecast forecast;
    private SunriseSunset sunriseSunset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_weather);

        setWeather();
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
        chart.setViewPortOffsets(25f, 5f, 25f, 50f);

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
        x.setAxisMinimum(5);
        x.setAxisMaximum(22);
        x.setValueFormatter(new ValueFormatter() {

            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value) {

                long millis = TimeUnit.HOURS.toMillis((long) value);
                return mFormat.format(new Date(millis).getTime());
            }
        });
    }

    private void setYAxis() {
        YAxis y = chart.getAxisLeft();
        y.setTypeface(tfLight);
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setAxisMinimum(-0.5f);
        y.setAxisMaximum(5.5f);
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

        Date dateTime = new Date(forecast.getDateTime());
        //int startHour =

        int sunrise_time = 5;
        int sunset_time = 18;
        // now in hours
        long sunrise = TimeUnit.HOURS.toHours(sunrise_time);
        long now = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
        long sunset = TimeUnit.HOURS.toHours(sunset_time);

        ArrayList<Entry> values = new ArrayList<>();

        // update values array
        for (float x = sunrise; x < sunset; x++) {

            Log.d("x value:", Float.toString(x));
            values.add(new Entry(x, x % 6)); // add one entry per hour
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
}
