package com.mobileapplecture.ilkin.trackme;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Created by Ilkin on 18-Apr-17.
 */


public class FeedIssuesDBHelper extends SQLiteOpenHelper {



    // TODO: original database name is user_locations
    private static final String DATABASE_NAME = "user_locations";
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "DBRead";


    public FeedIssuesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.e(TAG, "FeedIssuesDBHelper - 1");

    }
    private static FeedIssuesDBHelper singleton ;

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(FeedReaderIssues.FeedEntry.SQL_CREATE_ENTRIES);
        Log.e(TAG,"DB created: " + FeedReaderIssues.FeedEntry.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(FeedReaderIssues.FeedEntry.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public static synchronized FeedIssuesDBHelper getInstance(Context context) {
        Log.e(TAG, "FeedIssuesDBHelper - 2");
        if (singleton == null) {
            Log.e(TAG, "FeedIssuesDBHelper - 3");
            singleton = new FeedIssuesDBHelper(context.getApplicationContext());
            Log.e(TAG, "FeedIssuesDBHelper - 4");
        }
        return singleton;
    }
}
