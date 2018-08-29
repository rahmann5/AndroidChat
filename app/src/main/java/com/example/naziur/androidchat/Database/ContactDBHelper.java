package com.example.naziur.androidchat.Database;

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

    public long insertContact (String username, String profile, String profilePic) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues data = new ContentValues();
        data.put(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME, username);
        data.put(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE, profile);
        data.put(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC, profilePic);
        return db.insert(MyContactsContract.MyContactsContractEntry.TABLE_NAME, null, data);
    }

    public Cursor getAllMyContacts (String orderBy) {
        SQLiteDatabase db = getWritableDatabase();
        String [] projection =  {
                BaseColumns._ID,
                MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME,
                MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE,
                MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC
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

    public int removeContact (String username){
        SQLiteDatabase db = getWritableDatabase();
        String where = MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME  + " = ?";
        return db.delete(MyContactsContract.MyContactsContractEntry.TABLE_NAME, where, new String []{username});
    }
}
