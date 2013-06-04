package com.mslab.encryptsms.misc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class represents the controller for the database to store encrypted messages.
 * @author Paul Kramer
 *
 */
public class PlainDatabaseController extends SQLiteOpenHelper{
	
	public static final String COLUNM_MESSAGE = "message";
	public static final String COLUNM_PHONE = "phone";
	public static final String COLUNM_DATE = "date";
	private static final String DATABASE_NAME = "plainsms";
	
	private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "received_sms";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                COLUNM_PHONE + " TEXT not null, " +
                COLUNM_DATE + " integer not null, " +
                COLUNM_MESSAGE + " TEXT not null);";
    
    //Context
    private Context context;

    /**
     * Constructor.
     * @param context The application context.
     */
    public PlainDatabaseController(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		    onCreate(db);
	}
}
