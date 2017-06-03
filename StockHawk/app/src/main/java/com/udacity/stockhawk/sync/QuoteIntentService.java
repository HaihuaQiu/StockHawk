package com.udacity.stockhawk.sync;

import android.app.IntentService;
import android.content.Intent;

import timber.log.Timber;


public class QuoteIntentService extends IntentService {
    public QuoteIntentService() {
        super(QuoteIntentService.class.getSimpleName());
    }
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    @Override
    protected void onHandleIntent(Intent intent) {

        Timber.d("Intent handled");
        QuoteSyncJob.getQuotes(getApplicationContext());
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
        getApplicationContext().sendBroadcast(dataUpdatedIntent);
    }
}
