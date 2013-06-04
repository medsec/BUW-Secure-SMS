package com.mslab.encryptsms;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * This activity shows all contacts. At least one contact must be chosen from the list to send a message to it. By pressing
 * the send message buton, the user will be forwarded to the WriteSMSActivity.
 * @author Paul Kramer
 *
 */
public class SMSContactListActivity extends Activity {
	
	private ArrayList<Contact> contactlist;
	
	//UI elements
	private ListView mContactView;
	private Button mSendBtn;
	private Button mPanicButton;
	
	
	private Context context;
	private Contact contact;
	
	// DB password
	private byte[] password;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms_contact_list);
		
		// load extras
		Bundle extras = getIntent().getExtras();
		
		// load password
		password = extras.getByteArray(Constants.DB_PASSWORD_HASH);
		
		context = this;
		
		//init UI elements
		mContactView = (ListView) findViewById(R.id.sms_contact_contacts_list);
		mSendBtn = (Button) findViewById(R.id.sms_contact_btn_send);
		mPanicButton = (Button) findViewById(R.id.sms_contact_list_panic_button);
		
		
		contactlist = loadContacts();
		
		mSendBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				boolean selected = false;
				for (Contact contact : contactlist) {
					selected |= contact.isSelected;
				}
				if (!selected) {
					Toast.makeText(
							context,
							getResources().getString(
									R.string.error_no_contact_selected),
							Toast.LENGTH_LONG).show();
				} else {
					// send SMS
					ArrayList<Contact> contacts = new ArrayList<Contact>();
					for (Contact contact : contactlist) {
						if (contact.isSelected) {
//							contact.isSelected = false;
							contacts.add(contact);
						}
					}
					Intent intent = new Intent(context, WriteSMSActivity.class);
					intent.putParcelableArrayListExtra(Constants.CONTACT, contacts);
					intent.putExtra(Constants.DB_PASSWORD_HASH, password);
					startActivity(intent);
				}
			}
		});
		
		mContactView.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				contactlist.get((int) id).isSelected = !contactlist
						.get((int) id).isSelected;
				updateListView();
			}
			
		});
		
		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
		
	}
	
	
	@Override
	protected void onResume() {
		//send user alive message
		SessionManagerService.alive(context);
		
		//show contacts list
		updateListView();
		
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	private ArrayList<Contact> loadContacts() {
		// Cursor people = getContentResolver().query(
		// ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		//
		ArrayList<Contact> contactslist = new ArrayList<Contact>();
		//
		// while (people.moveToNext()) {
		// int nameFieldColumnIndex = people
		// .getColumnIndex(PhoneLookup.DISPLAY_NAME);
		// String name = people.getString(nameFieldColumnIndex);
		//
		// int hasPhone = Integer.parseInt(people
		// .getString(people
		// .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
		// String number = "";
		//
		// // this contact contains phone number(s)
		// if (hasPhone != 0) {
		//
		// Cursor pCur = getContentResolver()
		// .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
		// null,
		// ContactsContract.CommonDataKinds.Phone.CONTACT_ID
		// + " = "
		// + people.getString(people
		// .getColumnIndex(ContactsContract.Contacts._ID)),
		// null, null);
		// //iterate over all numbers
		// while (pCur.moveToNext()) {
		// //take only mobile numbers
		// if (Integer
		// .parseInt(pCur.getString(pCur
		// .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2))) ==
		// (ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)) {
		// number =
		// pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
		// //if one number was found, stop the loop
		// break;
		// }
		// }
		// pCur.close();
		// }
		//
		// contactslist.add(new Contact(name, number));
		// }
		
		ConversationsDataSource datasource = new ConversationsDataSource(
				context);
		datasource.open(password);
		contactslist.addAll(datasource.getAllContacts(true));
		datasource.close();
		
		// if this activity was started to answer to a conversation, we have to
		// select this contact
		if (contact != null) {
			for (Contact con : contactslist) {
				if (con.equals(contact))
					con.isSelected = true;
			}
		}
		
		return contactslist;
	}
	
	private void updateListView() {
		MyArrayAdapter mAdapter = new MyArrayAdapter(this,
				R.layout.contacts_list_chosable_item, contactlist);
		mContactView.setAdapter(mAdapter);
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
						.findViewById(R.id.contacts_list_item_name);
				holder.txtNumber = (TextView) row
						.findViewById(R.id.contacts_list_item_number);
				holder.chkSelected = (CheckBox) row
						.findViewById(R.id.contacts_list_item_check);
				
				holder.chkSelected
						.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								Contact element = (Contact) holder.chkSelected
										.getTag();
								element.isSelected = buttonView.isChecked();
								
							}
						});
				
				row.setTag(holder);
				holder.chkSelected.setTag(data.get(position));
			} else {
				row = convertView;
				((ContactHolder) row.getTag()).chkSelected.setTag(data
						.get(position));
			}
			
			Contact contact = data.get(position);
			ContactHolder holder = (ContactHolder) row.getTag();
			holder.txtName.setText(contact.name);
			holder.txtNumber.setText(contact.phonenumber);
			holder.chkSelected.setChecked(contact.isSelected);
			
			return row;
		}
		
		private class ContactHolder {
			TextView txtName;
			TextView txtNumber;
			CheckBox chkSelected;
		}
	}
	
	/**
	 * Only a debug helper. It outputs byte arrays as hex string.
	 * @param a a input byte array.
	 * @return the hex representation of the input.
	 */
	static public String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for (byte b : a) {
			sb.append(String.format("%02x", b & 0xff));
			sb.append(", ");
		}
		return sb.toString();
	}
	
}
