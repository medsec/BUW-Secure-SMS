package com.mslab.encryptsms;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.crypto.digests.Skein;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.ConversationsDataSource;

/**
 * This activity will be shown the first timne, the app is started. It shows some password input fields an asks for some information.
 * After the password was entered it save the hash to the preferences and creates the database. 
 * @author Paul Kramer
 *
 */
public class FirstStartActivity extends Activity {
	
	private TextView mPasswordView1;
	private TextView mPasswordView2;
	private ProgressBar mProgressBar;
	private Button mRegisterButton;
	
	private TextView mNameView;
	private TextView mPhoneView;
	private TextView mEmailView;
	
	private Context context;
	private ProgressDialog pd;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_first_start);
		
		context = this;
		
		mPasswordView1 = (TextView)findViewById(R.id.first_start_password_input);
		mPasswordView2 = (TextView)findViewById(R.id.first_start_password_input_2);
		mProgressBar = (ProgressBar)findViewById(R.id.first_start_password_progress);
		mProgressBar.setMax(12);
		mProgressBar.setProgress(0);
		mRegisterButton = (Button)findViewById(R.id.first_start_register_button);
		mNameView = (TextView)findViewById(R.id.first_start_name_input);
		mPhoneView = (TextView)findViewById(R.id.first_start_phone_input);
		mEmailView = (TextView)findViewById(R.id.first_start_email_input);
		
		//fill edittexts
		mEmailView.setText(getEMail());
		mPhoneView.setText(((TelephonyManager)  getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
		
		mPasswordView1.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				mProgressBar.setProgress(s.length());
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
		
		mRegisterButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mPasswordView1.getText().length() == mPasswordView2.getText().length() && mPasswordView1.getText().length() > 0){
					if(mPasswordView1.getText().toString().equals(mPasswordView2.getText().toString())){
						//save password
						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
						SharedPreferences.Editor editor = preferences.edit();
						
						String password = mPasswordView1.getText().toString();
						String name = mNameView.getText().toString();
						String phone = mPhoneView.getText().toString();
						String email = mEmailView.getText().toString();
						
						//generate salts
						byte[] pwsalt = saltShaker(8);
						byte[] dbsalt = saltShaker(8);
						
						//store salts
						editor.putString(Constants.PASSWORD_SALT, Base64.encodeToString(pwsalt, Base64.DEFAULT));
						editor.putString(Constants.DATABASE_SALT, Base64.encodeToString(dbsalt, Base64.DEFAULT));
						
						Skein hash = new Skein(256, 256);
						byte[] passwordBytes = Constants.concatenateArrays(pwsalt, password.getBytes());
						hash.update(passwordBytes, 0, passwordBytes.length);
						byte[] pwhash = hash.doFinal();
						
						editor.putString(Constants.PASSWORD_HASH, Base64.encodeToString(pwhash, Base64.DEFAULT));
						editor.putString(ContactsContract.Intents.Insert.NAME, name);
						editor.putString(ContactsContract.Intents.Insert.PHONE, phone);
						editor.putString(ContactsContract.Intents.Insert.EMAIL, email);
						
						byte[] dbPassword = Constants.concatenateArrays(dbsalt, password.getBytes());
						hash = new Skein(256, 256);
						hash.update(dbPassword, 0, dbPassword.length);
						dbPassword = hash.doFinal();
						
						// create database
						pd = ProgressDialog
								.show(context,
										"",
										getResources()
												.getString(
														R.string.first_start_progress_dialog_message),
										true);
						pd.setCancelable(false);
						CreateDatabaseTask task = new CreateDatabaseTask();
						task.execute(dbPassword);
						
						//safe data
						editor.commit();
						
						//open next activity
						Intent intent = new Intent(context, MainActivity.class);
						intent.putExtra(Constants.DB_PASSWORD_HASH, dbPassword);
						startActivity(intent);
					}else{
						Toast.makeText(context, getResources().getString(R.string.first_start_error_register), Toast.LENGTH_LONG).show();
					}
				}else{
					Toast.makeText(context, getResources().getString(R.string.first_start_error_register_length), Toast.LENGTH_LONG).show();
				}
			}
		});
		
	}
	
	/**
	 * This method takes the email address from the Google account of the device, if there is one.
	 * @return the email address.
	 */
	public String getEMail(){
	    AccountManager manager = AccountManager.get(this); 
	    Account[] accounts = manager.getAccountsByType("com.google"); 
	    List<String> possibleEmails = new LinkedList<String>();

	    for (Account account : accounts) {
	      // TODO: Check possibleEmail against an email regex or treat
	      // account.name as an email address only for certain account.type values.
	      possibleEmails.add(account.name);
	    }

	    if(!possibleEmails.isEmpty() && possibleEmails.get(0) != null){
	        return possibleEmails.get(0);
	    }else
	        return null;
	}
	private class CreateDatabaseTask extends AsyncTask<byte[], Void, String>{

		@Override
		protected String doInBackground(byte[]... params) {
			ConversationsDataSource datasource = new ConversationsDataSource(context);
			datasource.open(params[0]);
			datasource.close();
			return null;
		}
		
		@Override
	    protected void onPostExecute(String result) {
			super.onPostExecute(result);
			pd.dismiss();
	    }
		
	}
	/**
	 * Generates a random length long salt and returnes it as byte array.
	 * @param length - the length of the salt in bytes
	 * @return the salt
	 */
	public static byte[] saltShaker(int length){
		byte[] salt = new byte[length];
		SecureRandom sr = null;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");

			int i = 0;
			while(i < length){
				ByteBuffer bb = ByteBuffer.allocate(8);  
				byte[] part = bb.putLong(sr.nextLong()).array();
				int bytesToCopy = 8;
				if(length - i < 8) bytesToCopy = length - i;
				System.arraycopy(part, 0, salt, i, bytesToCopy);
				i += 8;
			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return salt;
	}
}
