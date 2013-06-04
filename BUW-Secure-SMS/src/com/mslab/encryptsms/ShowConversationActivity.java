package com.mslab.encryptsms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.Conversation;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.SMSNotifierHelper;
import com.mslab.encryptsms.receiver.SMSReceiver;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * This activity shows a chosen conversation. The messages on the left-hand side are sent messages
 * and the messages on the right-hand side are received messages. 
 * @author Paul Kramer
 *
 */
public class ShowConversationActivity extends FragmentActivity {
	
	private static final String DIALOG_CONVERSATION_EXTRA = "conversation";
	public static final String SEND_ANSWER_EXTRA = "sendanswer";
	public static String SHOW_SMS_BROADCAST = "com.mslab.encryptsms.showsms";
	
	private ArrayList<Conversation> conversationlist;
	private Contact contact;
	private ConversationsDataSource datasource;
	
	//UI elements
	private ListView mConversationsList;
	private Button mSendButton;
	private Button mPanicButton;
	
	//Context
	private Context context;
	
	//DB password
	private byte[] password;
	
	//Boradcastreceiver for updating view while reading sms
	private BroadcastReceiver receiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_conversation);
		
		//get bundled data
		Bundle bundle = getIntent().getExtras();
		
		//load context
		context = this;
		
		//load password
		password = getIntent().getExtras().getByteArray(Constants.DB_PASSWORD_HASH);
		
		//load UI
		mConversationsList = (ListView)findViewById(R.id.show_conversation_list);
		mSendButton = (Button)findViewById(R.id.show_conversation_btn_send);
		mPanicButton = (Button) findViewById(R.id.show_convertation_panic_button);
		
		//init datasource
		datasource = new ConversationsDataSource(this);
		datasource.open(password);
		
		//load conversation contact
		if(bundle.containsKey(Constants.CONTACT)){
			contact = bundle.getParcelable(Constants.CONTACT);
			
		}else if(bundle.containsKey(SMSReceiver.NEW_SMS)){
			contact = datasource.getContact(bundle.getString(SMSReceiver.NEW_SMS));
		}else{
			//handle error, no contact/contactid
		}
		
		mConversationsList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position,
					long id) {
				
				showSMSDetailsDialog(conversationlist.get((int)position));
				
//				AlertDialog.Builder builder = new AlertDialog.Builder(ShowConversationActivity.this);
//				builder.setTitle(R.string.show_conversation_contact_dialog_title);
//				builder.setMessage(conversationlist.get((int)position).message);
//				builder.setPositiveButton(R.string.show_conversation_contact_dialog_btn_pos, new OnClickListener() {
//					
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						dialog.dismiss();
//						datasource.deleteConversation(conversationlist.get((int)position));
//						updateConversationList();
//					}
//				});
//				builder.setNegativeButton(R.string.show_conversation_contact_dialog_btn_neg, new OnClickListener() {
//					
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						dialog.dismiss();
//					}
//				});
//				builder.create();
//				builder.show();
			}
		});
		
		mSendButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, WriteSMSActivity.class);
				ArrayList<Contact> contacts = new ArrayList<Contact>();
				contacts.add(contact);
				intent.putParcelableArrayListExtra(Constants.CONTACT, contacts);
				intent.putExtra(Constants.DB_PASSWORD_HASH, password);
				intent.putExtra(SEND_ANSWER_EXTRA, true);
				startActivity(intent);
			}
		});
		
		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
	}
	
	@Override
	protected void onResume() {
		//open database
		datasource.open(password);
		
		//load sms
		updateConversationList();
		
		// send user alive message
		SessionManagerService.alive(context);

		//conversation list update broadcast
		receiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				ReadSMSActivity.updateConversationsDatabase(datasource, context, password);
				int listlength = conversationlist.size();
				updateConversationList();
				updateListView();
				if(conversationlist.size() != listlength){
					//remove notification, if the received sms is shown
					SMSNotifierHelper.cancelNotification(context);
				}
			}
			
		};
		
		//register receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(SHOW_SMS_BROADCAST);
		registerReceiver(receiver, filter);
		

		//prevent from taking screenshots
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
					WindowManager.LayoutParams.FLAG_SECURE);
		}
		
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		datasource.close();
		unregisterReceiver(receiver);
		super.onPause();
	}
	
	private void updateConversationList(){
		conversationlist = new ArrayList<Conversation>();
		if(!datasource.isOpen())datasource.open(password);
		conversationlist.addAll(datasource.getAllConversations(contact));
		
		if(conversationlist.size() > 0){	
			updateListView();
		}else{
			//handle error: no conversations
		}
	}
	
	private void updateListView() {
		ConversationArrayAdapter mAdapter = new ConversationArrayAdapter(this,
				R.layout.show_conversation_list_item, conversationlist);
		mConversationsList.setAdapter(mAdapter);
	}
	
private class ConversationArrayAdapter extends ArrayAdapter<Conversation> {
		
		private int layoutResourceId;
		private Context context;
		private List<Conversation> data;
		private int datePadding;
		private int messagePadding;
		
		public ConversationArrayAdapter(Context context, int layoutResourceId,
				List<Conversation> data) {
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
				
				final ConversationHolder holder = new ConversationHolder();
				holder.txtDate = (TextView) row
						.findViewById(R.id.show_conversations_list_item_date);
				holder.txtMessage = (TextView) row
						.findViewById(R.id.show_conversations_list_item_message);
				
				datePadding = holder.txtDate.getPaddingLeft();
				messagePadding = holder.txtMessage.getPaddingLeft();
				
				row.setTag(holder);
			} else {
				row = convertView;
			}
			
			Conversation conversation = data.get(position);
			ConversationHolder holder = (ConversationHolder) row.getTag();
			SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, Locale.getDefault());//new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
			holder.txtDate.setText(df.format(conversation.date.getTime()));
			holder.txtMessage.setText(conversation.message);
			
			if(conversation.sent){
				holder.txtDate.setPadding(25 + datePadding, 0, 0, 0);
				holder.txtMessage.setPadding(25 + messagePadding, 0, 0, 0);
			}
			
			return row;
		}
		
		private class ConversationHolder {
			TextView txtDate;
			TextView txtMessage;
		}
	}

private void showSMSDetailsDialog(Conversation conversation) {
    FragmentManager fm = getSupportFragmentManager();
    SMSDetailsDialog smsDetailsDialog = new SMSDetailsDialog();
    Bundle bundle = new Bundle();
    bundle.putParcelable(DIALOG_CONVERSATION_EXTRA, conversation);
    smsDetailsDialog.setArguments(bundle);
    smsDetailsDialog.show(fm, "fragment_edit_name");
}

	@SuppressLint("ValidFragment")
	public class SMSDetailsDialog extends DialogFragment {
		
		private TextView mDateText;
		private TextView mStateText;
		private TextView mMessageText;
		private Conversation mConversation;
		
		public SMSDetailsDialog() {
			
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			mConversation = getArguments().getParcelable(DIALOG_CONVERSATION_EXTRA);
			super.onCreate(savedInstanceState);
			
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			final View v = inflater.inflate(R.layout.sms_details_dialog, null);
			
			mDateText = (TextView) v.findViewById(R.id.sms_details_date);
			mStateText = (TextView) v.findViewById(R.id.sms_details_state);
			mMessageText = (TextView) v.findViewById(R.id.sms_details_message);
			
			SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat
					.getDateTimeInstance(SimpleDateFormat.SHORT,
							SimpleDateFormat.SHORT, Locale.getDefault());// new
																			// SimpleDateFormat(
																			// "yyyy-MM-dd HH:mm:ss"
																			// );
			mDateText.setText(df.format(mConversation.date.getTime()));
			
			// show SMS state
			// note: we have to evaluate both fields, state and sent
			if (mConversation.sent) { // sent message
				mStateText
						.setText(getResources().getStringArray(
								R.array.sms_details_dialog_states)[mConversation.status]);
			} else {
				mStateText.setText(getResources().getStringArray(
						R.array.sms_details_dialog_states)[5]);
			}
			
			mMessageText.setText(mConversation.message);
			
			return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.sms_details_dialog_title))
            .setView(v)
            .setCancelable(true)
            .setPositiveButton(getResources().getString(R.string.sms_details_dialog_btn_pos), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                	dialog.dismiss();
					datasource.deleteConversation(mConversation);
					updateConversationList();
                }
            })
            .setNegativeButton(getResources().getString(R.string.sms_details_dialog_btn_neg), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
		}
	
}

}
