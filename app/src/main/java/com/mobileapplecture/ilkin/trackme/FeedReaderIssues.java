package com.mobileapplecture.ilkin.trackme;

import android.provider.BaseColumns;

/**
 * Created by Ilkin on 18-Apr-17.
 * <p>
 * Schema class for our Database
 */

public final class FeedReaderIssues {

    /*
    *     // To prevent someone from accidentally instantiating the contract class,
        // make the constructor private.
        private FeedReaderContract() {}

         Inner class that defines the table contents
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_SUBTITLE = "subtitle";
    }
    */
// To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private FeedReaderIssues() {
    }

    /*Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {

        public static final String TABLE_NAME = "user_location";

        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_LONG = "longitude";
        public static final String COLUMN_LAT = "latitude";
        public static final String COLUMN_ALT = "altitude";
        public static final String COLUMN_VEL = "velocity";


        public static final String SQL_CREATE_ENTRIES = "Create Table If NOT exists " + FeedEntry.TABLE_NAME + "(" +
                FeedEntry._ID + " INTEGER PRIMARY KEY, " +
                FeedEntry.COLUMN_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                FeedEntry.COLUMN_LONG + " REAL, " +
                FeedEntry.COLUMN_LAT + " REAL, " +
                FeedEntry.COLUMN_ALT + " REAL, " +
                FeedEntry.COLUMN_VEL + " REAL)";

        public static final String SQL_ENTER_VALUES = "Insert into " + FeedEntry.TABLE_NAME + "(time, longitude, latitude, altitude, velocity) " +
                "Values (datetime(), " + FeedEntry.COLUMN_LAT + ","+ FeedEntry.COLUMN_LONG+ ","+ FeedEntry.COLUMN_LONG + ","+
                FeedEntry.COLUMN_ALT + ","+ FeedEntry.COLUMN_VEL+ ")";


        public static final String SQL_DELETE_ENTRIES = "Drop table if exists " + FeedEntry.TABLE_NAME;
    }


}
