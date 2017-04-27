package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.os.SystemClock.sleep;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


import static android.os.SystemClock.sleep;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */


public class SecondActivity extends Activity {
    private TextView DataFieldSecScreen;
    private TextView DataFieldSecScreenDevNeg;
    LineGraphSeries<DataPoint> series;
    LineGraphSeries<DataPoint> seriesControl;
    ArrayList<Integer> get_squats = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph);
        DataFieldSecScreen = (TextView) findViewById(R.id.data_value_secScreen);
        DataFieldSecScreen.setText(R.string.data_of_avg_pitch);

        //переменные для расчета отклонения от угла в 45
        DataFieldSecScreenDevNeg = (TextView) findViewById(R.id.data_value_secScreen_dev_neg);
        DataFieldSecScreenDevNeg.setText(R.string.data_of_deviation_neg);

        //получение данных с первого экрана
        get_squats = getIntent().getIntegerArrayListExtra("graph_intent");

        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        seriesControl = new LineGraphSeries<DataPoint>();
        int y;
        double x = 0;
        int controlPitch = 45;
        int avg = 0;

        //для подсчета плохого отклонения от эталонного угла
        int sum_neg = 0;
        int sum_2_neg = 0;
        int count = 0;

        for(int i = 0; i < get_squats.size(); i++) {
            //из расчета частоты снимаемых измерений: 1 измерение раз в 50 мс; тогда за 1 секунду снимается 20 измерений; тогда 1 измерение снимается раз в 0.05 секунд
            //по оси x предполагается откладывать время в секундах, прошедшее с начала приседаний
            if (i == 0) {
                x = 0;
            } else {
                x += 0.05;
            }
                y = get_squats.get(i);

                avg += y;


            if (y < controlPitch) {
                sum_neg += y;
                sum_2_neg += Math.pow(y, 2);
                count++;
            }

            series.appendData(new DataPoint(x, y), true, get_squats.size());
            seriesControl.appendData(new DataPoint(x, controlPitch), true, get_squats.size());
        }
        //считаем среднее
        if (get_squats.size() != 0) { // предотвращение деления на ноль
            avg = avg / get_squats.size();
        }
        //считаем стандартное отклонение
        if (count > 2) {
        double deviation_neg = Math.sqrt((double)(sum_2_neg + (count-1) * Math.pow(controlPitch,2) - 2 * controlPitch * sum_neg) / (count - 2));
        double newDouble_neg = new BigDecimal(deviation_neg).setScale(2, RoundingMode.UP).doubleValue();
        String dev_str_neg = String.valueOf(newDouble_neg);
        DataFieldSecScreenDevNeg.setText(dev_str_neg);
        }

        if (avg != 0) {
            String avg_str = String.valueOf(avg);
           DataFieldSecScreen.setText(avg_str);
        }

        //НАСТРОЙКА ГРАФИКОВ


       //seriesControl.setTitle("Control Pitch");
       // series.setColor(Color.GREEN);
      // series.setDrawDataPoints(true);
        //series.setDataPointsRadius(6);
        series.setThickness(10);
        seriesControl.setThickness(2);
        seriesControl.setColor(Color.RED);

        graph.addSeries(series);
        graph.addSeries(seriesControl);

        graph.getGridLabelRenderer().setVerticalAxisTitle("Pitch");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Training time in sec");
        graph.getGridLabelRenderer().setVerticalAxisTitleTextSize(20);
        graph.getGridLabelRenderer().setLabelVerticalWidth(12);
        //graph.getGridLabelRenderer().setPadding(12);
        graph.getViewport().setMinX(0.05);

        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(100);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);

        //graph.getGridLabelRenderer().setHorizontalAxisTitle("Number of squats");
        series.setDrawBackground(true); // activate the background feature<br />
        series.setBackgroundColor(Color.rgb(186, 194, 255)); // below the line fill with color<br />



    }

    }


