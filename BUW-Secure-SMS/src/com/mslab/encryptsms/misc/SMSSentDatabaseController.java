package com.mslab.encryptsms.misc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This controller controls the unciphered sqlite database to store received messages.
 * @author Paul Kramer
 *
 */
public class SMSSentDatabaseController  extends SQLiteOpenHelper{
	
	public static final String COLUNM_STATE = "state";
	public static final String COLUNM_CID = "id";
	private static final String DATABASE_NAME = "sentsms";
	
	private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "sentsms";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                COLUNM_CID + " integer, "+
                COLUNM_STATE + " integer);";
    
    //Context
    private Context context;

    /**
     * Constructor
     * @param context The application context.
     */
    public SMSSentDatabaseController(Context context) {
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
