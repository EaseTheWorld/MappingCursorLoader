package com.easetheworld.mappingcursorloadertest;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

public class CheeseProvider extends ContentProvider {

	public static final String AUTHORITY = "com.easetheworld.mappingcursorloadertest.provider";
	
	private SQLiteOpenHelper mDatabase;

	@Override
	public boolean onCreate() {
		mDatabase = new DatabaseHelper(getContext());
		return true;
	}

	// Database Schema

	public static class CheeseTable  {
		public static final String TABLE_NAME = "cheese";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(Uri.parse("content://"+AUTHORITY), TABLE_NAME);
		public static final String ID = BaseColumns._ID;
		public static final String NAME = "name";
		public static final String FLAG1 = "flag1";
		public static final String FLAG2 = "flag2";
		
		public static final int COLUMN_INDEX_ID = 0;
		public static final int COLUMN_INDEX_NAME = 1;
		public static final int COLUMN_INDEX_FLAG1 = 2;
		public static final int COLUMN_INDEX_FLAG2 = 3;
	}
	
	// DatabaseHelper
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String NAME = "cheeses.db";
		private static final int VERSION = 1;
		
		private DatabaseHelper(Context context) {
			super(context, NAME, null, VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + CheeseTable.TABLE_NAME + "(" +
					 CheeseTable.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					 CheeseTable.NAME + " TEXT NOT NULL," +
					 CheeseTable.FLAG1 + " INTEGER DEFAULT 0," +
					 CheeseTable.FLAG2 + " INTEGER DEFAULT 0" +
					");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor c = mDatabase.getReadableDatabase().query(CheeseTable.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		if (c != null)
			c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId = mDatabase.getWritableDatabase().insert(CheeseTable.TABLE_NAME, null, values);
		if (rowId >= 0) {
			Uri newUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		} else
			return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = mDatabase.getWritableDatabase().update(CheeseTable.TABLE_NAME, values, selection, selectionArgs);
		if (count > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = mDatabase.getWritableDatabase().delete(CheeseTable.TABLE_NAME, selection, selectionArgs);
		if (count > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}
}