package com.mslab.encryptsms.misc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Controller for the unciphered phonenumber database.
 * @author Paul Kramer
 *
 */
public class PhoneNumberDatabaseController extends SQLiteOpenHelper{
	
	public static final String COLUNM_PHONE = "phone";
	public static final String COLUNM_PID = "id";
	private static final String DATABASE_NAME = "phonenumbers";
	
	private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "phonenumber";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                COLUNM_PID + " integer primary key autoincrement, "+
                COLUNM_PHONE + " TEXT not null);";
    
    //Context
    private Context context;

    /**
     * Default constructor, it uses the application context.
     * @param context The application context.
     */
    public PhoneNumberDatabaseController(Context context) {
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