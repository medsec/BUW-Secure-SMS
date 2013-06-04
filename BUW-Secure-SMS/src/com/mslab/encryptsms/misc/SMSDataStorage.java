package com.mslab.encryptsms.misc;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class represents the unciphered datastorage for received messages.
 * @author Paul Kramer
 *
 */
public class SMSDataStorage {
	// Database fields
	  private SQLiteDatabase database;
	  private PlainDatabaseController dbHelper;
	  private String[] allColumns = { PlainDatabaseController.COLUNM_DATE,
			  PlainDatabaseController.COLUNM_MESSAGE,
			  PlainDatabaseController.COLUNM_PHONE};

	  /**
	   * Default constructor
	   * @param context The application context.
	   */
	  public SMSDataStorage(Context context) {
	    dbHelper = new PlainDatabaseController(context);
	  }

	  /**
	   * Method to open the database. After every open call, there should be a close call to prevent of damage.
	   * @throws SQLException
	   */
	  public void open() throws SQLException {
	    database = dbHelper.getWritableDatabase();
	  }

	  /**
	   * Close call, to cloase the database.
	   */
	  public void close() {
	    dbHelper.close();
	  }

	  /**
	   * Adds a new received sms to the database by the given data.
	   * @param sms the received sms.
	   * @return The ID of the created database entry.
	   */
	  public long createReceivedSMS(ReceivedSMS sms) {
	    ContentValues values = new ContentValues();
	    values.put(PlainDatabaseController.COLUNM_DATE, sms.Date);
//	    String message = "'" + new String(Base64.encode(sms.Message.getBytes())) + "'";
	    String message = new String(Base64.encode(sms.Message));
	    values.put(PlainDatabaseController.COLUNM_MESSAGE, message);
	    values.put(PlainDatabaseController.COLUNM_PHONE, sms.Phone);
	    
	    return database.insert(PlainDatabaseController.TABLE_NAME, null,
	        values);
	  }

	  /**
	   * Deletes a specified received sms from the database.
	   * @param sms the received sms.
	   * @return True, if everything was ok, false otherwise.
	   */
	  public boolean deleteReceivedSMS(ReceivedSMS sms) {
	    return database.delete(PlainDatabaseController.TABLE_NAME, PlainDatabaseController.COLUNM_DATE
	        + " =? AND "
	        + PlainDatabaseController.COLUNM_MESSAGE + " =? AND "
	        + PlainDatabaseController.COLUNM_PHONE + " =?", new String[]{""+sms.Date, new String(Base64.encode(sms.Message)), sms.Phone}) > 0 ? true : false;
	  }
	  
	  /**
	   * Clears the database.
	   */
	  public void deleteAllReceivedSMS() {
		    database.delete(PlainDatabaseController.TABLE_NAME, null, null);
		  }

	  /**
	   * Gets a list of all received messages.
	   * @return a list of all received messages.
	   */
	  public List<ReceivedSMS> getAllSMS() {
	    List<ReceivedSMS> smss = new ArrayList<ReceivedSMS>();

	    Cursor cursor = database.query(PlainDatabaseController.TABLE_NAME,
	        allColumns, null, null, null, null, null);

	    cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	ReceivedSMS sms = cursorToReceivedSMS(cursor);
	      smss.add(sms);
	      cursor.moveToNext();
	    }
	    // Make sure to close the cursor
	    cursor.close();
	    return smss;
	  }

	  private ReceivedSMS cursorToReceivedSMS(Cursor cursor) {
//		  String message = new String(Base64.decode(cursor.getString(1).substring(1, cursor.getString(1).length()-1).getBytes()));
		  byte[] message = Base64.decode(cursor.getString(1).getBytes());
	    return new ReceivedSMS(cursor.getLong(0), message, cursor.getString(2));
	  }
}
