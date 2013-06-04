package com.mslab.encryptsms;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mslab.McOE.McOE;
import com.mslab.McOE.McOEX;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.Conversation;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.ReceivedSMS;
import com.mslab.encryptsms.receiver.SMSDeliveredReceiver;
import com.mslab.encryptsms.receiver.SMSSentReceiver;
import com.mslab.encryptsms.services.SessionManagerService;
import com.mslab.smsutils.SMSUtils;


/**
 * This activity represents the message input screen. Here, the user can write his message and 
 * press send to send it towards the chosen contact.
 * @author Paul Kramer
 *
 */
public class WriteSMSActivity extends Activity {
	
	private static int FIRST_SMS_LENGTH_7 = 136;
	private static int OTHER_SMS_LENGTH_7 = 146;
	private static int FIRST_SMS_LENGTH_8 = 119;
	private static int OTHER_SMS_LENGTH_8 = 127;
	
	private Context context;
	//UI elements
	private TextView mContactName;
	private TextView mSMSLabel;
	private EditText mInputField;
	private Button mSendButton;
	private Button mPanicButton;
	
	//remaining characters
	private int oldlength = 0;
	
	//contact
	private ArrayList<Contact> contacts;
	
	//DB password
	private byte[] password;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_write_sms);

		//load context
		context = this;
		
		//load bundled data
		Bundle bundle = getIntent().getExtras();
		
		//load DB password
		password = bundle.getByteArray(Constants.DB_PASSWORD_HASH);
		
		//load contacts
		contacts = bundle.getParcelableArrayList(Constants.CONTACT);
		
		//init UI elements
		mContactName = (TextView) findViewById(R.id.write_label_contact_name);
		mSMSLabel = (TextView)findViewById(R.id.write_label_remaining_chars);
		mInputField = (EditText)findViewById(R.id.write_input_sms);
		mSendButton = (Button)findViewById(R.id.write_btn_send);
		mPanicButton = (Button) findViewById(R.id.write_sms_panic_button);
		
		mSMSLabel.setText(FIRST_SMS_LENGTH_7 + " " + getResources().getString(R.string.write_sms_input_remain_plural));
		
		//debug
//		mInputField.setText("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat,");
//		mInputField.setText("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum do");
//		mInputField.setText("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam e");
//		mInputField.setText("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsu");
//		mInputField.setText("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquy");
//		mInputField.setText("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed");
		
		//show contact name
		if(contacts.size() == 1){
			mContactName.setText(getResources().getString(R.string.write_sms_contact_message) + " " + contacts.get(0).name);
		}else{
			String text = getResources().getString(R.string.write_sms_contact_message) + " ";
			for(Contact contact : contacts){
				text += contact.name;
				text += ",";
			}
			text = text.substring(0, text.length()-2);
			mContactName.setText(text);
		}
		
		//add imput listener
		mInputField.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//because of the GSM encoding we have decided to manage only 7-bit chars
				//handle 8-bit characters
				if(s.length() == 0) return;
				byte lastchar = (byte)s.charAt(s.length()-1);
//				if(lastchar <= -1 &&
//						lastchar >= -128){
//					s = s.subSequence(0, s.length()-1);
//					Toast.makeText(context, getResources().getString(R.string.write_sms_input_error), Toast.LENGTH_LONG).show();
//					mInputField.setText(s);
//					mInputField.setSelection(s.length());
//				}
				
				boolean eightbitmode = false;
				for(int i = 0; i < s.length(); ++i){
					lastchar = (byte) s.charAt(i);
					eightbitmode |= (lastchar <= -1 && lastchar >= -128); 
				}
				
				
				int inputlength = 0;
				
				int first_sms_length = FIRST_SMS_LENGTH_7;
				int other_sms_length = OTHER_SMS_LENGTH_7;
				
				if(eightbitmode){
					first_sms_length = FIRST_SMS_LENGTH_8;
					other_sms_length = OTHER_SMS_LENGTH_8;
				}
				
				//count the number of two byte chars
				int twobytechars = 0;
				if(eightbitmode){
					for(int i = 0; i < s.length(); ++i){
						lastchar = (byte) s.charAt(i);
						twobytechars += (lastchar <= -1 && lastchar >= -128) ? 1 : 0;
					}
				}
				
				inputlength = first_sms_length - s.length() - twobytechars;
				
				//show number of sms
				int number = (s.length() - first_sms_length) / other_sms_length + 1;
				
				if(inputlength < 0){ //multi SMS
					inputlength *= -1;
					int rest = other_sms_length - inputlength % other_sms_length;
					if(rest == 1){
						mSMSLabel.setText(rest + " " + getResources().getString(R.string.write_sms_input_remain_singular));
					}else{
						mSMSLabel.setText(rest + " " + getResources().getString(R.string.write_sms_input_remain_plural));
						if(rest == (other_sms_length-1) && oldlength < s.length()){
							Toast.makeText(context, "SMS "+(number+1), Toast.LENGTH_SHORT).show();  //show sms count increase toast
						}
						if(rest % other_sms_length == 0 && oldlength > s.length()){
							Toast.makeText(context, "SMS "+number, Toast.LENGTH_SHORT).show();  //show sms count decrease toast
						}
					}
				}else if(inputlength == 1){//one SMS
					mSMSLabel.setText(inputlength + " " + getResources().getString(R.string.write_sms_input_remain_singular));
				}else{
					mSMSLabel.setText(inputlength + " " + getResources().getString(R.string.write_sms_input_remain_plural));
					if(inputlength == 0 && oldlength > s.length()){
						Toast.makeText(context, "SMS "+number, Toast.LENGTH_SHORT).show();  //show sms count decrease toast
					}
				}
				
				oldlength = s.length();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
		
		mSendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				long time = GregorianCalendar.getInstance()
						.getTimeInMillis();
				
				//for all chosen contacts
				for(Contact contact : contacts){
					Conversation conversation = new Conversation(time, mInputField.getText().toString());
					conversation.contactid = contact.id;
					conversation.sent = true;
					sendSMS(contact, conversation);
				}
				
				//started from read sms activity
				if(getIntent().getExtras().containsKey(ShowConversationActivity.SEND_ANSWER_EXTRA)){
					finish();
				}else{
					//go back to main screen
					Intent intent = new Intent(context, MainActivity.class)
					.putExtra(Constants.DB_PASSWORD_HASH, password)
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					
					startActivity(intent);
				}
			}
		});

		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
	}
	
	@Override
	protected void onResume() {
		// send user alive message
		SessionManagerService.alive(context);
		
		super.onResume();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_write_sms, menu);
		return true;
	}
	
	// ---sends an SMS message to another device---
	// from http://mobiforge.com/developing/story/sms-messaging-android
	// 03.12.2012
	private void sendSMS(Contact contact, final Conversation conversation) {
		
		// save the sms into database
		ConversationsDataSource datasource = new ConversationsDataSource(
				context);
		datasource.open(password);
		conversation.status = Conversation.SMS_STATUS_OUTBOX;
		Conversation storedconversation = datasource
				.createConversation(conversation);
		datasource.close();
		
		SmsManager sms = SmsManager.getDefault();
		
		byte[] ciphertext = encryptMessage(contact, conversation.message);
		
//		ciphertext = SMSUtils.appendZeroEncoding(ciphertext);
		
		ArrayList<String> parts = sms.divideMessage(new String(ciphertext));
		
		ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliveredPIs = new ArrayList<PendingIntent>();
		
		Bundle bundle = new Bundle();
		bundle.putLong(Conversation.CONVERSATION_ID, storedconversation.id);
		
		for (int i = 0; i < parts.size(); ++i) {
			bundle.putInt("part", i);
			
			Intent sentIntent = new Intent(SMSSentReceiver.SMS_SENT);
			sentIntent.putExtras(bundle);
			Intent deliveredIntent = new Intent(
					SMSDeliveredReceiver.SMS_DELIVERED);
			deliveredIntent.putExtras(bundle);
			
			sentPIs.add(PendingIntent.getBroadcast(this, 0, sentIntent,
					PendingIntent.FLAG_ONE_SHOT));
			deliveredPIs.add(PendingIntent.getBroadcast(this, 1,
					deliveredIntent, PendingIntent.FLAG_ONE_SHOT));
		}
		
		sms.sendMultipartTextMessage(contact.phonenumber, null, parts, sentPIs,
				deliveredPIs);
//		sms.sendMultipartTextMessage("+4915737469021", null, parts, sentPIs,
//				deliveredPIs);
		
	}
	
	private boolean containsGSMEscapeByte(byte[] ciphertext) {
		for (byte b : ciphertext) {
			if (b == 27)
				return true;
		}
		return false;
	}
	
	public static byte[] encryptMessage(Contact contact, String plainmessage) {
		byte[] IV = McOE.generateIV(8);//McOE.generateIV(8, 12);// new byte[7];
		int Blocksize = 128;
		McOE mcoe = new McOEX();
		BlockCipher cipher = new AESEngine();
		
		byte[] message = plainmessage.getBytes();
		
		//check for only 7-Bit chars or 8 and 7 Bit mixed mode
		boolean eightbitmode = false;
		for(int i = 0; i < message.length; ++i){
			eightbitmode |= (message[i] <= -1 && message[i] >= -128); 
		}
		
		//compress only 7-Bit chars containing SMS
		if(!eightbitmode)
		message = SMSUtils.compressStringTo7BitPerCharEncoding(message);
		
		message = SMSUtils.appendPadding8SMSLength(message);
		
		// prepare header, it has to be of length 72 bits (8 Bits encrypted byte and 64 bits IV)
		byte[] header = new byte[8];
//		if(eightbitmode){
//			header[0] = ReceivedSMS.ENCRYPTED_BYTE_8;
//		}else{
//			header[0] = ReceivedSMS.ENCRYPTED_BYTE_7;
//		}
		System.arraycopy(IV, 0, header, 0, IV.length);
		
		byte[] encryptedmessage = mcoe.encryptAuthenticate(header, message,
				Blocksize, cipher,
				McOE.resizeKey(contact.secret, Blocksize, Blocksize));
		
		// append IV + Encrypted Byte
		byte[] help = new byte[encryptedmessage.length + header.length];
		System.arraycopy(header, 0, help, 0, header.length); // copy encrypted
																// byte + IV
		System.arraycopy(encryptedmessage, 0, help, header.length,
				encryptedmessage.length); // copy message
		encryptedmessage = help;
		
		//remove the half of the tag to get 64 bits tag
		help = new byte[encryptedmessage.length - 64 / Byte.SIZE];
		System.arraycopy(encryptedmessage, 0, help, 0, help.length);
		encryptedmessage = help;
		
		//encode to Base123
		encryptedmessage = SMSUtils.encodeBase123(encryptedmessage).getBytes();
		help = new byte[encryptedmessage.length+1];
		System.arraycopy(encryptedmessage, 0, help, 1, encryptedmessage.length);
		if(eightbitmode){
			help[0] = ReceivedSMS.ENCRYPTED_BYTE_8;
		}else{
			help[0] = ReceivedSMS.ENCRYPTED_BYTE_7;
		}
		encryptedmessage = help;
		
//		encryptedmessage = SMSUtils.expandBytesTo8BitPerChar(encryptedmessage);
		
		return encryptedmessage;// new String(encryptedmessage);
	}
	
}
