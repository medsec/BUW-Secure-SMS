package com.mslab.encryptsms;

import org.bouncycastle.crypto.digests.Skein;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.ConversationsDataSource;

/**
 * Activity which displays a screen to thange the login password.
 * The database password will be changed too and for both passwords two new salts are generated.
 * @author Paul Kramer
 *
 */
public class ChangePasswordActivity extends Activity {
	
	//UI Elements
	private EditText mOldPasswordInput;
	private EditText mNewPasswordInput;
	private EditText mNewPasswordRepeatInput;
	private ProgressBar mProgressBar;
	private Button mChangePasswordButton;
	
	//old password
	private String passwordHash;
	private String dbPasswordHash;
	
	//preferences
	private SharedPreferences preferences;
	
	//Context
	private Context context;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_password);
		
		//init context
		context = this;
		
		//init UI elements
		mOldPasswordInput = (EditText) findViewById(R.id.change_password_old_password_input);
		mNewPasswordInput = (EditText) findViewById(R.id.change_password_new_password_input);
		mNewPasswordRepeatInput = (EditText) findViewById(R.id.change_password_new_password_repeat_input);
		
		mProgressBar = (ProgressBar) findViewById(R.id.change_password_progress);
		mProgressBar.setMax(12);
		mProgressBar.setProgress(0);
		
		mChangePasswordButton = (Button) findViewById(R.id.change_password_button);
		
		//init preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		//load old password
		passwordHash = preferences.getString(Constants.PASSWORD_HASH, null);
		dbPasswordHash = preferences.getString(Constants.DB_PASSWORD_HASH, null);
		
		//set click listener
		mChangePasswordButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				byte[] newpwsalt = FirstStartActivity.saltShaker(8);
				byte[] newdbsalt = FirstStartActivity.saltShaker(8);
				byte[] oldpwsalt = Base64.decode(preferences.getString(Constants.PASSWORD_SALT, null), Base64.DEFAULT);
				byte[] olddbsalt = Base64.decode(preferences.getString(Constants.DATABASE_SALT, null), Base64.DEFAULT);
				
				byte[] oldpwbytes = Constants.concatenateArrays(oldpwsalt, mOldPasswordInput.getText().toString().getBytes());
				
				Skein hash = new Skein(256, 256);
				hash.update(oldpwbytes, 0, oldpwbytes.length);
				
				//old password is not correct
				if(!passwordHash.equals(Base64.encodeToString(hash.doFinal(), Base64.DEFAULT))){
					mOldPasswordInput.setText("");
					Toast.makeText(context, getResources().getString(R.string.change_password_old_pw_error), Toast.LENGTH_LONG).show();
					return;
				}
				
				String oldPassword = mOldPasswordInput.getText().toString(); 
				byte[] oldPasswordBytes = Constants.concatenateArrays(oldpwsalt, oldPassword.getBytes());
				byte[] oldDBPasswortBytes = Constants.concatenateArrays(olddbsalt, oldPassword.getBytes());
				
				//new password was not correct

				String newPassword = mNewPasswordInput.getText().toString(); 
				
				if(!newPassword.equals(mNewPasswordRepeatInput.getText().toString())){
					mNewPasswordInput.setText("");
					mNewPasswordRepeatInput.setText("");
					Toast.makeText(context, getResources().getString(R.string.change_password_new_pw_error), Toast.LENGTH_LONG).show();
					return;
				}
				
				//all password are ok, perform all changes
				byte[] newPasswordBytes = Constants.concatenateArrays(newpwsalt, newPassword.getBytes());
				hash = new Skein(256, 256);
				hash.update(newPasswordBytes, 0, newPasswordBytes.length);
				
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(Constants.PASSWORD_HASH, Base64.encodeToString(hash.doFinal(), Base64.DEFAULT));
				editor.putString(Constants.PASSWORD_SALT, Base64.encodeToString(newpwsalt, Base64.DEFAULT));
				editor.putString(Constants.DATABASE_SALT, Base64.encodeToString(newdbsalt, Base64.DEFAULT));

				editor.commit();
				
				//prepare old DB password hash
				hash = new Skein(256, 256);
				hash.update(oldDBPasswortBytes, 0, oldDBPasswortBytes.length);
				oldDBPasswortBytes = hash.doFinal();
				
				//prepare new DB password hash
				byte[] newDBPasswordBytes = Constants.concatenateArrays(newdbsalt, newPassword.getBytes());
				hash = new Skein(256, 256);
				hash.update(newDBPasswordBytes, 0, newDBPasswordBytes.length);
				newDBPasswordBytes = hash.doFinal();
				
				//changes the password
				ChangePasswordTask task = new ChangePasswordTask();
				task.execute(new byte[][]{oldDBPasswortBytes, newDBPasswordBytes});
			}
		});
		
		//set key listener
		mNewPasswordInput.addTextChangedListener(new TextWatcher() {
			
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
		
	}
	
private class ChangePasswordTask extends AsyncTask<byte[], Integer, byte[]> {
		
		ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {
			// Setup Progress Dialog
			dialog = new ProgressDialog(context);
			dialog.setTitle(R.string.change_password_dialog_title);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.show();
		}
		
		@Override
		protected byte[] doInBackground(byte[]... params) {
			
//			//Copy old Database
//			Copy(params[0], params[1]);
			
			ConversationsDataSource dataSource = new ConversationsDataSource(context);
			dataSource.open(params[0]);
			dataSource.changeKey(params[1]);
			dataSource.close();
			
			return params[1];
		}
		
		@Override
		protected void onPostExecute(byte[] result) {
			dialog.dismiss();
			
			super.onPostExecute(null);
			
			Intent intent = new Intent(context, MainActivity.class);
			intent.putExtra(Constants.DB_PASSWORD_HASH, result);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
	}
	
}
