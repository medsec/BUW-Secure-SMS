package com.mslab.encryptsms.misc;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import net.sqlcipher.database.SQLiteDatabase;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Base64;

import com.mslab.encryptsms.misc.DHKeyGen.DHKeyPair;

/**
 * This class represents the interface to the enciphered database to store all conversations in it. 
 * Additionally it manages the storage of secret keys and user contact details.
 * @author Paul Kramer
 *
 */
public class ConversationsDataSource {
	// Database fields
	  private SQLiteDatabase database;
	  private DatabaseController dbHelper;
	  private String[] contactColumns = { DatabaseController.COLUMN_USER_ID,
			  DatabaseController.COLUMN_USER_NAME, 
			  DatabaseController.COLUMN_USER_PHONE,
			  DatabaseController.COLUMN_USER_EMAIL,
			  DatabaseController.COLUMN_SHARED_KEY,
			  DatabaseController.COLUMN_EXCHANGE_DONE};
	  
	  private String[] conversationColumns = { 
			  DatabaseController.COLUMN_CONVERSATION_ID, 
			  DatabaseController.COLUMN_CONVERSATION_DATE, 
			  DatabaseController.COLUMN_CONVERSATION_MESSAGE,
			  DatabaseController.COLUMN_CONVERSATION_SENT,
			  DatabaseController.COLUMN_CONVERSATION_STATE};
	  
	  private String[] exchangeWaitingColumns = { 
			  DatabaseController.COLUMN_USER_ID, 
			  DatabaseController.COLUMN_EXCHANGE_PRIVATE_KEY, 
			  DatabaseController.COLUMN_EXCHANGE_PUBLIC_KEY};
	  
	  //hold context
	  private Context context;
	  
	  /**
	   * Special constructor to open a database.
	   * @param context the applicationcontext
	   */
	  public ConversationsDataSource(Context context) {
	    dbHelper = new DatabaseController(context);
	    this.context = context;
	  }

	  /**
	   * Open method to open a databse
	   * @param password The databasepassword.
	   * @return true if it was opened
	   * @throws SQLException if something goes wrong, like wrong password, no space on device or sth. like this.
	   */
	  public boolean open(byte[] password) throws SQLException {
		  database = dbHelper.open(Base64.encodeToString(password, Base64.DEFAULT));
		  return database != null;
	  }

	  /**
	   * Method to close the database. This method should be called after every 
	   * {@link #open(byte[]) open} method call to prevent of undefined states.
	   */
	  public void close() {
		  dbHelper.close();
	  }
	  
	  /**
	   * Returns the absolute databsepath on device.
	   * @return The absolute database path.
	   */
	  public String getDatabasePath(){
		  return dbHelper.getDatabasePath();
	  }
	  
	  /**
	   * Returns a databse path of the backup database.
	   * @return The path to the backup database.
	   */
	  public String getSecondDatabasePath(){
		  return dbHelper.getSecondDatabasePath();
	  }

	  /**
	   * This method creates a new contact in databse from the given contact object.
	   * @param contact The contact to create in database.
	   * @return The updates contact with row ID.
	   */
	  public Contact createContact(Contact contact) {
	    ContentValues values = new ContentValues();
	    values.put(DatabaseController.COLUMN_USER_NAME, contact.name);
	    values.put(DatabaseController.COLUMN_USER_EMAIL, contact.email);
	    values.put(DatabaseController.COLUMN_USER_PHONE, contact.phonenumber.replaceAll("\\s", ""));
	    values.put(DatabaseController.COLUMN_SHARED_KEY, Base64.encodeToString(contact.secret, Base64.DEFAULT));
	    values.put(DatabaseController.COLUMN_EXCHANGE_DONE, contact.exchange_done);
	    long insertId = database.insert(DatabaseController.TABLE_CONTACTS, null,
	        values);
	    Cursor cursor = database.query(DatabaseController.TABLE_CONTACTS,
	        contactColumns, DatabaseController.COLUMN_USER_ID + " = " + insertId, null,
	        null, null, null);
	    cursor.moveToFirst();
	    Contact newContact = cursorToContact(cursor);
	    cursor.close();
	    
		if (contact.exchange_done == Contact.ACCEPTED) {
			// add contact to phone numbers database
			PhonenumberDataSource phonenumbers = new PhonenumberDataSource(
					context);
			phonenumbers.open();
			long result = phonenumbers
					.createPhoneNumber(newContact.phonenumber);
			phonenumbers.close();
		}
	    
	    return newContact;
	  }
	  
	  /**
	   * Creates a contact from the given contact an the keypair. The contact does not contain a secret and has the state "waiting".
	   * @param contact The contact to create.
	   * @param keypair The keypair of the given contact.
	   * @return An updated contact, including row ID and waiting state.
	   */
	  public Contact createContact(Contact contact, DHKeyPair keypair){
		  contact.exchange_done = Contact.WAITING;
		  Contact newContact = createContact(contact);
		  addExchangeWaitingContact(newContact, keypair);
		  
		  return newContact;
	  }
	  
	  /**
	   * Creates a new contact in database with state "waiting for keyexchange".
	   * @param contact The contact to add
	   * @param keypair The keypair.
	   */
	  public void addExchangeWaitingContact(Contact contact, DHKeyPair keypair){
		  ContentValues values = new ContentValues();
		  values.put(DatabaseController.COLUMN_USER_ID, contact.id);
		  values.put(DatabaseController.COLUMN_EXCHANGE_PRIVATE_KEY, keypair.keypair.getPrivate().getEncoded());
		  values.put(DatabaseController.COLUMN_EXCHANGE_PUBLIC_KEY, keypair.keypair.getPublic().getEncoded());
		  database.insert(DatabaseController.TABLE_EXCHANGE_WAITING, null, values);
		  return;
	  }
	  
	  /**
	   * Gets the keypair of the contact with given contacts ID. If the ID is not valid it returns null.
	   * @param contactID The ID of the contact to get the keypair from.
	   * @return The keypair.
	   */
	  public DHKeyPair getExchangeWaitingKeyPair(int contactID){
		  DHKeyPair keypair = null;
		  Cursor cursor = database.query(DatabaseController.TABLE_EXCHANGE_WAITING, exchangeWaitingColumns, DatabaseController.COLUMN_USER_ID + "=?", new String[]{""+contactID}, null, null, null);
		  
		  if(cursor.moveToNext()){
				keypair = cursorToKeypair(cursor);
		  }
		  cursor.close();
		  return keypair;
	  }
	  
	  /**
	   * Removes a waiting contact with given contact ID from the database. 
	   * @param contactID The ID of the contact.
	   * @return True, if the contact exists and was sucesfully removed, otherwise false.
	   */
	  public boolean removeExchangeWaitingKeyPair(int contactID){
		  return database.delete(DatabaseController.TABLE_EXCHANGE_WAITING, DatabaseController.COLUMN_USER_ID + "=?", new String[]{""+contactID}) > 0 ? true : false;
	  }
	  

	private DHKeyPair cursorToKeypair(Cursor cursor) {
		DHKeyGen keygen = new DHKeyGen();
		KeyPair keypair = null;
		
		KeyFactory keyfactory;
		try {
			keyfactory = KeyFactory.getInstance("DH");
			KeySpec pubKeySpec = new X509EncodedKeySpec(cursor.getBlob(2));
			KeySpec priKeySpec = new PKCS8EncodedKeySpec(cursor.getBlob(1));
			
			PublicKey pubKey = keyfactory.generatePublic(pubKeySpec);
			PrivateKey priKey = keyfactory.generatePrivate(priKeySpec);
			
			keypair = new KeyPair(pubKey, priKey);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		
		return keygen.new DHKeyPair(keypair, Constants.p, Constants.g);
	}

	/**
	 * Deletes the given contact from the database and removes all relations to other databases. After this 
	 * operation the contact was completely removed from all databases.
	 * @param contact The contact to remove.
	 * @return True, if the contact exists and the operation was successfully done. False otherwise.
	 */
	public boolean deleteContact(Contact contact) {
	  long id = contact.id;
	  if(database.delete(DatabaseController.TABLE_CONTACTS, DatabaseController.COLUMN_USER_ID
	      + " =?", new String[]{""+id}) > 0){
	  	boolean result = true;
	   	if(contact.exchange_done == Contact.ACCEPTED){
	   		PhonenumberDataSource phonenumbers = new PhonenumberDataSource(context);
	   		phonenumbers.open();
	   		result = (phonenumbers.deletePhoneNumber(contact.phonenumber) > 0);
	   		phonenumbers.close();
	   	}
	   	return result;
	  }
	  return false;
	}
	
	/**
	 * Updates the database with the attributes of newContact to oldContact. The new Contact gets the ID of the old one. 
	 * @param newContact The updated contact.
	 * @param oldContact The contact to update.
	 * @return True, if everything was successfully done, false otherwise.
	 */
	public boolean updateContact(Contact newContact, Contact oldContact) {
		ContentValues values = new ContentValues();
		values.put(DatabaseController.COLUMN_USER_NAME, newContact.name);
		values.put(DatabaseController.COLUMN_USER_EMAIL, newContact.email);
		values.put(DatabaseController.COLUMN_USER_PHONE, newContact.phonenumber);
		values.put(DatabaseController.COLUMN_SHARED_KEY,
				Base64.encodeToString(newContact.secret, Base64.DEFAULT));
		boolean success = (database.update(DatabaseController.TABLE_CONTACTS,
				values, DatabaseController.COLUMN_USER_ID + "=?",
				new String[] { "" + newContact.id }) == 1);
		
		if(newContact.exchange_done == Contact.ACCEPTED){
		// add contact to phone numbers database
			if(!newContact.phonenumber.equals(oldContact.phonenumber)){
				PhonenumberDataSource phonenumbers = new PhonenumberDataSource(context);
				phonenumbers.open();
				success &= phonenumbers.updatePhoneNumber(newContact.phonenumber, oldContact.phonenumber);
				System.out.println("Success: "+success);
				phonenumbers.close();
			}
		}
		
		return success;
	}
	
	/**
	 * Changes the exchange state in the database.
	 * @param contact The contact to thange its state.
	 * @return True, if everything was successfully done, false otherwise.
	 */
	public boolean updateExchangeState(Contact contact){
		ContentValues values = new ContentValues();
		values.put(DatabaseController.COLUMN_EXCHANGE_DONE, contact.exchange_done);
		boolean success = (database.update(DatabaseController.TABLE_CONTACTS,
				values, DatabaseController.COLUMN_USER_ID + "=?",
				new String[] { "" + contact.id }) == 1);
		
		if (contact.exchange_done == Contact.ACCEPTED) {
			// add contact to phone numbers database
			PhonenumberDataSource phonenumbers = new PhonenumberDataSource(
					context);
			phonenumbers.open();
			long result = phonenumbers
					.createPhoneNumber(contact.phonenumber);
			phonenumbers.close();

			success &= result > 0 ? true : false;
		}
		return success;
	}
	  
	/**
	 * Getter for the contacts. Returns the first contact with given phone number.
	 * @param phonenumber The phonenumber of the contact.
	 * @return The contact or null, if it does not exist.
	 */
	  public Contact getContact(String phonenumber){
		  Contact contact = null;
//		  Cursor cursor = database.rawQuery("Select " + DatabaseController.TABLE_CONTACTS + "."+ DatabaseController.COLUMN_USER_ID
//				  	+ " , " + DatabaseController.COLUMN_USER_NAME
//					+ " , " + DatabaseController.COLUMN_PHONE
//					+ " , " + DatabaseController.COLUMN_SHARED_KEY
//					+ " FROM " + DatabaseController.TABLE_CONTACTS
//					+ " WHERE " + DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_PHONE
//					+ " = '" + phonenumber + "'"
//					, null);
		  
		  Cursor cursor = database.query(DatabaseController.TABLE_CONTACTS, contactColumns, DatabaseController.COLUMN_USER_PHONE + "=?", new String[]{phonenumber}, null, null, null);
		  
			if(cursor.moveToNext()){
				contact = cursorToContact(cursor);
//				contact.id = cursor.getLong(0);
//				contact.name = cursor.getString(1);
//				contact.phonenumber = cursor.getString(2).substring(1, cursor.getString(2).length()-1);
//				contact.secret = Base64.decode(cursor.getString(3), Base64.DEFAULT);
				
//				return contact;
			}
			cursor.close();
			
			return contact;
	  }
	  
	  /**
	   * Getter for a contact with given contact ID.
	   * @param ID The ID of the contact what was searched for.
	   * @return The contact or null if it does not exist.
	   */
	  public Contact getContact(long ID){
		  Contact contact = null;
		  Cursor cursor = database.query(DatabaseController.TABLE_CONTACTS, contactColumns, DatabaseController.COLUMN_USER_ID + "=?", new String[]{""+ID}, null, null, null);
		  
			if(cursor.moveToNext()){
				contact = cursorToContact(cursor);
			}
			cursor.close();
			
			return contact;
	}
	
	  /**
	   * Getter for a contact to a conversation.
	   * @param conversation The cnversation of the contact.
	   * @return The contact or null.
	   */
	public Contact getContact(Conversation conversation) {
		Cursor cursor = database.rawQuery("SELECT "+DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_USER_ID + 
				", "+ DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_USER_NAME + 
				", "+ DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_USER_PHONE +
				", "+ DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_USER_EMAIL +
				", "+ DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_SHARED_KEY +
				", "+ DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_EXCHANGE_DONE +
				" FROM "+ DatabaseController.TABLE_CONTACTS + " JOIN ( SELECT "+
				DatabaseController.TABLE_HAD + "."+DatabaseController.COLUMN_USER_ID + " AS temp"+
				" FROM "+ DatabaseController.TABLE_HAD +
				" WHERE "+ DatabaseController.COLUMN_CONVERSATION_ID + " =  ? )"+
				" ON "+ DatabaseController.TABLE_CONTACTS+ "."+DatabaseController.COLUMN_USER_ID + " = temp"
				 , new String[]{""+conversation.id});
		
		Contact contact = null;
		if(cursor.moveToNext()){
			contact = cursorToContact(cursor);
		}
		
		cursor.close();
		
		return contact;
	}
	
	/**
	 * Loads all contacts from database. Depending on exchange_done, it loads only contacts where the key exchange was successful or not.
	 * @param exchange_done - true: only contacts with done key exchange
	 * @return a list of all contacts
	 */
	public List<Contact> getAllContacts(boolean exchange_done) {
		List<Contact> contacts = new ArrayList<Contact>();

    Cursor cursor = database.query(DatabaseController.TABLE_CONTACTS,
	        contactColumns, null, null, null, null, null);

	    cursor.moveToFirst();
		if (exchange_done) {
			while (!cursor.isAfterLast()) {
				Contact contact = cursorToContact(cursor);
				if (contact.exchange_done == Contact.ACCEPTED)
					contacts.add(contact);
				cursor.moveToNext();
			}
		} else {
			while (!cursor.isAfterLast()) {
				Contact contact = cursorToContact(cursor);
				contacts.add(contact);
				cursor.moveToNext();
			}
		}
	    // Make sure to close the cursor
	    cursor.close();
	    return contacts;
	  }
	  
	/**
	 * Loads all contacts with a conversation into a list.
	 * @return The list of all contacts with a conversation.
	 */
	public List<Contact> getAllContactsWithConversations() {
		List<Contact> contacts = new ArrayList<Contact>();
		
		Cursor cursor = database.rawQuery("Select " + DatabaseController.TABLE_CONTACTS + "."+ DatabaseController.COLUMN_USER_ID
				+ " , " + DatabaseController.COLUMN_USER_NAME
				+ " , " + DatabaseController.COLUMN_USER_PHONE
				+ " , " + DatabaseController.COLUMN_USER_EMAIL
				+ " , " + DatabaseController.COLUMN_SHARED_KEY
				+ " , " + DatabaseController.COLUMN_EXCHANGE_DONE
				+ " FROM " + DatabaseController.TABLE_CONTACTS
				+ " JOIN " + DatabaseController.TABLE_HAD
				+ " ON "+ DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_USER_ID
				+ " = "+ DatabaseController.TABLE_HAD + "." + DatabaseController.COLUMN_USER_ID
				+ " JOIN ( SELECT " + DatabaseController.COLUMN_CONVERSATION_ID + ", " + DatabaseController.COLUMN_CONVERSATION_DATE
				+ " FROM " + DatabaseController.TABLE_CONVERSATION + ") AS A"
				+ " ON A." + DatabaseController.COLUMN_CONVERSATION_ID + " = " + DatabaseController.TABLE_HAD + "."+DatabaseController.COLUMN_CONVERSATION_ID
				+ " ORDER BY A."+ DatabaseController.COLUMN_CONVERSATION_DATE
				, null);
		
		while (cursor.moveToNext()) {
			Contact contact = cursorToContact(cursor);
			if(!contacts.contains(contact)){contacts.add(contact);}
		}
		// Make sure to close the cursor
		cursor.close();
		return contacts;
	}
	
	/**
	 * Gets the ID of the first contact with given phone number. 
	 * @param originNumber The phone number of the contact.
	 * @return The contact ID or -1 if it does not exist.
	 */
	public int getContactID(String originNumber){
		Cursor cursor = database.rawQuery("Select " + DatabaseController.TABLE_CONTACTS + "."+ DatabaseController.COLUMN_USER_ID
				+ " FROM " + DatabaseController.TABLE_CONTACTS
				+ " WHERE " + DatabaseController.TABLE_CONTACTS + "." + DatabaseController.COLUMN_USER_PHONE
				+ " = "+ originNumber
				, null);
		
		if(cursor.moveToFirst()){
			return cursor.getInt(0);
		}
		
		return -1;
	}

	  private Contact cursorToContact(Cursor cursor) {
	    Contact contact = new Contact(cursor.getString(1), cursor.getString(2));
	    contact.id = cursor.getLong(0);
	    contact.email = cursor.getString(3);
	    contact.secret = Base64.decode(cursor.getString(4), Base64.DEFAULT);
	    contact.exchange_done = cursor.getInt(5);
	    return contact;
	  }
	  
	  /**
	   * This Method creates a new row in the sqlite database for the given conversation.
	   * @param conversation the conversation to add to the database
	   * @return the same conversation with updated conversation ID
	   */
	  public Conversation createConversation(Conversation conversation) {
		    ContentValues values = new ContentValues();
		    values.put(DatabaseController.COLUMN_CONVERSATION_DATE, conversation.date.getTimeInMillis());
		    values.put(DatabaseController.COLUMN_CONVERSATION_MESSAGE, conversation.message);
		    values.put(DatabaseController.COLUMN_CONVERSATION_SENT, conversation.sent);
		    values.put(DatabaseController.COLUMN_CONVERSATION_STATE, conversation.status);
		    long insertId = database.insert(DatabaseController.TABLE_CONVERSATION, null,
		        values);
		    Cursor cursor = database.query(DatabaseController.TABLE_CONVERSATION,
		        conversationColumns, DatabaseController.COLUMN_CONVERSATION_ID + " = " + insertId, null,
		        null, null, null);
		    cursor.moveToFirst();
		    Conversation newConversation = cursorToConversation(cursor);
		    newConversation.contactid = conversation.contactid;
		    cursor.close();
		    //update had table
		    values = new ContentValues();
		    values.put(DatabaseController.COLUMN_USER_ID, newConversation.contactid);
		    values.put(DatabaseController.COLUMN_CONVERSATION_ID, newConversation.id);
		    database.insert(DatabaseController.TABLE_HAD, null, values);
		    
		    return newConversation;
		  }

	  /**
	   * Deletes a conversation from the conversation database. A conversation is one message, not a flow of messages.
	   * @param conversation The conversation to remove.
	   * @return The number of rows affected.
	   */
	public int deleteConversation(Conversation conversation) {
	    long id = conversation.id;
	    System.out.println("Conversation deleted with id: " + id);
	    return database.delete(DatabaseController.TABLE_CONVERSATION, DatabaseController.COLUMN_CONVERSATION_ID
	        + " = " + id, null);
	}
	
	/**
	 * Deletes all SMS that were received or sent to the given contact. The SMS would deleted from the table conversation and from table had.
	 * @param contact - the contact deleting all messages from
	 * @return true, if the action was successful
	 */
	public boolean deleteConversations(Contact contact){
		Cursor cursor = database.rawQuery(
				"Select " + DatabaseController.TABLE_HAD + "." + DatabaseController.COLUMN_CONVERSATION_ID
				+ " FROM " + DatabaseController.TABLE_HAD
				+ " JOIN " + DatabaseController.TABLE_CONVERSATION
				+ " ON "+ DatabaseController.TABLE_HAD + "." + DatabaseController.COLUMN_CONVERSATION_ID
				+ " = "+ DatabaseController.TABLE_CONVERSATION + "." + DatabaseController.COLUMN_CONVERSATION_ID
				+ " WHERE " + DatabaseController.COLUMN_USER_ID 
				+ " = " + contact.id, null);
		
		int deleted_rows = 0;
		
		while (cursor.moveToNext()){
			deleted_rows += database.delete(
					DatabaseController.TABLE_CONVERSATION, cursor.getInt(0) 
					+ " = " 
					+ DatabaseController.COLUMN_CONVERSATION_ID, null);
		}

		if(deleted_rows == 0) return false;
		
		cursor.moveToPosition(-1);
		
		while (cursor.moveToNext()){
			deleted_rows -= database.delete(
					DatabaseController.TABLE_HAD, cursor.getInt(0) 
					+ " = " 
					+ DatabaseController.COLUMN_CONVERSATION_ID, null);
		}
		return (deleted_rows == 0);
	}
	
	/**
	 * Gives a list of all conversations in database.
	 * @return List of all conversations.
	 */
	public List<Conversation> getAllConversations() {
	    List<Conversation> conversations = new ArrayList<Conversation>();

	    Cursor cursor = database.query(DatabaseController.TABLE_CONVERSATION,
	        conversationColumns, null, null, null, null, null);

	    while (cursor.moveToNext()) {
	      Conversation conversation = cursorToConversation(cursor);
	      conversations.add(conversation);
	    }
	    // Make sure to close the cursor
	    cursor.close();
	    return conversations;
	}
	
	/**
	 * Gets a list of all conversations of the given contact sorted by date in descending order.
	 * @param contact - the contact to look for its conversations
	 * @return a list of all conversations sorted by date in descending order
	 */
	public List<Conversation> getAllConversations(Contact contact) {
	    List<Conversation> conversations = new ArrayList<Conversation>();

	    Cursor cursor = database.rawQuery(
				"Select " + DatabaseController.TABLE_CONVERSATION + "."+ DatabaseController.COLUMN_CONVERSATION_ID
				+ " , " + DatabaseController.COLUMN_CONVERSATION_DATE
				+ " , " + DatabaseController.COLUMN_CONVERSATION_MESSAGE
				+ " , " + DatabaseController.COLUMN_CONVERSATION_SENT
				+ " , " + DatabaseController.COLUMN_CONVERSATION_STATE
				+ " FROM " + DatabaseController.TABLE_HAD
				+ " JOIN " + DatabaseController.TABLE_CONVERSATION
				+ " ON "+ DatabaseController.TABLE_HAD + "." + DatabaseController.COLUMN_CONVERSATION_ID
				+ " = "+ DatabaseController.TABLE_CONVERSATION + "." + DatabaseController.COLUMN_CONVERSATION_ID
				+ " WHERE " + DatabaseController.COLUMN_USER_ID 
				+ " = ?"
				+ " ORDER BY "+DatabaseController.COLUMN_CONVERSATION_DATE + " DESC"
				, new String[]{""+contact.id});
	    
	    while (cursor.moveToNext()) {
	      Conversation conversation = cursorToConversation(cursor);
	      conversation.contactid = contact.id;
	      conversations.add(conversation);
	    }
	    // Make sure to close the cursor
	    cursor.close();
	    return conversations;
	}
	
	/**
	 * Gives a list of all conversations of the outbox.
	 * @return List of all outbox conversations.
	 */
	public ArrayList<Conversation> getAllOutboxConversations() {
		ArrayList<Conversation> conversations = new ArrayList<Conversation>();
		
		Cursor cursor = database.query(DatabaseController.TABLE_CONVERSATION,
				conversationColumns,
				DatabaseController.COLUMN_CONVERSATION_STATE + "= ?",
				new String[] { "" + Conversation.SMS_STATUS_OUTBOX }, null,
				null, null);
		
		while(cursor.moveToNext()){
			conversations.add(cursorToConversation(cursor));
		}
		
		cursor.close();
		
		return conversations;
	}
	
	/**
	 * Gives a special conversation with given conversation ID.
	 * @param conversationID The ID of the convresation.
	 * @return A conversation or null, if it does not exist.
	 */
	public Conversation getConversation(long conversationID) {
		Cursor cursor = database.query(DatabaseController.TABLE_CONVERSATION,
				conversationColumns, DatabaseController.COLUMN_CONVERSATION_ID
						+ " = ?", new String[] { "" + conversationID }, null,
				null, null);
		Conversation conversation = null;
		if(cursor.moveToNext()){
			conversation = cursorToConversation(cursor);
		}
		cursor.close();
		return conversation;
	}
	
	private Conversation cursorToConversation(Cursor cursor) {
		Conversation conversation = new Conversation(cursor.getLong(1), cursor.getString(2));
		conversation.id = cursor.getLong(0);
		conversation.sent = (cursor.getInt(3) == 1);
		conversation.status = (cursor.getInt(4));
		return conversation;
	}
	
	/**
	 * Updates the state of the conversation towards the state given in argument.
	 * @param sms The sms with new state.
	 * @return True if it was successfull, false otherwise.
	 */
	public boolean updateConversationState(SentSMS sms){
		ContentValues values = new ContentValues();
		values.put(DatabaseController.COLUMN_CONVERSATION_STATE, sms.State);
		return (database.update(DatabaseController.TABLE_CONVERSATION, values, DatabaseController.COLUMN_CONVERSATION_ID + "=?", new String[]{""+sms.ID}) == 1);
	}

	public boolean isOpen() {
		return dbHelper.isOpen();
	}

	/**
	 * This method creates all dependent database entries in other databases.
	 */
	public long postProcessImport() {
		//first: delete all old data
		PhonenumberDataSource phonenumbers = new PhonenumberDataSource(context);
	    phonenumbers.open();
	    
	    phonenumbers.deleteAllPhonenumbers();
	    
	    //now we can fill the new ones in
	    long result = 0l;
	    Cursor cursor = database.query(DatabaseController.TABLE_CONTACTS, new String[]{DatabaseController.COLUMN_USER_PHONE}, null, null, null, null, null);
	    while(cursor.moveToNext()){
	    	result += (phonenumbers.createPhoneNumber(cursor.getString(0)) >= 0) ? 1 : 0;
	    }
	    phonenumbers.close();
	    
	    return result;
	}

	/**
	 * This method changes the password of the enciphered database.
	 * @param newPassword The new password.
	 */
	public void changeKey(byte[] newPassword) {
		dbHelper.changeKey(Base64.encode(newPassword, Base64.DEFAULT));
	}

	/**
	 * After a key request, a new contact should be added to the database. If the contact
	 * sends a message, we have to look if it is an answer or a new key.
	 * @param phone The phonenumber of the contact.
	 * @return The contact.
	 */
	public Contact isAnswerToKeyRequest(String phone) {
		Contact contact = null;
		Cursor cursor = database.query(DatabaseController.TABLE_CONTACTS, contactColumns, DatabaseController.COLUMN_USER_PHONE + "=?", new String[]{phone}, null, null, null);
		if(cursor.moveToNext()){
			contact = cursorToContact(cursor);
		}
		cursor.close();
		return contact;
	}
}
