package com.mslab.encryptsms;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHPublicKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.DHKeyGen;
import com.mslab.encryptsms.misc.DHKeyGen.DHKeyPair;
import com.mslab.encryptsms.misc.PhonenumberDataSource;
import com.mslab.encryptsms.misc.ReceivedSMS;
import com.mslab.encryptsms.misc.SMSDataStorage;
import com.mslab.encryptsms.misc.SMSNotifierHelper;
import com.mslab.encryptsms.receiver.SMSDeliveredReceiver;
import com.mslab.encryptsms.receiver.SMSSentReceiver;
import com.mslab.encryptsms.services.SessionManagerService;
import com.mslab.smsutils.SMSUtils;


/**
 * This activity shows the screen to exchange user's key by sms.
 * @author Paul Kramer
 *
 */
public class SMSKeyExchangeActivity extends Activity {
	
	//UI Elements
	private AutoCompleteTextView mNameText;
	private EditText mMailText;
	private AutoCompleteTextView mPhoneText;
	private Button mPerformExchangeBtn;
	
	private Button mPanicButton;
	
	//load context
	private Context context;
	
	//load password
	private byte[] password;
	
	//load bundled data
	private Bundle bundle;
	
	// contacts list
	public ArrayList<String> c_Name = new ArrayList<String>();
	public ArrayList<String> c_Number = new ArrayList<String>();
	String[] name_Val = null;
	String[] phone_Val = null;
	
	//ProgressDialog for SMS sending
	ProgressDialog pd = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_smskey_exchange);
		
		//init context
		context = this;
		
		//init bundled data
		bundle = getIntent().getExtras();
		
		//load password
		password = bundle.getByteArray(Constants.DB_PASSWORD_HASH);
		
		//init UI elements
		mNameText = (AutoCompleteTextView) findViewById(R.id.sms_key_exchange_name_input);
		mMailText = (EditText) findViewById(R.id.sms_key_exchange_email_input);
		mPhoneText = (AutoCompleteTextView) findViewById(R.id.sms_key_exchange_phone_input);
		
		mPerformExchangeBtn = (Button) findViewById(R.id.sms_key_exchange_keyexchange_btn);
		mPanicButton = (Button) findViewById(R.id.sms_key_exchange_panic_button);
		
		mNameText.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				mPhoneText.setText(c_Number.get(c_Name.indexOf(mNameText.getText().toString())));
				System.out.println("selected: "+mNameText.getText().toString());
				
			}
		});
		
		mPhoneText.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				mNameText.setText(c_Name.get(c_Number.indexOf(mPhoneText.getText().toString())));
			}
		});
		
		mPerformExchangeBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pd = ProgressDialog
						.show(context,
								"",
								getResources()
										.getString(
												R.string.sms_key_exchange_progress_text),
								true);
				pd.setCancelable(false);
				//really new contact, send own key
				if(bundle.containsKey(Constants.SMS_KEY)){
					ReceivedSMS rsms = bundle.getParcelable(Constants.SMS_KEY);
					pd.show();
					SendOwnKeyTask sokt = new SendOwnKeyTask();
					sokt.execute(rsms);
				}else if(bundle.containsKey(Constants.CONTACT)){
					//rekey known contact
					pd.show();
					ReKeyTask task = new ReKeyTask();
					Contact contact = bundle.getParcelable(Constants.CONTACT); 
					task.execute(contact);
				}else{
					pd.show();
					KeyPairGeneratorTask task = new KeyPairGeneratorTask();
					task.execute(new Contact(mNameText.getText().toString(), mPhoneText.getText().toString()));
				}
				
			}
		});
		
		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
	}
	
	@Override
	protected void onResume() {

		//load contacts from contacts list
		loadContacts(mNameText, mPhoneText);
		
		//handle sms key exchange request
		handleKeyExchangeRequest(bundle);
		
		//load contact
		loadContactForNewKey(bundle);

		// send user alive message
		SessionManagerService.alive(context);

		//remove notification, if it is necessary
		SMSNotifierHelper.cancelNotification(context);
		
		super.onResume();
	}
	
	private void loadContactForNewKey(Bundle bundle) {

		if(!bundle.containsKey(Constants.CONTACT)) return;
		Contact contact = bundle.getParcelable(Constants.CONTACT);
		mNameText.setText(contact.name);
		mPhoneText.setText(contact.phonenumber);
		mMailText.setText(contact.email);
		mPerformExchangeBtn.requestFocus();
		
	}

	private void handleKeyExchangeRequest(Bundle bundle) {

		if(!bundle.containsKey(Constants.SMS_KEY_EXCHANGE)) return;
		
		final ReceivedSMS rsms = bundle.getParcelable(Constants.SMS_KEY);
		
		//look for known contact
		PhonenumberDataSource phones = new PhonenumberDataSource(context);
		phones.open();
		String knownContact = phones.containsPhoneNumber(rsms.Phone);
		phones.close();
		
		if(knownContact == null){
			//potentially it can be the answer of a key exchange request
			ConversationsDataSource dataSource = new ConversationsDataSource(context);
			dataSource.open(password);
			Contact contact = dataSource.isAnswerToKeyRequest(rsms.Phone);
			if(contact != null && contact.exchange_done == Contact.WAITING){
				//answer to a key request
				byte[] temp = rsms.Message;
				//cut the detection byte
				byte[] key = new byte[temp.length - 1];
				System.arraycopy(temp, 1, key, 0, key.length);
				PublicKey pubkey = parseKeyFromBytes(key);
				
				
				DHKeyPair ownKeyPair = dataSource.getExchangeWaitingKeyPair((int)contact.id);
				DHKeyGen keygen = new DHKeyGen();
				Contact oldContact = dataSource.getContact(rsms.Phone);
				contact.secret = keygen.getSecret(ownKeyPair.keypair,pubkey);
				contact.exchange_done = Contact.NOT_ACCEPTED;
				dataSource.updateContact(contact, oldContact);
				dataSource.updateExchangeState(contact);
				dataSource.removeExchangeWaitingKeyPair((int)contact.id);
				dataSource.close();

				showKeyExchangeDoneDialog(rsms, contact);
			}else{		
				dataSource.close();
				// new Contact requests key exchange
				int i = 0;
				for (String phone : c_Number) {
					if (phone.replaceAll("\\s","").equals(rsms.Phone))
						break;
					++i;
				}
				if (i < c_Number.size()) {
					// contact known
					mNameText.setText(c_Name.get(i));
					mPhoneText.setText(c_Number.get(i));
				} else {
					// Contact unknown or not in the right format
					mPhoneText.setText(rsms.Phone);
				}
				// perform exchange
				// show information
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(R.string.sms_key_exchange_alert_received_title);
				builder.setCancelable(false);
				String message = context
						.getString(R.string.sms_key_exchange_alert_received_text_1)
						+ " ";
				if (i < c_Number.size()) {
					message += c_Name.get(i) + " ";
				} else {
					message += rsms.Phone + " ";
				}
				message += context
						.getString(R.string.sms_key_exchange_alert_received_text_2)
						+ " \""
						+ context
								.getString(R.string.sms_key_exchange_key_exchange_btn)
						+ "\" "
						+ context
								.getString(R.string.sms_key_exchange_alert_received_text_3);
				builder.setMessage(message);
				builder.setNeutralButton(R.string.sms_key_exchange_alert_btn,
						new AlertDialog.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});

				builder.create().show();
			}
		} else {
			// known contact or just a new key?
			ConversationsDataSource dataSource = new ConversationsDataSource(
					context);
			dataSource.open(password);

			Contact contact = dataSource.getContact(rsms.Phone);

			if (contact.exchange_done > Contact.WAITING) {
				//fill in contact information
				mMailText.setText(contact.email);
				mNameText.setText(contact.name);
				mPhoneText.setText(contact.phonenumber);

				//new key info
				//show information
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(R.string.sms_key_exchange_alert_received_title);
				String message = context.getString(R.string.sms_key_exchange_alert_received_text_1) + " ";
				message += contact.name+ " ";
				message += context.getString(R.string.sms_key_exchange_alert_text_2) + " " + context.getString(R.string.sms_key_exchange_key_exchange_btn) + " " + context.getString(R.string.sms_key_exchange_alert_received_text_3);
				context.getString(R.string.sms_key_exchange_alert_text_2);
				builder.setMessage(message);
				builder.setNeutralButton(R.string.sms_key_exchange_alert_btn, new AlertDialog.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

				builder.create().show();
				
			}else{
				//existing contact gets new key
				byte[] temp = rsms.Message;
				//cut the detection byte
				byte[] key = new byte[temp.length - 1];
				System.arraycopy(temp, 1, key, 0, key.length);
				PublicKey pubkey = parseKeyFromBytes(key);
				DHKeyPair ownKeyPair = dataSource.getExchangeWaitingKeyPair((int)contact.id);
				DHKeyGen keygen = new DHKeyGen();
				Contact oldContact = dataSource.getContact(rsms.Phone);
				contact.secret = keygen.getSecret(ownKeyPair.keypair,pubkey);
				contact.exchange_done = Contact.NOT_ACCEPTED;
				dataSource.updateContact(contact, oldContact);
				dataSource.updateExchangeState(contact);
				dataSource.removeExchangeWaitingKeyPair((int)contact.id);
				//show dialog as confirmation
				showKeyExchangeDoneDialog(rsms, contact);
			}
			
			dataSource.close();
		}
	}

	private void loadContacts(AutoCompleteTextView Names, AutoCompleteTextView Phones) {
		ContentResolver cr = getContentResolver();
		
		Uri contacts = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;//Uri.parse("content://icc/adn");
		
		Cursor managedCursor = cr.query(contacts, null, null, null, null);
		
		if (managedCursor.moveToFirst()) {
			
			String contactname;
			String cphoneNumber;
			
			int nameColumn = managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
			int phoneColumn = managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
			
			do {
				// Get the field values
				contactname = managedCursor.getString(nameColumn);
				cphoneNumber = managedCursor.getString(phoneColumn);
				if ((contactname != " " || contactname != null)
						&& (cphoneNumber != " " || cphoneNumber != null)) {
					
					c_Name.add(contactname);
					c_Number.add(cphoneNumber);
				}
				
			} while (managedCursor.moveToNext());
			
		}
		
		//convert ArrayList to Autocomplete compatible list
		name_Val = (String[]) c_Name.toArray(new String[c_Name.size()]);
		phone_Val = (String[]) c_Number.toArray(new String[c_Name.size()]);
		ArrayAdapter<String> nameAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, name_Val);
		Names.setAdapter(nameAdapter);
		
		ArrayAdapter<String> phoneAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, phone_Val);
		Phones.setAdapter(phoneAdapter);
	}
	
	private class KeyPairGeneratorTask extends AsyncTask<Contact, Void, Map<Contact, DHKeyPair>>{

		@Override
		protected Map<Contact, DHKeyPair> doInBackground(Contact... params) {

			DHKeyGen keygen = new DHKeyGen();
			DHKeyPair keypair = keygen.getKeyPair(Constants.p, Constants.g);
			
			ConversationsDataSource dataSource = new ConversationsDataSource(context);
			dataSource.open(password);
			dataSource.createContact(params[0], keypair);
			dataSource.close();
			
			HashMap<Contact, DHKeyPair> result = new HashMap<Contact, DHKeyGen.DHKeyPair>();
			result.put(params[0], keypair);
			
			return result;
		}
		
		@Override
		protected void onPostExecute(Map<Contact, DHKeyPair> result) {
			Map.Entry<Contact, DHKeyPair> entry = result.entrySet().iterator().next();
			
			//generating keypair done, send sms to contact
			byte[] temp = ((DHPublicKey)(entry.getValue().keypair.getPublic())).getY().toByteArray();
			
			//get instance of SMS Manager
			SmsManager sms = SmsManager.getDefault();
			
			//apply zero encoding
//			smscontent = SMSUtils.appendZeroEncoding(smscontent);
			//convert to gsm compatible chars
			
			//apply Base123 encoding
			temp = SMSUtils.encodeBase123(temp).getBytes();
			
			//append detection bytes
			byte[] smscontent = new byte[temp.length+1];
			System.arraycopy(temp, 0, smscontent, 1, temp.length);
			smscontent[0] = ReceivedSMS.KEY_EXCHANGE_BYTE;
			
			//split into sms
			ArrayList<String> parts = sms.divideMessage(new String(smscontent));
			
			//prepare SMS Intents
			ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
			ArrayList<PendingIntent> deliveredPIs = new ArrayList<PendingIntent>();
			
			//prepare intents
			for (int i = 0; i < parts.size(); ++i) {
				bundle.putInt("part", i);
				
				Intent sentIntent = new Intent(SMSSentReceiver.SMS_SENT);
				sentIntent.putExtras(bundle);
				Intent deliveredIntent = new Intent(
						SMSDeliveredReceiver.SMS_DELIVERED);
				deliveredIntent.putExtras(bundle);
				
				sentPIs.add(PendingIntent.getBroadcast(context, 0, sentIntent,
						PendingIntent.FLAG_ONE_SHOT));
				deliveredPIs.add(PendingIntent.getBroadcast(context, 1,
						deliveredIntent, PendingIntent.FLAG_ONE_SHOT));
			}
			
			//send message
			sms.sendMultipartTextMessage(entry.getKey().phonenumber, null, parts, sentPIs,
					deliveredPIs);
			
			//close progress dialog
			pd.dismiss();
			
			//show information
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.sms_key_exchange_alert_title);
			String message = context.getString(R.string.sms_key_exchange_alert_text_1) + " "+entry.getKey().name + " "+
			context.getString(R.string.sms_key_exchange_alert_text_2);
			builder.setMessage(message);
			builder.setNeutralButton(R.string.sms_key_exchange_alert_btn, new AlertDialog.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});

			builder.create().show();
			super.onPostExecute(result);
		}
		
	}
	/**
	 * Sends users own Key and saves the secret.
	 * @author Paul
	 *
	 */
	private class SendOwnKeyTask extends AsyncTask<ReceivedSMS, Void, Map<ReceivedSMS, Contact>>{

		@Override
		protected Map<ReceivedSMS, Contact> doInBackground(ReceivedSMS... params) {
			byte[] temp = params[0].Message;
			//cut the detection byte off
			byte[] foreignKey = new byte[temp.length - 1];
			System.arraycopy(temp, 1, foreignKey, 0,
					foreignKey.length);
			
			
			PublicKey pubkey = parseKeyFromBytes(foreignKey);
			
			DHKeyGen keygen = new DHKeyGen();
			DHKeyPair ownKeyPair = keygen.getKeyPair(Constants.p, Constants.g);
			
			// look for known contact
			PhonenumberDataSource phones = new PhonenumberDataSource(context);
			phones.open();
			String knownContact = phones.containsPhoneNumber(params[0].Phone);
			phones.close();
			
			Contact contact = null;
			
			if (knownContact != null) {
				ConversationsDataSource dataSource = new ConversationsDataSource(
						context);
				dataSource.open(password);
				
				contact = dataSource.getContact(params[0].Phone);
				
				if (contact.exchange_done > Contact.WAITING) {
					// just a new key
//					temp = SMSUtils.compressBytesTo7BitPerCharEncoding(params[0].Message.getBytes());
//					byte[] key = new byte[temp.length - 2];
//					System.arraycopy(temp, 2, key, 0,
//							key.length);
					
					Contact newContact = dataSource.getContact(params[0].Phone);
					newContact.secret = keygen.getSecret(ownKeyPair.keypair,
							pubkey);
					newContact.exchange_done = Contact.NOT_ACCEPTED;
					
					// store new key
					dataSource.updateContact(newContact, contact);
					dataSource.updateExchangeState(newContact);
					dataSource.close();
					
					//remove the key exchange request sms
					SMSDataStorage datastorage = new SMSDataStorage(context);
					datastorage.open();
					datastorage.deleteReceivedSMS(params[0]);
					datastorage.close();
					
					//set te contact for new contact
					contact = newContact;
				}
			} else {
				
				// generate new contact
				contact = new Contact(mNameText.getText().toString(),
						mPhoneText.getText().toString());
				contact.exchange_done = Contact.NOT_ACCEPTED;
				contact.secret = keygen.getSecret(ownKeyPair.keypair, pubkey);
				ConversationsDataSource dataSource = new ConversationsDataSource(
						context);
				dataSource.open(password);
				contact = dataSource.createContact(contact);
				dataSource.close();
			}
			
			// send own key
			// get instance of SMS Manager
			SmsManager sms = SmsManager.getDefault();
			
			//mark as key
			temp = ((DHPublicKey)ownKeyPair.keypair.getPublic()).getY().toByteArray(); //reuse the variable
			
			//encode to Base123
			temp = SMSUtils.encodeBase123(temp).getBytes();
			
			byte[] smscontent = new byte[temp.length+1];
			//set key exchange bytes
			smscontent[0] = ReceivedSMS.KEY_EXCHANGE_BYTE;
			//copy public key
			System.arraycopy(temp, 0, smscontent, 1, temp.length);
			

			//expand bytes to sms compatible values
//			smscontent = SMSUtils.expandBytesTo8BitPerChar(smscontent);
			
			//apply zero encoding
//			smscontent = SMSUtils.appendZeroEncoding(smscontent);
			//convert to gsm compatible chars
			ArrayList<String> parts = sms.divideMessage(new String(smscontent));
			
			//prepare SMS Intents
			ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
			ArrayList<PendingIntent> deliveredPIs = new ArrayList<PendingIntent>();
			
			//prepare intents
			for (int i = 0; i < parts.size(); ++i) {
				bundle.putInt("part", i);
				
				Intent sentIntent = new Intent(SMSSentReceiver.SMS_SENT);
				sentIntent.putExtras(bundle);
				Intent deliveredIntent = new Intent(
						SMSDeliveredReceiver.SMS_DELIVERED);
				deliveredIntent.putExtras(bundle);
				
				sentPIs.add(PendingIntent.getBroadcast(context, 0, sentIntent,
						PendingIntent.FLAG_ONE_SHOT));
				deliveredPIs.add(PendingIntent.getBroadcast(context, 1,
						deliveredIntent, PendingIntent.FLAG_ONE_SHOT));
			}
			
			//send message
			sms.sendMultipartTextMessage(contact.phonenumber, null, parts, sentPIs,
					deliveredPIs);
			HashMap<ReceivedSMS, Contact> result = new HashMap<ReceivedSMS, Contact>();
			result.put(params[0], contact);
			return result;
		}
		
		@Override
		protected void onPostExecute( Map<ReceivedSMS, Contact> result) {
			
			pd.dismiss();
			
			Map.Entry<ReceivedSMS, Contact> entry = result.entrySet().iterator().next();
			ReceivedSMS rsms = entry.getKey();
			final Contact contact = entry.getValue();
			//show information
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.sms_key_exchange_alert_send_own_title);
			String message = context.getString(R.string.sms_key_exchange_alert_send_own_text_1);
			message += " " + context.getString(R.string.sms_key_exchange_alert_send_own_text_2);
			message += "\n";
			message += contact.getSecretFingerprint(context);
			message += "\n";
			message += context.getString(R.string.sms_key_exchange_alert_send_own_text_3);
			builder.setMessage(message);
			builder.setCancelable(false);
			builder.setNegativeButton(R.string.sms_key_exchange_alert_neg_btn, new AlertDialog.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			builder.setPositiveButton(R.string.sms_key_exchange_alert_pos_btn, new AlertDialog.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ConversationsDataSource dataSource = new ConversationsDataSource(context);
					dataSource.open(password);
					contact.exchange_done = Contact.ACCEPTED;
					dataSource.updateExchangeState(contact);
					dataSource.close();
					finish();
				}
			});
			
			builder.create().show();
			//remove received key exchange sms from storage
			SMSDataStorage datastorage = new SMSDataStorage(context);
			datastorage.open();
			datastorage.deleteReceivedSMS(rsms);
			datastorage.close();
			
			super.onPreExecute();
		}
		
	}

private class ReKeyTask extends AsyncTask<Contact, Void, Contact>{

	@Override
	protected Contact doInBackground(Contact... params) {
		
		DHKeyGen keygen = new DHKeyGen();
		DHKeyPair ownKeyPair = keygen.getKeyPair(Constants.p, Constants.g);
		
		ConversationsDataSource dataSource = new ConversationsDataSource(context);
		dataSource.open(password);
		Contact contact = params[0];
		contact.exchange_done = Contact.WAITING;
		dataSource.updateExchangeState(contact);

		//save the own keypair
		dataSource.addExchangeWaitingContact(contact, ownKeyPair);
		
		//close DB
		dataSource.close();
		
		//send own key
		//get instance of SMS Manager
		SmsManager sms = SmsManager.getDefault();

		//mark as key
		byte[] temp = ((DHPublicKey)ownKeyPair.keypair.getPublic()).getY().toByteArray();
		//encode to Base123
		temp = SMSUtils.encodeBase123(temp).getBytes();
		byte[] smscontent = new byte[temp.length+1];
		System.arraycopy(temp, 0, smscontent, 1, temp.length);
		smscontent[0] = ReceivedSMS.KEY_EXCHANGE_BYTE;
		
		//expand bytes to sms compatible values
//		smscontent = SMSUtils.expandBytesTo8BitPerChar(smscontent);
		//apply zero encoding
//		smscontent = SMSUtils.appendZeroEncoding(smscontent);
		//convert to gsm compatible chars
		ArrayList<String> parts = sms.divideMessage(new String(smscontent));
		
		//prepare SMS Intents
		ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliveredPIs = new ArrayList<PendingIntent>();
		
		//prepare intents
		for (int i = 0; i < parts.size(); ++i) {
			bundle.putInt("part", i);
			
			Intent sentIntent = new Intent(SMSSentReceiver.SMS_SENT);
			sentIntent.putExtras(bundle);
			Intent deliveredIntent = new Intent(
					SMSDeliveredReceiver.SMS_DELIVERED);
			deliveredIntent.putExtras(bundle);
			
			sentPIs.add(PendingIntent.getBroadcast(context, 0, sentIntent,
					PendingIntent.FLAG_ONE_SHOT));
			deliveredPIs.add(PendingIntent.getBroadcast(context, 1,
					deliveredIntent, PendingIntent.FLAG_ONE_SHOT));
		}
		
		//send message
		sms.sendMultipartTextMessage(contact.phonenumber, null, parts, sentPIs,
				deliveredPIs);
		
		return params[0];
	}
	
	@Override
	protected void onPostExecute(Contact result) {
		//close progress dialog
		pd.dismiss();
		
		//show information
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.sms_key_exchange_alert_title);
		String message = context.getString(R.string.sms_key_exchange_alert_text_1) + " "+result.name + " "+
		context.getString(R.string.sms_key_exchange_alert_text_2);
		builder.setMessage(message);
		builder.setNeutralButton(R.string.sms_key_exchange_alert_btn, new AlertDialog.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});

		builder.create().show();
		super.onPostExecute(result);
	}
	
}
	
	private PublicKey parseKeyFromBytes(byte[] foreignKey){
		PublicKey pubkey = null;
		DHPublicKeySpec keyspec = new DHPublicKeySpec(new BigInteger(foreignKey), Constants.p, Constants.g);
//		X509EncodedKeySpec x509enc = new X509EncodedKeySpec(foreignKey);
		try {
			KeyFactory kf = KeyFactory.getInstance("DH", "BC");
			pubkey = kf.generatePublic(keyspec);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pubkey;
	}
	
	private void showKeyExchangeDoneDialog(final ReceivedSMS rsms, final Contact contact){
		
		//remove the SMS
		SMSDataStorage datastorage = new SMSDataStorage(context);
		datastorage.open();
		System.out.println("Remove SMS: "+datastorage.deleteReceivedSMS(rsms));
		System.out.println("Remove SMS: "+datastorage.deleteReceivedSMS(rsms));
		datastorage.close();
		
		//show the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.sms_key_exchange_alert_send_own_title);
		String message = getString(R.string.sms_key_exchange_alert_done_text_1) + " \"" + contact.name + "\" "+ getString(R.string.sms_key_exchange_alert_done_text_2);
		message += "\n";
		message += getString(R.string.sms_key_exchange_alert_done_text_3);
		message += "\n";
		message += contact.getSecretFingerprint(context);
		message += "\n";
		message += getString(R.string.sms_key_exchange_alert_done_text_4);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.sms_key_exchange_alert_pos_btn,
				new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						ConversationsDataSource dataSource = new ConversationsDataSource(context);
						dataSource.open(password);
						contact.exchange_done = Contact.ACCEPTED;
						dataSource.updateExchangeState(contact);
						dataSource.close();
						finish();
					}
				});
		
		builder.setNegativeButton(R.string.sms_key_exchange_alert_neg_btn,
				new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						finish();
					}
				});

		builder.create().show();
	}
}
