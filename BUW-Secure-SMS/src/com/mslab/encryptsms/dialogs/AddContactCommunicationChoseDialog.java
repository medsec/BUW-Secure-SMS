package com.mslab.encryptsms.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mslab.encryptsms.ListBluetoothActivity;
import com.mslab.encryptsms.QRScanActivity;
import com.mslab.encryptsms.R;
import com.mslab.encryptsms.SMSKeyExchangeActivity;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;

/**
 * The dialog to chose the key exchange method. Three methods are available, exchange by QR-code, by bluetooth and by sms.
 * @author Paul Kramer
 *
 */
@SuppressLint("ValidFragment")
public class AddContactCommunicationChoseDialog extends DialogFragment{
	
//	private RadioButton mOption1, mOption2, mOption3;
	private ListView mCommunicationsList;
	private String[] mCommunicationTypes = new String[]{"QR-Code", "Bluetooth", "SMS"};
	private Context context;
//	private String password;
//	private Contact contact;
	
	public AddContactCommunicationChoseDialog(){
		
	}
	
	public AddContactCommunicationChoseDialog(Context context){
		this.context = context;
	}

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        this.password = getArguments().getString(LoginActivity.PASSWORD_HASH);
//        if(getArguments().containsKey(ContactDetailsActivity.CONTACT)){
//        	//rekeeing given contact
//        	this.contact = getArguments().getParcelable(ContactDetailsActivity.CONTACT);
//        }
    }
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View v = inflater.inflate(R.layout.add_contact_communication_choser, null);
        
//        mOption1 = (RadioButton)v.findViewById(R.id.add_communicaton_option1);
//        mOption2 = (RadioButton)v.findViewById(R.id.add_communicaton_option2);
//        mOption3 = (RadioButton)v.findViewById(R.id.add_communicaton_option3);
//        mOption1.setChecked(true);
        mCommunicationsList = (ListView)v.findViewById(R.id.communication_list);
        mCommunicationsList.setAdapter(new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_single_choice,
                android.R.id.text1, mCommunicationTypes));
        mCommunicationsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        Contact contact = null;
        if(getArguments().containsKey(Constants.CONTACT))
        	contact = getArguments().getParcelable(Constants.CONTACT);
        
        return new AlertDialog.Builder(getActivity())
                .setTitle(getTitle(contact))
                .setView(v)
                .setCancelable(false)
                .setPositiveButton(getPositiveButtonText(contact), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    	int selected = mCommunicationsList.getCheckedItemPosition();
                    	Bundle bundle = getArguments();
                    	if(selected == 0){
                    		startActivity(new Intent(context, QRScanActivity.class).putExtras(bundle));
                    		dismiss();
                    	}else if(selected == 1){
                    		startActivity(new Intent(context, ListBluetoothActivity.class).putExtras(bundle));
                    		dismiss();
                    	}else if(selected == 2){
                    		startActivity(new Intent(context, SMSKeyExchangeActivity.class).putExtras(bundle));
                    		dismiss();
                    	}
                    }
                })
                .setNegativeButton(R.string.add_communication_neg_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create();
    }
	
	private int getTitle(Contact contact){
		if(contact != null)
			return R.string.add_communication_title_new_key;
		return R.string.add_communication_title;
	}
	
	private int getPositiveButtonText(Contact contact){
		if(contact != null)
			return R.string.add_communication_pos_btn_new_key;
		return R.string.add_communication_pos_btn;
	}
	
}
