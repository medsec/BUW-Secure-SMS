package com.mslab.encryptsms;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.mslab.encryptsms.dialogs.AddContactCommunicationChoseDialog;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.services.SessionManagerService;
/**
 * An activity which displays a screen containing all contact details. This activity supports the change shared secret dialog.
 * @author Paul Kramer
 *
 */
public class ContactDetailsActivity extends FragmentActivity {
	
	//UI Elements
	private EditText mNameInput;
	private EditText mEMailInput;
	private EditText mPhoneInput;
	private Button mPanicButton;
	
	private Button mSaveButton;
	private Button mNewKeyButton;
	
	//current Contact
	private Contact contact;
	
	//database password
	private byte[] password;
	
	//application context
	private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_details);
		
		//load password
		password = getIntent().getExtras().getByteArray(Constants.DB_PASSWORD_HASH);
		
		//initialize context
		context = this;
		
		//load UI Elements
		mNameInput = (EditText) findViewById(R.id.contact_details_name_input);
		mEMailInput = (EditText) findViewById(R.id.contact_details_email_input);
		mPhoneInput = (EditText) findViewById(R.id.contact_details_phone_input);
		mPanicButton = (Button) findViewById(R.id.contact_details_panic_button);
		
		mSaveButton = (Button) findViewById(R.id.contact_details_save_btn);
		mNewKeyButton = (Button) findViewById(R.id.contact_details_keyexchange_btn);
		
		//load Contact
		contact = getIntent().getExtras().getParcelable(Constants.CONTACT);
		
		//show contact details
		fillContactDetails(contact);
		
		//button listeners
		
		//save changes
		mSaveButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Contact newContact = contact.clone();
				boolean modified = false;
				if(!mNameInput.getText().toString().equals(newContact.name)){
					contact.name = mNameInput.getText().toString();
					modified = true;
				}
				if(!mEMailInput.getText().toString().equals(newContact.email)){
					contact.email = mEMailInput.getText().toString();
					modified = true;
				}
				if(!mPhoneInput.getText().toString().equals(newContact.phonenumber)){
					contact.phonenumber = mPhoneInput.getText().toString();
					modified = true;
				}
				if(modified){
					ConversationsDataSource datasource = new ConversationsDataSource(context);
					datasource.open(password);
					datasource.updateContact(newContact, contact);
					datasource.close();
				}
				finish();
			}
		});
		
		//renew key
		mNewKeyButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showAddContactCommunicationDialog();
			}
		});
		
		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//reload contact
		ConversationsDataSource datasource = new ConversationsDataSource(context);
		datasource.open(password);
		contact = datasource.getContact(contact.id);
		datasource.close();
		
		//fill fields
		fillContactDetails(contact);
		
		//send user alive message
		SessionManagerService.alive(context);
	}
	
	private void fillContactDetails(Contact contact){
		mNameInput.setText(contact.name);
		mEMailInput.setText(contact.email);
		mPhoneInput.setText(contact.phonenumber);
	}
	
	private void showAddContactCommunicationDialog(){
    	FragmentManager fm = getSupportFragmentManager();
        AddContactCommunicationChoseDialog addContactsCommunicationDialog = new AddContactCommunicationChoseDialog(context);
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.DB_PASSWORD_HASH, password);
        bundle.putParcelable(Constants.CONTACT, contact);
        addContactsCommunicationDialog.setArguments(bundle);
        addContactsCommunicationDialog.show(fm, "fragment_edit_name");
    }
}
