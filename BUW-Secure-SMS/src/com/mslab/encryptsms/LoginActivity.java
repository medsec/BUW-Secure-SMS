package com.mslab.encryptsms;

import java.util.Arrays;

import org.bouncycastle.crypto.digests.Skein;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	
	public static final String PREFERENCES_NAME = "encryptsms";
	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private byte[] mPassword;
	
	// UI references.
	private EditText mPasswordView;
	private Button mLoginButton;
	
	private SharedPreferences preferences;
	
	private Context context;
	
	private Bundle bundle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login);
		
		context = LoginActivity.this;
		
		loadPreferences();
		
		mPasswordView = (EditText) findViewById(R.id.password);
		mLoginButton = (Button)findViewById(R.id.sign_in_button);
		
		if(mPassword.length <= 1){
			startActivity(new Intent(this, FirstStartActivity.class));
		}
		
		
		mLoginButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
//				String password = "test";
				String password = mPasswordView.getText().toString();
				byte[] passwordBytes = Constants.concatenateArrays(Base64.decode(preferences.getString(Constants.PASSWORD_SALT, ""), Base64.DEFAULT), password.getBytes());
				Skein hash = new Skein(256, 256);
				hash.update(passwordBytes, 0, passwordBytes.length);
				
				byte[] pwhash = hash.doFinal();
				
				if(Arrays.equals(pwhash,mPassword)){
					//pw ok
					
					//prepare DB password
					byte[] dbPassword = Constants.concatenateArrays(Base64.decode(preferences.getString(Constants.DATABASE_SALT, ""), Base64.DEFAULT), password.getBytes());
					hash = new Skein(256, 256);
					hash.update(dbPassword, 0, dbPassword.length);
					dbPassword = hash.doFinal();
					
					//clear input field
					mPasswordView.setText("");
					
					//start the session manager service
					Intent intent = new Intent(context, SessionManagerService.class);
					intent.putExtra(Constants.DB_PASSWORD_HASH, dbPassword);
					context.startService(intent);
					
					//start the main activity
					intent = new Intent(context, MainActivity.class);
					intent.putExtra(Constants.DB_PASSWORD_HASH, dbPassword);
					if(bundle != null){
						intent.putExtra(Constants.SMS_RECEIVED, bundle.getBoolean(Constants.SMS_RECEIVED));
					}
					if(getIntent().getExtras() != null &&
							getIntent().getExtras().containsKey(Constants.AUTO_LOGOFF)){
						finish();
					}else{
						startActivity(intent);
					}
				}else{
					Toast.makeText(context, getResources().getString(R.string.error_incorrect_password), Toast.LENGTH_LONG).show();
					mPasswordView.setText("");
				}
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		loadPreferences();
		
		bundle = getIntent().getExtras();
		//remove the extra
		getIntent().removeExtra(Constants.SMS_RECEIVED);
		
	}
	
	
	private void loadPreferences(){
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		mPassword = Base64.decode(preferences.getString(Constants.PASSWORD_HASH, ""), Base64.DEFAULT);
	}
}
	
	