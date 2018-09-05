package com.example.naziur.androidchat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class ContactDBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "AChat.db";

    public ContactDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(MyContactsContract.MyContactsContractEntry.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(MyContactsContract.MyContactsContractEntry.SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }

    public long insertContact (String username, String profile, String profilePic, String deviceToken) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues data = new ContentValues();
        data.put(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME, username);
        data.put(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE, profile);
        data.put(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC, profilePic);
        data.put(MyContactsContract.MyContactsContractEntry.COLUMN_DEVICE_TOKEN, deviceToken);
        return db.insert(MyContactsContract.MyContactsContractEntry.TABLE_NAME, null, data);
    }

    public Cursor getAllMyContacts (String orderBy) {
        SQLiteDatabase db = getWritableDatabase();
        String [] projection =  {
                BaseColumns._ID,
                MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME,
                MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE,
                MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC,
                MyContactsContract.MyContactsContractEntry.COLUMN_DEVICE_TOKEN
        };

        Cursor cursor = db.query(
                MyContactsContract.MyContactsContractEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                orderBy
        );


        return cursor;
    }

    public boolean isUserAlreadyInContacts(String username){
        SQLiteDatabase db = this.getWritableDatabase();

        String[] projection = {
                BaseColumns._ID
        };

        String selection = MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = { username };

        Cursor cursor = db.query(
                MyContactsContract.MyContactsContractEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null                    // The sort order
        );

        return (cursor.getCount() > 0);

    }

    public String[] getProfileNameAndPic(String username){
        String profileName = username;
        String profilePic = "";
        SQLiteDatabase db = this.getWritableDatabase();

        String[] projection = {
                MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE,
                MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC
        };

        String selection = MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = { username };

        Cursor cursor = db.query(
                MyContactsContract.MyContactsContractEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null                    // The sort order
        );

        if(cursor.getCount() > 0){
            try{
                while (cursor.moveToNext()) {
                    profileName = cursor.getString(cursor.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE));
                    profilePic = cursor.getString(cursor.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC));
                }
            } finally {
                cursor.close();
            }
        }
        return new String[]{profileName, profilePic};
    }

    public int removeContact (String username){
        SQLiteDatabase db = getWritableDatabase();
        String where = MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME  + " = ?";
        return db.delete(MyContactsContract.MyContactsContractEntry.TABLE_NAME, where, new String []{username});
    }

    public void updateProfile(String username, String newProfile, String profilePic){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE, newProfile);
        cv.put(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC, profilePic);
        db.update(MyContactsContract.MyContactsContractEntry.TABLE_NAME, cv, MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME + "= ?", new String[] {username});
    }
}
