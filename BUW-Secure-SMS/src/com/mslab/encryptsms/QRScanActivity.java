package com.mslab.encryptsms;

import java.math.BigInteger;

import org.bouncycastle.util.encoders.Base64;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.Contents;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.DHKeyGen;
import com.mslab.encryptsms.misc.DHKeyGen.DHKeyPair;
import com.mslab.encryptsms.misc.QRCodeDecoder;
import com.mslab.encryptsms.misc.QRCodeEncoder;
import com.mslab.encryptsms.misc.QRCodeReturn;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * This activity manages the key-exchange by QR-code. It shows a QR-code and a button to scan another code.
 * From here, the key-exchange by QR-code can be initiated.  
 * @author Paul Kramer
 *
 */
public class QRScanActivity extends Activity {
	
	private static int BITLENGTH = 2048;
	
	//UI elements
	private Button mButton;
	private ImageView mQRImage;
	private Button mPanicButton;
	
	
	private SharedPreferences preferences;
	
	private Contact newContact;
	private DHKeyPair ownKeyPair;
	private Context context;
	
	//static values
	public static String DIRECTION = "D";
	private static int DIRECTION_0 = 1;
	private static int DIRECTION_1 = 2;
	
	//dialogs
	private ProgressDialog pd;
	
	//DB password
	private byte[] password;
	
	//Contact to exchange a new key
	private Contact contact;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qr_scan);
		
		//load context
		context = this;
		
		//load password
		password = getIntent().getExtras().getByteArray(Constants.DB_PASSWORD_HASH);
		
		//load contact, if it exists
		if(getIntent().getExtras().containsKey(Constants.CONTACT)){
			contact = getIntent().getExtras().getParcelable(Constants.CONTACT);
		}
		
		//load preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		
		//load UI
		mButton = (Button)findViewById(R.id.qr_scan_btn);
		mQRImage = (ImageView)findViewById(R.id.qr_scan_qr_code);
		mPanicButton = (Button) findViewById(R.id.qr_scan_panic_button);
		
		
		//init UI listeners
		mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mButton
						.getText()
						.toString()
						.equalsIgnoreCase(
								getResources().getString(
										R.string.qr_scan_btn_scan_text))) {
					IntentIntegrator integrator = new IntentIntegrator(
							QRScanActivity.this);
					integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
				}else{
					// everything was exchanged
					//if contact is not null, we have to update an existing contact
					if(contact != null){
						newContact.id = contact.id;
						newContact.exchange_done = Contact.ACCEPTED;
						updateKnownContact(newContact, contact);
					}else{
						//otherwise: store the new contact to database
						newContact.exchange_done = Contact.ACCEPTED;
						saveNewContact(newContact);
					}
					finish();
				}
			}
		});
		
		//prepare Keypair
//		DHKeyGen keygen = new DHKeyGen();
//		ownKeyPair = keygen.getKeyPair(BITLENGTH);
//		
//				Bitmap barcode = encodeBarcode(Contents.Type.CONTACT, bundleOwnContact(ownKeyPair, DIRECTION_0));
//				showBarcode(barcode);
		
		pd = ProgressDialog
				.show(context,
						"",
						getResources()
								.getString(
										R.string.qr_scan_progress_dialog_message),
						true);
		pd.setCancelable(false);
		GenerateBigPrimeTask task = new GenerateBigPrimeTask();
		task.execute(""+BITLENGTH);
	
		//Add panic action
		Constants.setPanicAction(mPanicButton, context);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// send user alive message
		SessionManagerService.alive(context);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (resultCode == RESULT_OK) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(
					requestCode, resultCode, data);
			if (scanResult != null) {
				
				QRCodeReturn retval = null;
				
				try {
					retval = QRCodeDecoder.decodeQRCodeContents(scanResult
							.getContents());
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println(retval.contact.toString());
				if (retval.direction == DIRECTION_0) {
					updateQRCode(retval);
					mButton.setText(R.string.qr_scan_btn_scan_done_text);
				} else {
					newContact = retval.contact;
					DHKeyGen keygen = new DHKeyGen();
					newContact.secret = keygen.getSecret(ownKeyPair.keypair,
							retval.pubkey);
					// everything was exchanged
					//if contact is not null, we have to update an existing contact
					if(contact != null){
						newContact.id = contact.id;
						newContact.exchange_done = Contact.ACCEPTED;
						updateKnownContact(newContact, contact);
					}else{
						newContact.exchange_done = Contact.ACCEPTED;
						//otherwise: store the new contact to database
						saveNewContact(newContact);
					}
					finish();
				}
			}
		}
	}
	
	private Bitmap encodeBarcode(String type, Bundle data) {
		// Find screen size
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int width = metrics.widthPixels;
		int height = metrics.heightPixels;
		
		int smallerDimension = width < height ? width : height;
		smallerDimension = smallerDimension * 8 / 9;
		// Encode with a QR Code image
		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder("bla", data,
				type, BarcodeFormat.QR_CODE.toString(),
				smallerDimension);
		
		Bitmap bitmap = null;
		try {
			bitmap = qrCodeEncoder.encodeAsBitmap();
			
		} catch (WriterException e) {
			e.printStackTrace();
		}
		
		return bitmap;
	}
	
	private Bundle bundleOwnContact(DHKeyPair keypair, int direction ){
		Bundle bundle = new Bundle();
		
		bundle.putInt(DIRECTION, direction);
		
		String number = preferences.getString(ContactsContract.Intents.Insert.PHONE, null);
		if(number == null){
			//telefonnummer abfragen und in preferences speichern
			Toast.makeText(getApplicationContext(), "keine Telefonnummer gefunden", Toast.LENGTH_LONG).show();
		}
		
		
		bundle.putString(ContactsContract.Intents.Insert.PHONE, number);
		
		String username = preferences.getString(ContactsContract.Intents.Insert.NAME, null);
		if(username == null){
			//telefonnummer abfragen und in preferences speichern
			Toast.makeText(getApplicationContext(), "keinen Namen gefunden", Toast.LENGTH_LONG).show();
		}

		
		bundle.putString(ContactsContract.Intents.Insert.NAME, username);
		
		String email = preferences.getString(ContactsContract.Intents.Insert.EMAIL, null);
		
		bundle.putString(ContactsContract.Intents.Insert.EMAIL, email);
		
		// add public key
		bundle.putByteArray(DHKeyGen.PUBLIC_KEY, Base64.encode(keypair.keypair.getPublic()
				.getEncoded()));

//		if(direction == DIRECTION_0){
//			bundle.putString(DHKeyGen.P, new String(keypair.p.toString()));
//			bundle.putString(DHKeyGen.G, new String(keypair.g.toString()));
//		}
		
		return bundle;
	}
	
	private void updateQRCode(QRCodeReturn retval){
		DHKeyGen keygen = new DHKeyGen();
//		ownKeyPair.keypair = keygen.getKeyPair(retval.p, retval.g).keypair;
		ownKeyPair.keypair = keygen.getKeyPair(Constants.p, Constants.g).keypair;
		newContact = retval.contact;
		newContact.secret = keygen.getSecret(ownKeyPair.keypair, retval.pubkey);
		
		Bitmap barcode = encodeBarcode(Contents.Type.CONTACT, bundleOwnContact(ownKeyPair, DIRECTION_1));
		showBarcode(barcode);
	}
	
	private void showBarcode(Bitmap barcode){
		mQRImage.setImageBitmap(barcode);
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
	
	private class GenerateBigPrimeTask extends AsyncTask<String, Void, BigInteger[]>{

		@Override
		protected BigInteger[] doInBackground(String... params) {
//			SecureRandom srnd = new SecureRandom();
//			final BigInteger p = BigInteger.probablePrime(Integer.parseInt(params[0]), srnd);
//			final BigInteger g = BigInteger.probablePrime(Integer.parseInt(params[0]), srnd);
			return new BigInteger[] {Constants.p,Constants.g};
		}
		
		@Override
	    protected void onPostExecute(BigInteger[] result) {
			super.onPostExecute(result);
			
			DHKeyGen keygen = new DHKeyGen();
			ownKeyPair = keygen.getKeyPair(result[0], result[1]);
			Bitmap barcode = encodeBarcode(Contents.Type.CONTACT, bundleOwnContact(ownKeyPair, DIRECTION_0));
			showBarcode(barcode);
			pd.dismiss();
	    }
		
	}
	
}
