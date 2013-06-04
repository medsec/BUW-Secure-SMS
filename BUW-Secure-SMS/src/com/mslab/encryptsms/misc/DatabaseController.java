package com.mslab.encryptsms.misc;

import java.io.File;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import android.content.Context;
import android.provider.OpenableColumns;

/**
 * This class controlls all action with the database. It should be used only from a helper class.
 * @author Paul Kramer
 *
 */
public class DatabaseController {
	
	public static final String TABLE_CONTACTS = "contact";
	public static final String TABLE_HAD = "had";
	public static final String TABLE_CONVERSATION = "conversation";
	public static final String TABLE_EXCHANGE_WAITING = "waiting";
	
	public static final String COLUMN_USER_ID = "uid";
	public static final String COLUMN_CONVERSATION_ID = "cid";
	public static final String COLUMN_USER_NAME = "uname";
	public static final String COLUMN_USER_EMAIL = "email";
	public static final String COLUMN_USER_PHONE = "phone";
	public static final String COLUMN_SHARED_KEY = "skey";
	public static final String COLUMN_EXCHANGE_DONE = "exchange";
	
	public static final String COLUMN_CONVERSATION_DATE = "cdate";
	public static final String COLUMN_CONVERSATION_MESSAGE = "message";
	public static final String COLUMN_CONVERSATION_SENT = "sent";
	public static final String COLUMN_CONVERSATION_STATE = "state";
	
	public static final String COLUMN_EXCHANGE_PRIVATE_KEY = "private";
	public static final String COLUMN_EXCHANGE_PUBLIC_KEY = "public";
	
	public static final String DATABASE_NAME = "conversations.db";
	public static final String DATABASE_SECOND_NAME = "conversations_backup.db";
	
	private static final String DATABASE_CREATE_CONTACTS = "create table "
		      + TABLE_CONTACTS + "(" 
		      + COLUMN_USER_ID + " integer primary key autoincrement, " 
		      + COLUMN_USER_NAME + " text not null, " 
		      + COLUMN_USER_PHONE + " text not null, " 
		      + COLUMN_USER_EMAIL + " text not null, " 
		      + COLUMN_SHARED_KEY + " text not null, "
		      + COLUMN_EXCHANGE_DONE + " integer);";
	
	private static final String DATABASE_CREATE_HAD_RELATION = "create table "
		      + TABLE_HAD + "(" + COLUMN_USER_ID
		      + " integer, "+ COLUMN_CONVERSATION_ID + " integer, foreign key (" + COLUMN_USER_ID + ") references " 
		      + TABLE_CONTACTS + " (" + COLUMN_USER_ID + "), "
		      + "foreign key (" + COLUMN_CONVERSATION_ID + ") references "
		      + TABLE_CONVERSATION + " (" + COLUMN_CONVERSATION_ID + ") "
		      + ");";
	
	private static final String DATABASE_CREATE_CONVERSATIONS = "create table "
		      + TABLE_CONVERSATION + "(" + COLUMN_CONVERSATION_ID
		      + " integer primary key autoincrement, " + COLUMN_CONVERSATION_DATE
		      + " integer not null, " + COLUMN_CONVERSATION_MESSAGE
		      + " text not null, " + COLUMN_CONVERSATION_SENT
		      + " integer not null, " + COLUMN_CONVERSATION_STATE
		      + " );";
	
	private static final String DATABASE_CREATE_WAITING = "create table "
		      + TABLE_EXCHANGE_WAITING + "( "+ COLUMN_USER_ID + " integer, "
		      + COLUMN_EXCHANGE_PRIVATE_KEY + " BLOB not null, " 
		      + COLUMN_EXCHANGE_PUBLIC_KEY + " BLOB not null, "
		      +"foreign key(" + COLUMN_USER_ID
		      + ") references " +TABLE_CONTACTS + " (" + COLUMN_USER_ID+ "));";
	
	private Context context;
	private SQLiteDatabase database;

	/**
	 * Default constructor. Do not use the constructor without arguments!
	 * @param context The application context.
	 */
	public DatabaseController(Context context) {
		this.context = context;
	}
	
	/**
	 * Gets the path to the database.
	 * @return The path to the database.
	 */
	public String getDatabasePath(){
		return context.getDatabasePath(DATABASE_NAME).getPath();
	}
	
	/**
	 * Gets a path to a backup database.
	 * @return The path to a backup database.
	 */
	public String getSecondDatabasePath(){
		return context.getDatabasePath(DATABASE_SECOND_NAME).getPath();
	}
	
	/**
	 * Initializes the database. It creates all files and tables and initializes the cipher. 
	 * @param password The database password.
	 * @return The Database.
	 */
	private SQLiteDatabase InitializeSQLCipher(String password) {
        SQLiteDatabase.loadLibs(context);
        String databaseFilePath = context.getDatabasePath(DATABASE_NAME).getPath();
        File databaseFile = new File(databaseFilePath);
//        databaseFile.mkdirs();
        boolean db_exists = databaseFile.exists();
        
        if(!db_exists) databaseFile.getParentFile().mkdirs();
//        databaseFile.delete();
        SQLiteDatabase database = null;
        try{
        	database = SQLiteDatabase.openOrCreateDatabase(databaseFilePath, password, null);
        }catch(SQLiteException se){
        	se.printStackTrace();
        }
        
        if(!db_exists && database != null){
	        database.execSQL(DATABASE_CREATE_CONTACTS);
	        database.execSQL(DATABASE_CREATE_CONVERSATIONS);
	        database.execSQL(DATABASE_CREATE_HAD_RELATION);
	        database.execSQL(DATABASE_CREATE_WAITING);
        }
        
        return database;
    }
	
	/**
	 * Opens a database or if no database exists, it creates one.
	 * @param password The password of the database.
	 * @return The database.
	 */
	public SQLiteDatabase open(String password){
		if(database == null || !database.isOpen()){
			database = InitializeSQLCipher(password);
		}
		return database;
	}

	/**
	 * After every {@link #open(String) open} call, a close call should follow to prevent the database from errors.
	 */
	public void close() {
		database.close();
		database = null;
	}
	
	/**
	 * Checks if the database is open.
	 * @return True if it is open, false otherwise.
	 */
	public boolean isOpen(){
		return !(database == null);
	}

	/**
	 * This method changes the database password. It can not be undone, do not loose the key!
	 * @param newPassword The new password.
	 */
	public void changeKey(byte[] newPassword) {
		database.execSQL("PRAGMA rekey = '"+new String(newPassword)+"';");
	}
	
}
