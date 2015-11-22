package com.felipeapp.myapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {

	final static String TABLE_ITEMS = "items";
	final static String TABLE_CORES = "cores";
	final static String TABLE_ROOMS = "rooms";
	final static String ROOM_NAME = "room_name";
	final static String ITEM_NAME = "item_name";
	final static String CORE_ID = "core_id";
	final static String CORE_PIN = "core_pin";
	final static String CORE_NAME = "core_name";
	final static String CORE_ACCESS_TOKEN = "core_acceess_token";
	final static String _ID = "_id";
	final static String[] ITEM_COLUMNS = { _ID, ROOM_NAME, ITEM_NAME, CORE_ID, CORE_PIN };
	final static String[] CORE_COLUMNS = { CORE_ID, CORE_NAME, CORE_ACCESS_TOKEN };
	final static String[] ROOM_COLUMNS = { _ID, ROOM_NAME };

	final private static String CREATE_ITEMS =
		"CREATE TABLE items (" +
			_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			ROOM_NAME + " TEXT NOT NULL, " +
			ITEM_NAME + " TEXT NOT NULL, " +
			CORE_ID + " TEXT NOT NULL, " +
			CORE_PIN + " TEXT NOT NULL)";

	final private static String CREATE_CORES =
		"CREATE TABLE cores (" +
			CORE_ID + " TEXT NOT NULL, " +
			CORE_NAME + " TEXT NOT NULL, " +
			CORE_ACCESS_TOKEN + " TEXT NOT NULL)";

	final private static String CREATE_ROOMS =
		"CREATE TABLE rooms (" +
			_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			ROOM_NAME + " TEXT NOT NULL)";

	final private static String NAME = "my_app_db";
	final private static Integer VERSION = 2;
	final private Context mContext;

	public DatabaseOpenHelper(Context context) {
		super(context, NAME, null, VERSION);
		this.mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_ITEMS);
		db.execSQL(CREATE_CORES);
		db.execSQL(CREATE_ROOMS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	void deleteDatabase() {
		mContext.deleteDatabase(NAME);
	}
}