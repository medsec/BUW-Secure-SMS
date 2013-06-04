package com.mslab.encryptsms.misc;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Abstracted database interface for managing sent sms.
 * @author Paul Kramer
 *
 */
public class SMSSentDataSource {
	// Database fields
	private SQLiteDatabase database;
	private SMSSentDatabaseController dbHelper;
	private String[] allColumns = { SMSSentDatabaseController.COLUNM_CID, SMSSentDatabaseController.COLUNM_STATE };
	
	/**
	 * Constructor.
	 * @param context The Application context.
	 */
	public SMSSentDataSource(Context context) {
		dbHelper = new SMSSentDatabaseController(context);
	}
	
	/**
	 * Method to open the database. After every open call, there should be a close call.
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	/**
	 * Close call to close the database properly.
	 */
	public void close() {
		dbHelper.close();
	}
	
	/**
	 * Adds new message to the Database.
	 * @param id The ID of the message, this ID comes from the ciphered database.
	 * @param state The state to store.
	 * @return The ID in this database.
	 */
	public long createSentSMS(long id, int state) {
		ContentValues values = new ContentValues();
		values.put(SMSSentDatabaseController.COLUNM_CID, id);
		values.put(SMSSentDatabaseController.COLUNM_STATE, state);
		return database.insert(SMSSentDatabaseController.TABLE_NAME, null,
				values);
	}
	
	/**
	 * Removes a successfully sent message from this database.
	 * @param id The message ID.
	 * @return The number of rows affected.
	 */
	public int deleteSentSMS(long id) {
		int result = database.delete(SMSSentDatabaseController.TABLE_NAME,
				SMSSentDatabaseController.COLUNM_CID + "=?", new String[]{""+id});
		return result;
	}
	
	/**
	 * Returns a list of all messages.
	 * @return a list of all messages waiting for received state.
	 */
	public List<SentSMS> getAllSentSMS() {
		List<SentSMS> numbers = new ArrayList<SentSMS>();
		
		Cursor cursor = database.query(
				SMSSentDatabaseController.TABLE_NAME, allColumns, null,
				null, null, null, null);
		
		while (cursor.moveToNext()) {
			SentSMS sentsms = cursorToSentSMS(cursor);
			numbers.add(sentsms);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return numbers;
	}
	
	private SentSMS cursorToSentSMS(Cursor cursor) {
		return new SentSMS(cursor.getLong(0), cursor.getInt(1));
	}
	
	/**
	 * Update the state for a specified message.
	 * @param id The ID of the message to change state.
	 * @param newState The new state.
	 * @return True, if everything was ok, false otherwise.
	 */
	public boolean updateSentStatus(long id, int newState){
		ContentValues values = new ContentValues();
		values.put(SMSSentDatabaseController.COLUNM_STATE, newState);
		return (database.update(SMSSentDatabaseController.TABLE_NAME, values, SMSSentDatabaseController.COLUNM_CID + "=?", new String[]{""+id}) == 1);
	}
	
}
