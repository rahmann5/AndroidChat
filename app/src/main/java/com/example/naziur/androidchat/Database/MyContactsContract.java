package com.example.naziur.androidchat.Database;

import android.provider.BaseColumns;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class MyContactsContract {

    private MyContactsContract () {}

    public static class MyContactsContractEntry implements BaseColumns {
        public static final String TABLE_NAME = "contacts";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_PROFILE = "profile";
        public static final String COLUMN_PROFILE_PIC = "profile_pic";
        public static final String COLUMN_DATE_ADDED = "date_added";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_USERNAME + " INTEGER," +
                        COLUMN_PROFILE + " TEXT," +
                        COLUMN_PROFILE_PIC + " TEXT)"+
                        COLUMN_DATE_ADDED + " DEFAULT CURRENT_DATE)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

    }

}
