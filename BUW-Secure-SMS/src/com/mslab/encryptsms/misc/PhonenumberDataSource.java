package com.mslab.encryptsms.misc;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Abstracted controller for the phone number database.
 * @author Paul Kramer
 *
 */
public class PhonenumberDataSource {
	// Database fields
	private SQLiteDatabase database;
	private PhoneNumberDatabaseController dbHelper;
	private String[] allColumns = { PhoneNumberDatabaseController.COLUNM_PHONE };
	
	/*
	 * Constructor.
	 */
	public PhonenumberDataSource(Context context) {
		dbHelper = new PhoneNumberDatabaseController(context);
	}
	
	/**
	 * Opens the database. Throws an exception, if there was an error.
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	/**
	 * After every {@link #open() open} call there should be a close call to prevent the database from errors.
	 */
	public void close() {
		dbHelper.close();
	}
	
	/**
	 * Adds a new phone number to the database.
	 * @param phonenumber The phonenumebr to add.
	 * @return The ID of the added phone number.
	 */
	public long createPhoneNumber(String phonenumber) {
		ContentValues values = new ContentValues();
		values.put(PhoneNumberDatabaseController.COLUNM_PHONE, phonenumber);
		return database.insert(PhoneNumberDatabaseController.TABLE_NAME, null,
				values);
	}
	
	/**
	 * Deletes a given phone nmber from the database.
	 * @param phonenumber Thephone number to delete.
	 * @return The number of rows which where affected.
	 */
	public int deletePhoneNumber(String phonenumber) {
//		Cursor c = database.rawQuery("SELECT * FROM " + PhoneNumberDatabaseController.TABLE_NAME, null);
//		while(c.moveToNext()){
//			System.out.println(c.getString(1));
//		}
		
		int result = database.delete(PhoneNumberDatabaseController.TABLE_NAME,
				PhoneNumberDatabaseController.COLUNM_PHONE + "=?", new String[]{phonenumber});
		
		return result;
	}
	
	/**
	 * Cleans the whole table.
	 * @return The number of rows affected.
	 */
	public int deleteAllPhonenumbers(){
		return database.delete(PhoneNumberDatabaseController.TABLE_NAME, null, null);
	}
	
	/**
	 * Gives a list of all phone numbers.
	 * @return A list of all phone numbers.
	 */
	public List<String> getAllPhoneNumbers() {
		List<String> numbers = new ArrayList<String>();
		
		Cursor cursor = database.query(
				PhoneNumberDatabaseController.TABLE_NAME, allColumns, null,
				null, null, null, null);
		
		while (cursor.moveToNext()) {
			String phonenumber = cursorToPhoneNumbers(cursor);
			numbers.add(phonenumber);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return numbers;
	}
	
	private String cursorToPhoneNumbers(Cursor cursor) {
		return cursor.getString(0);
	}
	
	/**
	 * Gets the phone number if it exists in database.
	 * @param phonenumber The phone number to look for.
	 * @return The phone number.
	 */
	public String containsPhoneNumber(String phonenumber) {
		String contact = null;
		Cursor cursor = database.query(
				PhoneNumberDatabaseController.TABLE_NAME, allColumns, PhoneNumberDatabaseController.COLUNM_PHONE + "=?",
				new String[]{phonenumber}, null, null, null);
		if (cursor.moveToNext()) {
			contact = cursor.getString(0);
		}
		cursor.close();
		return contact;
	}

	/**
	 * Changes the given old phone number with the new one.
	 * @param newPhonenumber The new number.
	 * @param oldPhonenumber The old number.
	 * @return True, if everything gone right.
	 */
	public boolean updatePhoneNumber(String newPhonenumber, String oldPhonenumber) {
		ContentValues values = new ContentValues();
		values.put(PhoneNumberDatabaseController.COLUNM_PHONE, newPhonenumber);
		int mod = database.update(PhoneNumberDatabaseController.TABLE_NAME, values, PhoneNumberDatabaseController.COLUNM_PHONE + "=?", new String[]{oldPhonenumber});
		System.out.println("modified: "+mod);
		return (mod == 1);
	}
}
