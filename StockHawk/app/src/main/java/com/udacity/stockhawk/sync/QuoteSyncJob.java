package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.IntDef;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import static com.udacity.stockhawk.data.PrefUtils.setStatus;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED_SUCCESS = "com.udacity.stockhawk.ACTION_DATA_UPDATED_SUCCESS";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK, STATUS_WITH_NO_STOCK, STATUS_WITH_ALREADY_EXISTED_STOCK,  STATUS_WITH_UPDATED_FAILURE,STATUS_UNKNOWN})
    public @interface Status {}
    public static final int STATUS_OK = 0;
    public static final int STATUS_WITH_NO_STOCK = 1;
    public static final int STATUS_WITH_ALREADY_EXISTED_STOCK = 2;
    public static final int STATUS_WITH_UPDATED_FAILURE = 3;
    public static final int STATUS_UNKNOWN = 4;
    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {

        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();


                Stock stock = quotes.get(symbol);
                StockQuote quote = stock.getQuote();

                float price = quote.getPrice().floatValue();
                float change = quote.getChange().floatValue();
                float percentChange = quote.getChangeInPercent().floatValue();

                // WARNING! Don't request historical data for a stock that doesn't exist!
                // The request will hang forever X_x
                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);


/*
                Begin dummy data code, Please remove this when API is functioning again or we have a better solution
                ----------------------------------------------------------------------------------------------------
*/

//                List<HistoricalQuote> history = new ArrayList<>();
//
//                InputStream is = context.getResources().openRawResource(R.raw.dummy_data);
//                Writer writer = new StringWriter();
//                char[] buffer = new char[1024];
//                try {
//                    Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//                    int n;
//                    while ((n = reader.read(buffer)) != -1) {
//                        writer.write(buffer, 0, n);
//                    }
//                } finally {
//                    is.close();
//                }
//
//                String jsonString = writer.toString();
//                try {
//                    JSONObject json = new JSONObject(jsonString);
//                    JSONObject query = json.getJSONObject("query");
//                    JSONObject results = query.getJSONObject("results");
//                    JSONArray quoteArray = results.getJSONArray("quote");
//                    for (int i = 0; i < quoteArray.length(); i++) {
//                        JSONObject quoteObject = quoteArray.getJSONObject(i);
//                        String symbolName =quoteObject.getString("Symbol");
//                        //Get calendar
//                        String dateString = quoteObject.getString("Date");
//                        Calendar cal = Calendar.getInstance();
//                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//                        cal.setTime(sdf.parse(dateString));
//                        BigDecimal open = new BigDecimal(quoteObject.getString("Open"));
//                        BigDecimal high = new BigDecimal(quoteObject.getString("High"));
//                        BigDecimal low = new BigDecimal(quoteObject.getString("Low"));
//                        //get closign price
//                        BigDecimal closingPrice = new BigDecimal(quoteObject.getString("Close"));
//                        BigDecimal adjClose = new BigDecimal(quoteObject.getString("Adj_Close"));
//                        Long volume =quoteObject.getLong("Volume");
//                        HistoricalQuote historicalQuote = new HistoricalQuote(symbolName,cal,open,low,high,closingPrice,adjClose,volume);
//                        history.add(historicalQuote);
//
//                    }
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }

                StringBuilder historyBuilder = new StringBuilder();
                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    historyBuilder.append(", ");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append("\n");
//                    if(!loop){
//                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//                        SharedPreferences.Editor editor = prefs.edit();
//                        editor.putLong(symbol,it.getDate().getTimeInMillis());
//                        editor.apply();
//                        loop = true;
//                    }
                }

                /*
                ----------------------------------------------------------------------------------------------------
                End dummy data code
                */

                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);


                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                quoteCVs.add(quoteCV);

            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));
            setStatus(context,STATUS_OK);
//            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
//            context.sendBroadcast(dataUpdatedIntent);
            updateWidgets(context);
        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
            setStatus(context,STATUS_WITH_UPDATED_FAILURE);
        }
        catch (NullPointerException ex){
            setStatus(context,STATUS_WITH_NO_STOCK);
        }
    }

    private static void updateWidgets(Context context) {
        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED_SUCCESS)
                .setPackage(context.getPackageName());
        context.sendBroadcast(dataUpdatedIntent);
    }
    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());


        }
    }


}
