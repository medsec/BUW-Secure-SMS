package com.mslab.encryptsms;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.mslab.McOE.McOE;
import com.mslab.McOE.McOEX;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.Conversation;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.ReceivedSMS;
import com.mslab.encryptsms.misc.SMSDataStorage;
import com.mslab.encryptsms.misc.SMSNotifierHelper;
import com.mslab.encryptsms.misc.SMSSentDataSource;
import com.mslab.encryptsms.misc.SentSMS;
import com.mslab.encryptsms.services.SessionManagerService;
import com.mslab.smsutils.SMSUtils;


/**
 * This activity shows a list of all contacts with messages. It is started from the main menu and decrypts received messages.
 * @author Paul Kramer
 *
 */
@SuppressLint("ValidFragment")
public class ReadSMSActivity extends FragmentActivity {
	
	private ConversationsDataSource datasource;
	
	private ArrayList<Contact> contactlist;
	
	private static String CORRUPTED = "corrupted";

	private Context context;
	
	private byte[] password;
	
	// UI elements
	private ListView mContactsList;
	private Button mPanicButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read_sms);
		
		context = this;
		password = getIntent().getExtras().getByteArray(Constants.DB_PASSWORD_HASH);
		
		// load ui elements
		mContactsList = (ListView) findViewById(R.id.read_sms_conversations_list);
		mPanicButton = (Button) findViewById(R.id.read_sms_panic_button);
		
		// load database
		datasource = new ConversationsDataSource(this);
		datasource.open(password);
		
//		contactlist = new ArrayList<Contact>();
//		contactlist.addAll(datasource.getAllContactsWithConversations());
		
//		updateListView();
//		
		mContactsList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> paent, View v, int position,
					long id) {
				showConversation(position);
			}
		});
		
		mContactsList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					final int position, long id) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ReadSMSActivity.this);
				builder.setTitle(R.string.read_sms_dialog_title);
				builder.setMessage(contactlist.get((int)position).name);
				builder.setPositiveButton(R.string.read_sms_dialog_btn_pos, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						datasource.deleteConversations(contactlist.get((int)position));
						//load all SMS which were received
						updateConversationsDatabase(datasource, context, password);
						//reload contacts
						loadContacts();
						//update the view
						updateListView();
					}
				});
				builder.setNegativeButton(R.string.read_sms_dialog_btn_neg, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.create();
				builder.show();
				return false;
			}
			
		});
		
		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_read_sms, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		datasource.open(password);
		super.onResume();
		
		//load all SMS which were received
		int corrupted = updateConversationsDatabase(datasource, context, password);
		
		//update outbox states
		updateSentSMSStates(datasource);
		
		//reload conversations
		loadContacts();
		
		//update listview
		updateListView();
		
		//show dialog if necessary
		if(contactlist.size() == 0) showNoConversationsDialog();
		
		//show recent sms, if this activity was started by a new received sms
		if(getIntent().getExtras().getBoolean(Constants.SMS_RECEIVED)){
			if(corrupted == 0){
				getIntent().removeExtra(Constants.SMS_RECEIVED);
				showConversation(0);
			}else showCorruptedMessagesDialog(corrupted);
			getIntent().removeExtra(Constants.SMS_RECEIVED);
		}
		
		// send user alive message
		SessionManagerService.alive(context);
		
		//remove notification, if it is necessary
		SMSNotifierHelper.cancelNotification(context);
		
		//prevent from taking screenshots
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
					WindowManager.LayoutParams.FLAG_SECURE);
		}
	}
	
	@Override
	protected void onPause() {
		datasource.close();
		super.onPause();
	}
	
	private void loadContacts(){
		contactlist = new ArrayList<Contact>();
		contactlist.addAll(datasource.getAllContactsWithConversations());
	}
	
	private void updateListView(){
		MyArrayAdapter mAdapter = new MyArrayAdapter(this,
				R.layout.read_sms_contacts_list_item, contactlist);
		mContactsList.setAdapter(mAdapter);
	}
	
	
	public class MyArrayAdapter extends ArrayAdapter<Contact> {
		
		private int layoutResourceId;
		private Context context;
		private List<Contact> data;
		
		public MyArrayAdapter(Context context, int layoutResourceId,
				List<Contact> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = null;
			
			if (convertView == null) {
				LayoutInflater inflater = ((Activity) context)
						.getLayoutInflater();
				row = inflater.inflate(layoutResourceId, null);
				// row = inflater.inflate(layoutResourceId, parent, false);
				
				final ContactHolder holder = new ContactHolder();
				holder.txtName = (TextView) row
						.findViewById(R.id.read_sms_contacts_list_item_name);
				
				row.setTag(holder);
			} else {
				row = convertView;
			}
			
			Contact contact = data.get(position);
			ContactHolder holder = (ContactHolder) row.getTag();
			holder.txtName.setText(contact.name);
			
			return row;
		}
		
		private class ContactHolder {
			TextView txtName;
		}
	}
	
	@SuppressLint("ValidFragment")
	private class NoConversationsDialog extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setMessage(R.string.no_conversations_dialog_message)
	        		.setTitle(R.string.no_conversations_dialog_title)
	               .setNeutralButton(R.string.no_conversations_btn, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   finish();
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
	
	private void showNoConversationsDialog() {
        FragmentManager fm = getSupportFragmentManager();
        NoConversationsDialog noContactsDialog = new NoConversationsDialog();
        noContactsDialog.show(fm, "fragment_no_conv_name");
    }
	
	private class CorruptedMessagesDialog extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        
	        String message = "";
	        
	        if(getArguments().getInt(CORRUPTED) == 1){
	        	message += getResources().getString(R.string.corrupted_messages_dialog_message_0_singular) + " " +
		        		getArguments().getInt(CORRUPTED) + " " +
		        		getResources().getString(R.string.corrupted_messages_dialog_message_1_singular);
	        }else{
	        	message += getResources().getString(R.string.corrupted_messages_dialog_message_0_plural) + " " +
		        		getArguments().getInt(CORRUPTED) + " " +
		        		getResources().getString(R.string.corrupted_messages_dialog_message_1_plural);
	        }
	        
	        builder.setMessage(message)
	        		.setTitle(R.string.corrupted_messages_dialog_title)
	               .setNeutralButton(R.string.corrupted_messages_btn, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   finish();
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
	
	private void showCorruptedMessagesDialog(int count) {
        FragmentManager fm = getSupportFragmentManager();
        CorruptedMessagesDialog corruptedMessagesDialog = new CorruptedMessagesDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(CORRUPTED, count);
        corruptedMessagesDialog.setArguments(bundle);
        corruptedMessagesDialog.show(fm, "fragment_no_conv_name");
    }
	
	/**
	 * This method updates the ciphered conversation database with all received messages and changed states.
	 * @param datasource The ciphered database.
	 * @param context The application context.
	 * @param password The database password.
	 * @return the number of added entries.
	 */
	public static int updateConversationsDatabase(ConversationsDataSource datasource, Context context, byte[] password){
		//load SMS from temporary sms database
		SMSDataStorage datastorage = new SMSDataStorage(context);
		datastorage.open();
		ArrayList<ReceivedSMS> smss = new ArrayList<ReceivedSMS>();
		smss.addAll(datastorage.getAllSMS());
		
		//count corrupted messages
		int corrupted = 0;
		
		for(ReceivedSMS sms : smss){
			Contact contact = datasource.getContact(sms.Phone);
			
			if(sms.isProbablyKeyExchangeSMS()){
				//handle key exchange sms
				Intent intent = new Intent(context, SMSKeyExchangeActivity.class);
				intent.putExtra(Constants.DB_PASSWORD_HASH, password);
				intent.putExtra(Constants.SMS_KEY_EXCHANGE, true);
				intent.putExtra(Constants.SMS_KEY, sms);
				context.startActivity(intent);
			}else{
				Conversation conversation = new Conversation(sms.Date, decryptMessage(contact, sms.Message));
				if(conversation.message == null){
					++corrupted;
				}else{
					conversation.contactid = contact.id;
					datasource.createConversation(conversation);
				}
			}
		}
		datastorage.deleteAllReceivedSMS();
		datastorage.close();
		return corrupted;
	}
	
	private static String decryptMessage(Contact contact, byte[] encryptedmessage){
		
		McOE mcoe = new McOEX();
		int Blocksize = 128;
		BlockCipher cipher = new AESEngine();
		
		//ensure, that at least two blocks where sent
//		if(((encryptedmessage.length*7/8) - 9) / 16 < 2) return null;
		
//		byte[] Ciphertext = SMSUtils.compressBytesTo7BitPerCharEncoding(encryptedmessage);
		byte[] Ciphertext = encryptedmessage;
		

		//prepare header
		byte[] header = new byte[9];
//		System.arraycopy(Ciphertext, Ciphertext.length - 8, header, 0, header.length);
		System.arraycopy(Ciphertext, 0, header, 0, header.length);
		//remove IV
		byte[] help = new byte[Ciphertext.length - 9];
		System.arraycopy(Ciphertext, header.length, help, 0, help.length);
		Ciphertext = help;
		
		//remove detection byte from header
		help = new byte[header.length - 1];
		System.arraycopy(header, 1, help, 0, help.length);
		header = help;
		
		byte[] Tag = new byte[Blocksize / 2 / Byte.SIZE]; // 64 Bits Tag
		
		System.arraycopy(Ciphertext, Ciphertext.length - Tag.length, Tag, 0, Tag.length);
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Tag.length];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		byte[] message = mcoe.decryptAuthenticate(header, CiphertextWithoutTag, Tag, Blocksize, cipher, McOE.resizeKey(contact.secret, Blocksize, Blocksize));
		
		message = McOE.resizeArray(message);
		
		if(message == null) return null;
		
		//handle 7 or 8 bit chars
		if(encryptedmessage[0] == ReceivedSMS.ENCRYPTED_BYTE_7){
			message = SMSUtils.expandStringTo8BitPerChar(SMSUtils.removePadding8(message));
		}else{
			//message contains 7 and 8 bit chars
			message = SMSUtils.removePadding8(message);
		}
		
		
		return new String(message);
	}
	
	private void updateSentSMSStates(ConversationsDataSource datasource) {
		SMSSentDataSource sentSMSDatasource = new SMSSentDataSource(context);
		sentSMSDatasource.open();
		
		ArrayList<SentSMS> smslist = new ArrayList<SentSMS>();
		smslist.addAll(sentSMSDatasource.getAllSentSMS());
		
		//update conversation database
		for(SentSMS sms: smslist){
			datasource.updateConversationState(sms);
		}
		
		//clean sentSMS database
		for(SentSMS sms : smslist){
			if(sms.State == Conversation.SMS_STATUS_DELIVERED){
				sentSMSDatasource.deleteSentSMS(sms.ID);
			}
		}
		
		sentSMSDatasource.close();
	}

	
	private void showConversation(int position){
		if(contactlist.size() == 0) return;
		Intent intent = new Intent(context, ShowConversationActivity.class);
		intent.putExtra(Constants.CONTACT, contactlist.get(position));
		intent.putExtra(Constants.DB_PASSWORD_HASH, password);
		startActivity(intent);
	}
}
