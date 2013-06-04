package com.mslab.encryptsms;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mslab.encryptsms.dialogs.AddContactCommunicationChoseDialog;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * An activity showing a list of all contacts. This activity helps to add or delete contacts and show contact details. 
 * @author Paul Kramer
 *
 */
public class ContactManagerActivity extends FragmentActivity {
	
	//UI elements
	private ListView mContactView;
	private Button mPanicButton;
	
	//contact list
	private ArrayList<Contact> contactlist;
	
	//Context and Database
	private Context context;
	private ConversationsDataSource datasource;
	
	private static String LIST_POSITION = "listposition";
	
	//database password
	private byte[] password;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_manager);
		context = ContactManagerActivity.this;
		
		//init ui elements
		mContactView = (ListView) findViewById(R.id.contact_manager_contacts_list);
		mPanicButton = (Button) findViewById(R.id.contact_manager_panic_button);
		
		//load password
		password = getIntent().getExtras().getByteArray(Constants.DB_PASSWORD_HASH);
		
		//load contacts list from database
		datasource = new ConversationsDataSource(context);
//		datasource.open();
//		updateContactsList();

		mContactView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				showModifyContactslistDialog(id);
			}
		});
//		mContactView.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				showModifyContactslistDialog(-1);
//			}
//		});

		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
	}
	
	@Override
	protected void onResume() {

		super.onResume();
		datasource.open(password);
		

		updateContactsList();
		
		//send user alive message
		SessionManagerService.alive(context);
	}
	
	@Override
	protected void onPause() {
		datasource.close();
		super.onPause();
	}	
	
	private void updateContactsList(){
		contactlist = new ArrayList<Contact>();
//		if(!datasource.isOpen())datasource.open();
		contactlist.addAll(datasource.getAllContacts(false));
		
		updateListView();
		if(contactlist.size() == 0){
			showNoContactsDialog();
			}
	}
	
	private void updateListView() {
		MyArrayAdapter mAdapter = new MyArrayAdapter(this,
				R.layout.contacts_list_item, contactlist);
		mContactView.setAdapter(mAdapter);
	}
	
	private void showNoContactsDialog() {
        FragmentManager fm = getSupportFragmentManager();
        NoContactsDialog noContactsDialog = new NoContactsDialog();
        noContactsDialog.show(fm, "fragment_edit_name");
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
				holder.txtExchange = (TextView) row
				.findViewById(R.id.contacts_list_item_exchange);
				
				row.setTag(holder);
			} else {
				row = convertView;
			}
			
			Contact contact = data.get(position);
			ContactHolder holder = (ContactHolder) row.getTag();
			holder.txtName.setText(contact.name);
			holder.txtNumber.setText(contact.phonenumber);
			
			holder.txtExchange
					.setText(getString(contact.exchange_done == Contact.WAITING ? R.string.contact_list_item_exchange_waiting
							: (contact.exchange_done == Contact.NOT_ACCEPTED ? R.string.contact_list_item_exchange_not_accepted
									: R.string.contact_list_item_exchange_accepted)));

			return row;
		}
		
		private class ContactHolder {
			TextView txtName;
			TextView txtNumber;
			TextView txtExchange;
		}
	}
	
	@SuppressLint("ValidFragment")
	private class NoContactsDialog extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setMessage(R.string.no_contacts_found_dialog_message)
	        		.setTitle(R.string.no_contacts_found_dialog_title)
	               .setPositiveButton(R.string.no_contacts_found_dialog_positive_btn, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {

	                	   //Add Contact Activity
	                	   dismiss();
	                	   showAddContactCommunicationDialog();
	                	   
	                   }
	               })
	               .setNegativeButton(R.string.no_contacts_found_dialog_negative_btn, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // User cancelled the dialog
	                	   if(contactlist.size() == 0){
	                		   finish();
	                	   }else{
	                		   dismiss();
	                	   }
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}

    private void showAddContactCommunicationDialog(){
    	FragmentManager fm = getSupportFragmentManager();
        AddContactCommunicationChoseDialog addContactsCommunicationDialog = new AddContactCommunicationChoseDialog(context);
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.DB_PASSWORD_HASH, password);
        addContactsCommunicationDialog.setArguments(bundle);
        addContactsCommunicationDialog.show(fm, "fragment_edit_name");
    }
    
	@SuppressLint("ValidFragment")
	private class ModifyContactsDialog extends DialogFragment {
		
		Button mAddContact;
		Button mModifyContact;
		Button mRemoveContact;
		Button mAcceptContact;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			final View v = inflater.inflate(R.layout.modify_contacts_dialog,
					null);
			
			mAddContact = (Button) v.findViewById(R.id.modify_dialog_add_contact_button);
			mModifyContact = (Button) v.findViewById(R.id.modify_dialog_modify_contact_button);
			mRemoveContact = (Button) v.findViewById(R.id.modify_dialog_remove_contact_button);
			mAcceptContact = (Button) v.findViewById(R.id.modify_dialog_accept_contact_button);
			
			if(getArguments().getLong(LIST_POSITION) < 0){
				mRemoveContact.setEnabled(false);
				mModifyContact.setEnabled(false);
			}
			
			//write text on accept/decline button
			final Contact contact = contactlist.get((int)getArguments().getLong(LIST_POSITION)); 
			if(contact.exchange_done > Contact.WAITING){
				if(contact.exchange_done == Contact.NOT_ACCEPTED){
					mAcceptContact.setText(getString(R.string.modify_contacts_dialog_accept_btn_text));
				}else{
					mAcceptContact.setText(getString(R.string.modify_contacts_dialog_decline_btn_text));
				}
			}else{
				//waiting for key
				mAcceptContact.setText(getString(R.string.modify_contacts_dialog_accept_btn_text));
				mAcceptContact.setEnabled(false);
			}
			
			mAddContact.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//startActivity(new Intent(context, QRScanActivity.class));
					showAddContactCommunicationDialog();
					dismiss();
				}
			});
			
			mModifyContact.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(context, ContactDetailsActivity.class);
					intent.putExtra(Constants.DB_PASSWORD_HASH, password);
					intent.putExtra(Constants.CONTACT, contactlist.get((int)getArguments().getLong(LIST_POSITION)));
					startActivity(intent);
					dismiss();
				}
			});
			
			mRemoveContact.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					showRemoveSafetyQuestionDialog(getArguments().getLong(LIST_POSITION));
					dismiss();
				}
			});
			
			mAcceptContact.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(contact.exchange_done == Contact.ACCEPTED){
						//decline it
						contact.exchange_done = Contact.NOT_ACCEPTED;
						ConversationsDataSource dataSource = new ConversationsDataSource(context);
						dataSource.open(password);
						dataSource.updateExchangeState(contact);
						dataSource.close();
						
						//update listview
						updateContactsList();
					}else if(contact.exchange_done == Contact.NOT_ACCEPTED){
						//show accept dialog
						showAcceptKeyDialog(contact);
					}
					
					dismiss();
				}
			});
			
			
			return new AlertDialog.Builder(getActivity())
					.setTitle(R.string.modify_contacts_dialog_title)
					.setView(v)
					.setCancelable(true)
					.setNegativeButton(
							R.string.modify_contacts_dialog_neg_btn_text,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).create();
		}
		
	}
	
	private void showModifyContactslistDialog(long position){
    	FragmentManager fm = getSupportFragmentManager();
        ModifyContactsDialog modifyContactsDialog = new ModifyContactsDialog();
        Bundle bundle = new Bundle();
        bundle.putLong(LIST_POSITION, position);
        modifyContactsDialog.setArguments(bundle);
        modifyContactsDialog.show(fm, "fragment_edit_name");
    }
	
@SuppressLint("ValidFragment")
private class RemoveSafetyQuestionDialog extends DialogFragment {
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			
			String message = getResources().getString(R.string.remove_safety_question_dialog_message_part1);
			message += " \""+ contactlist.get((int)getArguments().getLong(LIST_POSITION)).name + "\"?";
			
			return new AlertDialog.Builder(getActivity())
					.setMessage(message)
					.setCancelable(true)
					.setPositiveButton(R.string.remove_safety_question_dialog_pos_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeContact(getArguments().getLong(LIST_POSITION));
                        }
                    })
					.setNegativeButton(
							R.string.remove_safety_question_dialog_neg_btn,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).create();
		}
		
	}
	
	private void showRemoveSafetyQuestionDialog(long position){
    	FragmentManager fm = getSupportFragmentManager();
        RemoveSafetyQuestionDialog removeSafetyQuestionDialog = new RemoveSafetyQuestionDialog();
        Bundle bundle = new Bundle();
        bundle.putLong(LIST_POSITION, position);
        removeSafetyQuestionDialog.setArguments(bundle);
        removeSafetyQuestionDialog.show(fm, "fragment_edit_name");
    }
	
	private void removeContact(long position){
		if(!datasource.deleteContact(contactlist.get((int)position))){
			//failure occured
			Toast.makeText(context, "problem removing contact", Toast.LENGTH_LONG).show();
		}else{
			//everything was fine
			updateContactsList();
		}
	}
	
@SuppressLint("ValidFragment")
private class AcceptDialog extends DialogFragment {
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			final Contact contact = getArguments().getParcelable(Constants.CONTACT); 
			String message = getResources().getString(R.string.accept_key_dialog_message_part1);
			message += "\n" + contact.getSecretFingerprint(context) + "\n";
			message += getString(R.string.accept_key_dialog_message_part2);
			
			return new AlertDialog.Builder(getActivity())
					.setMessage(message)
					.setCancelable(true)
					.setPositiveButton(R.string.accept_key_dialog_pos_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	contact.exchange_done = Contact.ACCEPTED;
                        	ConversationsDataSource dataSource = new ConversationsDataSource(context);
    						dataSource.open(password);
    						dataSource.updateExchangeState(contact);
    						dataSource.close();
    						
    						//update listview
    						updateContactsList();
    						
    						dialog.dismiss();
                        }
                    })
					.setNegativeButton(
							R.string.accept_key_dialog_neg_btn,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).create();
		}
	}

private void showAcceptKeyDialog(Contact contact){
	FragmentManager fm = getSupportFragmentManager();
    AcceptDialog acceptDialog = new AcceptDialog();
    Bundle bundle = new Bundle();
    bundle.putParcelable(Constants.CONTACT, contact);
    acceptDialog.setArguments(bundle);
    acceptDialog.show(fm, "fragment_edit_name");
}

}
