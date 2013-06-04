package com.mslab.encryptsms;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bouncycastle.util.encoders.Base64;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mslab.encryptsms.misc.BluetoothService;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.DHKeyGen;
import com.mslab.encryptsms.misc.DHKeyGen.DHKeyPair;
import com.mslab.encryptsms.misc.QRCodeDecoder;
import com.mslab.encryptsms.misc.QRCodeReturn;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * This activity shows all paired bluetooth devices and allows to select one of them. 
 * After selection it initiates the keyexchange by bluetooth.
 * @author Paul Kramer
 *
 */
public class ListBluetoothActivity extends FragmentActivity {
	
	//load preferences
	SharedPreferences preferences;
	
	private static final int REQUEST_ENABLE_BT = 121;
    private static final int BT_SETTINGS_RESULT = 122;
    
    //BT statics
    private static final UUID MY_UUID = UUID.fromString("7ca54fa9-0e33-4c3a-a280-22b58298b565");
    private static final String NAME = "encryptSMS";
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    
    //communication directions
    private static int DIRECTION_0 = 1;
    private static int DIRECTION_1 = 2;
    
    //dh bitlength
    private static final int BITLENGTH = 2048;
    private DHKeyPair ownKeyPair;
    private Contact newContact;
    
	private ArrayList<BluetoothDevice> pairedlist;
	
	//ui elements
	private ListView mBTListView;
	private ProgressDialog pd1;
	private ProgressDialog pd2;
	private Button mPanicButton;
	
	private Context context;
	
	//bluetooth adapter elements
	private BluetoothAdapter bluetoothAdapter;
	private boolean hasBTenabled = false;
	//bt handling service
	private BluetoothService btService;
	// String buffer for outgoing messages
    private StringBuffer outStringBuffer;
    //connected device
    private String connectedDeviceName;
    
    //DB password
    private byte[] password;
    
    //contact in case of rekeying
    private Contact contact;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_bluetooth);
		
		//load context
		context = this;
		
		//load password
		password = getIntent().getExtras().getByteArray(Constants.DB_PASSWORD_HASH);
		
		//load contact
		if(getIntent().getExtras().containsKey(Constants.CONTACT))
			contact = getIntent().getExtras().getParcelable(Constants.CONTACT);
		
		//load preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		//load UI
		mBTListView = (ListView)findViewById(R.id.list_bluetooth_list);
		mPanicButton = (Button) findViewById(R.id.list_bluetooth_panic_button);
		
		//set UI listeners
		mBTListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				showBTExchangeDialog(pairedlist.get((int) id));
			}
			
		});
		
		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
		
		initProgressDialogs();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if(resultCode != RESULT_OK){
				Toast.makeText(this, "An error occured while enabling BT.", Toast.LENGTH_LONG).show();
				hasBTenabled = false;
				finish();
			}else{
				setupBTService();
				updateBluetoothList();
			}
			break;
			
		case BT_SETTINGS_RESULT:
			updateBluetoothList();
			break;
			
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
		}
	}		
	
	@Override
	protected void onStart() {
		super.onStart();
		
		//enable BT adapter
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		 // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            hasBTenabled = true;
        // Otherwise, setup the chat session
        } else {
            if (btService == null) setupBTService();
            updateBluetoothList();
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btService.getState() == BluetoothService.STATE_NONE) {
              // Start the Bluetooth chat services
            	btService.start();
            }
        }
        
      //send user alive message
      SessionManagerService.alive(context);
	}
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (btService != null) btService.stop();
        bluetoothAdapter.disable();
    }
	
	private void setupBTService(){

        // Initialize the BluetoothChatService to perform bluetooth connections
        btService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        outStringBuffer = new StringBuffer("");
	}
	
	/**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (btService.getState() != BluetoothService.STATE_CONNECTED) {
        	//not connected
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            btService.write(send);

            // Reset out string buffer to zero
            outStringBuffer.setLength(0);
        }
    }
    
 // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
    	private String inputMessage = "";
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
//                	Toast.makeText(context, "connected!!!", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                if(!readMessage.endsWith(";;")){
                	inputMessage += readMessage;
                	break;
                }
                inputMessage += readMessage;
                readMessage = inputMessage;
                QRCodeReturn retval = QRCodeDecoder.decodeQRCodeContents(readMessage);
                if (retval.direction == DIRECTION_0) {
                	sendOwnContact(retval);
                	newContact.exchange_done = Contact.ACCEPTED;
                	if(contact != null){
                		newContact.id = contact.id;
                		updateKnownContact(newContact, contact);
                	}else{
                		saveNewContact(newContact);
                	}
                	finish();
                }else{
                	newContact = retval.contact;
                	newContact.exchange_done = Contact.ACCEPTED;
					DHKeyGen keygen = new DHKeyGen();
					newContact.secret = keygen.getSecret(ownKeyPair.keypair,
							retval.pubkey);
					// everything was exchanged, store to database
					if(contact != null){
						newContact.id = contact.id;
						updateKnownContact(newContact, contact);
					}else{
						saveNewContact(newContact);
					}
					pd2.dismiss();
					finish();
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(context, "device name: "+connectedDeviceName, Toast.LENGTH_LONG).show();
                break;
            case MESSAGE_TOAST:
            	Toast.makeText(context, msg.getData().getString(TOAST), Toast.LENGTH_LONG).show();
                break;
            }
        }
    };
    
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        btService.connect(device, secure);
    }
	
	private void updateBluetoothList(){
		
		//check if the device supports bluetooth
		if(bluetoothAdapter == null){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.no_bt_error_dialog_title);
			builder.setMessage(R.string.no_bt_error_dialog_message);
			builder.setNeutralButton(R.string.no_bt_error_dialog_btn, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
		}
		
		//check if it is enabled
//		if (!bluetoothAdapter.isEnabled()) {
//		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//		}
		
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter
				.getBondedDevices();
		
		pairedlist = new ArrayList<BluetoothDevice>();
		
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				if (isDeviceUsefulForSMS(device.getBluetoothClass()
						.getMajorDeviceClass())) {
					pairedlist.add(device);
				}
			}
		}
		
		if(pairedlist.size() <= 0){
			showBTPairedDialog();
		}
		
		updateBTListView();
	}
	
	private boolean isDeviceUsefulForSMS(int major) {
		switch (major) {
		case BluetoothClass.Device.Major.AUDIO_VIDEO:
			return false;
		case BluetoothClass.Device.Major.COMPUTER:
			return false;
		case BluetoothClass.Device.Major.HEALTH:
			return false;
		case BluetoothClass.Device.Major.IMAGING:
			return false;
		case BluetoothClass.Device.Major.MISC:
			return false;
		case BluetoothClass.Device.Major.NETWORKING:
			return false;
		case BluetoothClass.Device.Major.PERIPHERAL:
			return false;
		case BluetoothClass.Device.Major.PHONE:
			return true;
		case BluetoothClass.Device.Major.TOY:
			return false;
		case BluetoothClass.Device.Major.UNCATEGORIZED:
			return false;
		case BluetoothClass.Device.Major.WEARABLE:
			return false;
		default:
			return false;
		}
	}
	
private class BTDeviceArrayAdapter extends ArrayAdapter<BluetoothDevice> {
		
		private int layoutResourceId;
		private Context context;
		private List<BluetoothDevice> data;
		
		public BTDeviceArrayAdapter(Context context, int layoutResourceId,
				List<BluetoothDevice> data) {
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
				
				final BluetoothDeviceHolder holder = new BluetoothDeviceHolder();
				holder.txtName = (TextView) row
						.findViewById(R.id.show_bt_device_list_item_name);
				
				row.setTag(holder);
			} else {
				row = convertView;
			}
			
			BluetoothDevice device = data.get(position);
			BluetoothDeviceHolder holder = (BluetoothDeviceHolder) row.getTag();
			holder.txtName.setText(device.getName());
			
			return row;
		}
		
		private class BluetoothDeviceHolder {
			TextView txtName;
		}
	}

private void updateBTListView(){
	BTDeviceArrayAdapter btAdapter = new BTDeviceArrayAdapter(this,
			R.layout.show_bt_device_list_item, pairedlist);
	mBTListView.setAdapter(btAdapter);
}

@SuppressLint("ValidFragment")
private class NoBTDevicesDialog extends DialogFragment {

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.no_bt_devices_found_dialog_message)
        		.setTitle(R.string.no_bt_devices_found_dialog_title)
               .setPositiveButton(R.string.no_bt_devices_found_dialog_positive_btn, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {

                	   //Add BT Device Activity
                	   Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                	   startActivityForResult(settingsIntent, BT_SETTINGS_RESULT);
                   }
               })
               .setNegativeButton(R.string.no_bt_devices_found_dialog_negative_btn, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                	   finish();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

private void showBTPairedDialog(){
	FragmentManager fm = getSupportFragmentManager();
    NoBTDevicesDialog nobtDevicesDialog = new NoBTDevicesDialog();
    nobtDevicesDialog.show(fm, "fragment_edit_name");
}

@SuppressLint("ValidFragment")
private class BTExchangeDialog extends DialogFragment {
	
	private BluetoothDevice device;
	
	public BTExchangeDialog(BluetoothDevice device){
		this.device = device;
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(device.getName())
        		.setTitle(R.string.bt_exchange_dialog_title)
               .setPositiveButton(R.string.bt_exchange_dialog_positive_btn, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {

                	   //Add BT Device Activity
                	   performBTKeyExchange(device);
                   }
               })
               .setNegativeButton(R.string.bt_exchange_dialog_negative_btn, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                	   dismiss();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

private void showBTExchangeDialog(BluetoothDevice device){
	FragmentManager fm = getSupportFragmentManager();
	BTExchangeDialog btExchangeDialog = new BTExchangeDialog(device);
	btExchangeDialog.show(fm, "fragment_edit_name");
}

private void initProgressDialogs(){
	pd1 = new ProgressDialog(context);
	pd1.setMessage(getResources().getString(R.string.list_bluetooth_key_exchange_progress_dialog_message_1));
	pd2 = new ProgressDialog(context);
	
	pd2.setButton(Dialog.BUTTON_NEGATIVE, getResources().getString(R.string.list_bluetooth_key_exchange_progress_dialog_btn_neg), new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			btService.stop();
			btService = null;
		}
	});
}

private void performBTKeyExchange(BluetoothDevice device){
	
	pd2.setMessage(getResources().getString(R.string.list_bluetooth_key_exchange_progress_dialog_message_2)
			+ device.getName()
			+ getResources().getString(R.string.list_bluetooth_key_exchange_progress_dialog_message_3));
	

	if(btService == null) setupBTService();
	btService.connect(device, true);
	
	pd1.show();
	GenerateBigPrimeTask task = new GenerateBigPrimeTask();
	task.execute(""+BITLENGTH);
	
}

private class GenerateBigPrimeTask extends AsyncTask<String, Void, BigInteger[]>{
	
	@Override
	protected BigInteger[] doInBackground(String... params) {
//		SecureRandom srnd = new SecureRandom();
//		final BigInteger p = BigInteger.probablePrime(Integer.parseInt(params[0]), srnd);
//		final BigInteger g = BigInteger.probablePrime(Integer.parseInt(params[0]), srnd);
		return new BigInteger[] {Constants.p,Constants.g};
	}
	
	@Override
    protected void onPostExecute(BigInteger[] result) {
		super.onPostExecute(result);
		
		DHKeyGen keygen = new DHKeyGen();
		ownKeyPair = keygen.getKeyPair(result[0], result[1]);

		pd1.dismiss();

		pd2.show();
		
		WaitForConnectionTask wfct = new WaitForConnectionTask(ownKeyPair);
		wfct.execute(new String[]{});
		
    }
}

private class WaitForConnectionTask extends AsyncTask<String, Void, Boolean>{
	
	private DHKeyPair keypair;
	
	public WaitForConnectionTask(DHKeyPair keypair){
		this.keypair = keypair;
	}

	@Override
	protected Boolean doInBackground(String... params) {
		int i = 10;
		while(i > 0){
			if(btService != null && btService.getState() == BluetoothService.STATE_CONNECTED){
				i = -1;
				return true;
			}
			--i;
			try {
				synchronized(this){
				wait(1000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		pd2.dismiss();
		
		return false;
	}
	
	@Override
    protected void onPostExecute(Boolean connected) {
		super.onPostExecute(connected);
		
		if(connected){
			sendMessage(getOwnContactData(DIRECTION_0, keypair));
		}
    }
}

private String getOwnContactData(int direction, DHKeyPair kp){
	StringBuilder returnValue = new StringBuilder();
	returnValue.append("MECARD:");
	
	String number = preferences.getString(ContactsContract.Intents.Insert.PHONE, null);
	String username = preferences.getString(ContactsContract.Intents.Insert.NAME, null);
	String email = preferences.getString(ContactsContract.Intents.Insert.EMAIL, null);
	
	returnValue.append("N:").append(escapeMECARD(trim(username))).append(";");
	returnValue.append("TEL:").append(escapeMECARD(trim(number))).append(";");
	returnValue.append("EMAIL:").append(escapeMECARD(trim(email))).append(";");
	
	returnValue.append("D:").append(direction).append(";");
	
//	if(direction == DIRECTION_0){
//		returnValue.append("P:").append(escapeMECARD(trim(kp.p.toString()))).append(";");
//		returnValue.append("G:").append(escapeMECARD(trim(kp.g.toString()))).append(";");
//	}
	
	returnValue.append("PK:").append(new String(Base64.encode(kp.keypair.getPublic().getEncoded()))).append(";");
	
	if(returnValue.length() > 0){
		returnValue.append(";");
	}
	
	return returnValue.toString();

}

private String trim(String s) {
    if (s == null) { return null; }
    String result = s.trim();
    return result.length() == 0 ? null : result;
}

private String escapeMECARD(String input) {
    if (input == null || (input.indexOf(':') < 0 && input.indexOf(';') < 0)) { return input; }
    int length = input.length();
    StringBuilder result = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
        char c = input.charAt(i);
        if (c == ':' || c == ';') {
            result.append('\\');
        }
        result.append(c);
    }
    return result.toString();
}

private void sendOwnContact(QRCodeReturn retval){
	DHKeyGen keygen = new DHKeyGen();
	ownKeyPair = keygen.new DHKeyPair();
//	ownKeyPair.keypair = keygen.getKeyPair(retval.p, retval.g).keypair;
	ownKeyPair.keypair = keygen.getKeyPair(Constants.p, Constants.g).keypair;
	newContact = retval.contact;
	newContact.secret = keygen.getSecret(ownKeyPair.keypair, retval.pubkey);
	
	sendMessage(getOwnContactData(DIRECTION_1, ownKeyPair));
}

private void saveNewContact(Contact contact){
	ConversationsDataSource datasource = new ConversationsDataSource(this);
	datasource.open(password);
	
	datasource.createContact(contact);
	
	datasource.close();
}

private void updateKnownContact(Contact newContact, Contact oldContact){
	newContact.name = oldContact.name;
	newContact.phonenumber = oldContact.phonenumber;
	
	ConversationsDataSource datasource = new ConversationsDataSource(this);
	datasource.open(password);
	
	datasource.updateContact(newContact, oldContact);
	datasource.updateExchangeState(newContact);
	
	datasource.close();
}
		
}
