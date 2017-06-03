package com.udacity.stockhawk.ui;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import yahoofinance.YahooFinance;

import static com.udacity.stockhawk.R.id.date;

;

/**
 * Created by QHH on 2017/5/31.
 */

public class BarChartActivity extends AppCompatActivity implements OnChartValueSelectedListener,LoaderManager.LoaderCallbacks<Cursor>{
    protected BarChart mChart;
    private static final int STOCK_LOADER = 0;
    private TextView tvX, tvY;
    private String symbolName;
    private String[] xLableArray;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_barchart);
        symbolName=getIntent().getStringExtra("Symbol");
        tvX = (TextView) findViewById(date);
        tvY = (TextView) findViewById(R.id.close);


        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
        // mChart.setDrawLegend(false);

    }
    protected RectF mOnValueSelectedRectF = new RectF();

    @SuppressLint("NewApi")
    @Override
    public void onValueSelected(Entry e, Highlight h) {

        if (e == null)
            return;
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis((long) e.getX());
        SimpleDateFormat format1=new SimpleDateFormat("yyyy-MM-dd");
        String c =format1.format(calendar.getTime());
        tvX.setText(xLableArray[(int)e.getX()]);
        tvX.setContentDescription(xLableArray[(int)e.getX()]);
        String close="$"+e.getY();
        tvY.setText(close);
        tvY.setContentDescription(close);

        RectF bounds = mOnValueSelectedRectF;

        mChart.getBarBounds((BarEntry) e, bounds);
        MPPointF position = mChart.getPosition(e, AxisDependency.LEFT);

        Log.i("bounds", bounds.toString());
        Log.i("position", position.toString());

        Log.i("x-index",
                "low: " + mChart.getLowestVisibleX() + ", high: "
                        + mChart.getHighestVisibleX());

        MPPointF.recycleInstance(position);
    }

    @Override
    public void onNothingSelected() { }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.makeUriForStock(symbolName),
                null,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()){
            int index = data.getColumnIndex(Contract.Quote.COLUMN_HISTORY);
            String stockInfo=data.getString(index);
            displayFunc(stockInfo);

        }
        data.close();
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void displayFunc(String history) {
        try {
            InputStreamReader is = new InputStreamReader(new ByteArrayInputStream(history.getBytes(StandardCharsets.UTF_8)));
            BufferedReader br = new BufferedReader(is);
            ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();
            final ArrayList<String> xLabel = new ArrayList<>();
            int i = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] data = line.split(YahooFinance.QUOTES_CSV_DELIMITER);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(Long.parseLong(data[0]));

                SimpleDateFormat format1 = new SimpleDateFormat("yyyy/MM/dd");
                String c = format1.format(calendar.getTime());
                xLabel.add(c);
//            float date = Float.parseFloat(c);
//            Date date1= new Date();
//            Calendar calendar2 = Calendar.getInstance();
//            calendar2.setTimeInMillis(Long.parseLong(data[0]));
//            Date date2=calendar2.getTime();
//            long timeDiff = date1.getTime() -date2.getTime();
//            long days = timeDiff / (24 * 60 * 60 * 1000);

                yVals1.add(new BarEntry(i++, Float.parseFloat(data[1])));
            }
            mChart = (BarChart) findViewById(R.id.chart1);
            xLableArray = xLabel.toArray(new String[xLabel.size()]);
            IAxisValueFormatter formatter = new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    String xLable= xLableArray[(int) value];
                    String[] xLable1=xLable.split("/");
                    if(mChart.getVisibleXRange() >60)
                        return xLable1[0].substring(2)+"/"+xLable1[1];
                    else
                        return xLableArray[(int) value];
                }
            };
            mChart = (BarChart) findViewById(R.id.chart1);
            XAxis xAxis = mChart.getXAxis();
            xAxis.setPosition(XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(7f); // only intervals of 1 day
            xAxis.setLabelCount(7);
            xAxis.setValueFormatter(formatter);

            BarDataSet set1;

            if (mChart.getData() != null &&
                    mChart.getData().getDataSetCount() > 0) {
                set1 = (BarDataSet) mChart.getData().getDataSetByIndex(0);
                set1.setValues(yVals1);
                mChart.getData().notifyDataChanged();
                mChart.notifyDataSetChanged();
            } else {
                set1 = new BarDataSet(yVals1, getString(R.string.Historical_stock_name,symbolName.toUpperCase()));

                set1.setColors(ColorTemplate.MATERIAL_COLORS);
                ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
                dataSets.add(set1);
                BarData data1 = new BarData(dataSets);
                data1.setValueTextSize(10f);
                data1.setBarWidth(0.9f);
                data1.setDrawValues(false);
                mChart.setData(data1);
            }

            mChart.setOnChartValueSelectedListener(this);

            mChart.setDrawBarShadow(true);
            mChart.setDrawValueAboveBar(true);

            mChart.getDescription().setEnabled(false);

            // if more than 60 entries are displayed in the chart, no values will be
            // drawn
            mChart.setMaxVisibleValueCount(800);

            // scaling can now only be done on x- and y-axis separately
            mChart.setPinchZoom(false);

            mChart.setDrawGridBackground(false);
            // mChart.setDrawYLabels(false);

            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.setLabelCount(8, false);
            leftAxis.setPosition(YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setSpaceTop(15f);
            leftAxis.setAxisMinimum(0f);// this replaces setStartAtZero(true)


            YAxis rightAxis = mChart.getAxisRight();
            rightAxis.setDrawGridLines(false);
            rightAxis.setLabelCount(8, false);
            rightAxis.setSpaceTop(15f);
            rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)


            Legend l = mChart.getLegend();
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            l.setDrawInside(false);
            l.setForm(LegendForm.SQUARE);
            l.setFormSize(9f);
            l.setTextSize(11f);
            l.setXEntrySpace(4f);
            // l.setExtra(ColorTemplate.VORDIPLOM_COLORS, new String[] { "abc",
            // "def", "ghj", "ikl", "mno" });
            // l.setCustom(ColorTemplate.VORDIPLOM_COLORS, new String[] { "abc",
            // "def", "ghj", "ikl", "mno" });
            mChart.invalidate();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

}
