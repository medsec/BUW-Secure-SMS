package com.mslab.encryptsms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.crypto.digests.Skein;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.DatabaseController;
import com.mslab.encryptsms.misc.MediaScannerNotifier;
import com.mslab.encryptsms.misc.SMSDataStorage;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * This activity shows the main menu of the app. Form there the 
 * user can decide to use all functions of the app, which are reading or writing a sms and manage the contacts..
 * @author Paul Kramer
 *
 */
public class MainActivity extends Activity {
	
	private Button mReadSMSBtn;
	private Button mWriteSMSBtn;
	private Button mManageContactsBtn;
	private Button mLogoffBtn;
	
	private Context context;
	
	private byte[] password;
	
	private SharedPreferences preferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// load password
		password = getIntent().getExtras().getByteArray(Constants.DB_PASSWORD_HASH);
		
		//load preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		// load context
		context = this;
		
		// init UI elements
		mReadSMSBtn = (Button) findViewById(R.id.main_btn_read_sms);
		mWriteSMSBtn = (Button) findViewById(R.id.main_btn_write_sms);
		mManageContactsBtn = (Button) findViewById(R.id.main_btn_manage_contacts);
		mLogoffBtn = (Button) findViewById(R.id.main_btn_logoff);
		
		// init on click listener
		mWriteSMSBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context,
						SMSContactListActivity.class);
				intent.putExtra(Constants.DB_PASSWORD_HASH, password);
				startActivity(intent);
			}
		});
		
		mReadSMSBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, ReadSMSActivity.class);
				intent.putExtra(Constants.DB_PASSWORD_HASH, password);
				startActivity(intent);
			}
		});
		
		mManageContactsBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context,
						ContactManagerActivity.class);
				intent.putExtra(Constants.DB_PASSWORD_HASH, password);
				startActivity(intent);
			}
		});
		
		mLogoffBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// stopp Service
				logOff();
				
				// close main activity
				finish();
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
//	@Override
//	public void onCreateContextMenu(ContextMenu menu, View v,
//			ContextMenuInfo menuInfo) {
//		super.onCreateContextMenu(menu, v, menuInfo);
//		MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.activity_main, menu);
//	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.main_menu_settings: {
			Intent settingsActivity = new Intent(getBaseContext(),
					MainPreferencesActivity.class);
			startActivity(settingsActivity);
			break;
		}
		case R.id.main_menu_export: {
			showExportDialog();
			break;
		}
		case R.id.main_menu_import: {
			showImportDialog();
			break;
		}
		case R.id.main_menu_change_password: {
			Intent intent = new Intent(context, ChangePasswordActivity.class);
			intent.putExtra(Constants.DB_PASSWORD_HASH, password);
			startActivity(intent);
			break;
		}
		}
		return true;
	}
	
//	@Override
//	public boolean onContextItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case R.id.main_menu_settings: {
//			Intent settingsActivity = new Intent(getBaseContext(),
//					MainPreferencesActivity.class);
//			startActivity(settingsActivity);
//			break;
//		}
//		case R.id.main_menu_export: {
//			showExportDialog();
//			break;
//		}
//		case R.id.main_menu_import: {
//			showImportDialog();
//			break;
//		}
//		case R.id.main_menu_change_password: {
//			Intent intent = new Intent(context, ChangePasswordActivity.class);
//			intent.putExtra(Constants.DB_PASSWORD_HASH, password);
//			startActivity(intent);
//			break;
//		}
//		}
//		return super.onContextItemSelected(item);
//	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (preferences.getBoolean("cleardb", false)) {
			SharedPreferences.Editor edit = preferences.edit();
			edit.putBoolean("cleardb", false);
			edit.commit();
			
			// clear DB
			SMSDataStorage storage = new SMSDataStorage(context);
			storage.open();
			storage.deleteAllReceivedSMS();
			storage.close();
		}
		
		// send user alive message
		SessionManagerService.alive(context);
		
		if(getIntent().getExtras().getBoolean(Constants.SMS_RECEIVED)){
			Intent intent = new Intent(context, ReadSMSActivity.class);
			intent.putExtra(Constants.DB_PASSWORD_HASH, password);
			intent.putExtra(Constants.SMS_RECEIVED, true);
			startActivity(intent);
			//remove the extra
			getIntent().removeExtra(Constants.SMS_RECEIVED);
		}
	}
	
	@Override
	protected void onDestroy() {
		//stopp the service
		logOff();
		
		super.onDestroy();
	}
	
	private void showExportDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.main_export_dialog_title);
		
		// open output path
		File exportPath = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/"
				+ getResources().getString(R.string.app_name));
		exportPath.mkdir();
		// open database path
		final File inputPath = new File(
				new ConversationsDataSource(context).getDatabasePath());
		
		// Message including path
		String message = getResources().getString(
				R.string.main_export_dialog_message)
				+ " '" + exportPath.getAbsolutePath() + "'";
		builder.setMessage(message);
		
		builder.setPositiveButton(R.string.main_export_dialog_btn_pos,
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// because of the java api, we have to build our own
						// copy method
						File exportPath = new File(Environment
								.getExternalStorageDirectory()
								.getAbsolutePath()
								+ "/"
								+ getResources().getString(R.string.app_name)
								+ "/" + DatabaseController.DATABASE_NAME);
						//salt file
						File saltPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath()								+ "/"
								+ getResources().getString(R.string.app_name)
								+ "/"+ "salt");
						CopyTask cTask = new CopyTask();
						cTask.execute(new File[] { inputPath, exportPath, saltPath });
					}
				});
		
		builder.setCancelable(true);
		builder.setNegativeButton(R.string.main_export_dialog_btn_neg,
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		
		AlertDialog dialog = builder.create();
		dialog.show();
		
		// handle path error
		if (exportPath == null || !exportPath.exists()) {
			Toast.makeText(
					context,
					getResources().getString(
							R.string.main_export_dialog_error_folder)
							+ " '" + exportPath.getAbsolutePath() + "'",
					Toast.LENGTH_LONG).show();
			dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
		}
	}
	
	private class CopyTask extends AsyncTask<Object, Integer, Void> {
		
		ProgressDialog dialog;
		String exportPath;
		boolean error = false;
		
		@Override
		protected void onPreExecute() {
			// Setup Progress Dialog
			dialog = new ProgressDialog(context);
			dialog.setTitle(R.string.main_export_dialog_progress_title);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setMax(100);
			dialog.show();
		}
		
		@Override
		protected Void doInBackground(Object... params) {
			try {
				InputStream in = new FileInputStream((File)params[0]);
				OutputStream out = new FileOutputStream((File)params[1]);
				
				exportPath = ((File)params[1]).getAbsolutePath();
				
				long inputlength = ((File)params[0]).length();
				
				// Transfer bytes from in to out
				final int BUFFERSIZE = 4096;
				byte[] buf = new byte[BUFFERSIZE];
				int len;
				int chunk = 0;
				while ((len = in.read(buf, chunk * BUFFERSIZE, BUFFERSIZE)) > 0) {
					out.write(buf, chunk * BUFFERSIZE, len);
					publishProgress((int) (((float) (chunk * BUFFERSIZE + len))
							/ ((float) inputlength) * 100.f));
				}
				in.close();
				out.close();
				
				
				//if params length > 3 then this was an import call
				if(params.length > 3){
					//import salt
					InputStream saltin = new FileInputStream((File)params[2]);
					byte[] salt = new byte[8];
					saltin.read(salt);
					saltin.close();
					
					//load password
					byte[] dbPassword = Constants.concatenateArrays(salt, ((String)params[3]).getBytes());
					Skein hash = new Skein(256, 256);
					hash.update(dbPassword, 0, dbPassword.length);
					dbPassword = hash.doFinal();
					
					ConversationsDataSource datasource = new ConversationsDataSource(context);
					if(datasource.open(dbPassword)){
						//everything was ok!
						datasource.postProcessImport();
						datasource.close();
						
						//overwrite password
						password = dbPassword;
						
						//store salt to the preferences
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString(Constants.DATABASE_SALT, Base64.encodeToString(salt, Base64.DEFAULT));
						editor.commit();
					}else{
						File file = new File(datasource.getDatabasePath());
						file.delete();
						datasource.open(password);
						datasource.postProcessImport();
						datasource.close();
						error = true;
					}
				}else{
					//export salt
					OutputStream saltout = new FileOutputStream((File)params[2]);
					byte[] buffer = Base64.decode(preferences.getString(Constants.DATABASE_SALT, null), Base64.DEFAULT);
					saltout.write(buffer);
					saltout.close();
				}
				
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			if(error)Toast.makeText(context, getResources().getString(R.string.main_import_dialog_error_password), Toast.LENGTH_LONG).show();
			// scan folder to show file on windows / linux file manager
			new MediaScannerNotifier(context, exportPath, "*.db");
			
			super.onPostExecute(result);
		}
	}
	
	private void showImportDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.main_import_dialog_title);
		
		// open SD Card path
		File importPath = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/"
				+ getResources().getString(R.string.app_name)
				+ "/"
				+ DatabaseController.DATABASE_NAME);
		
		// open database path
		final File exportPath = new File(
				new ConversationsDataSource(context).getDatabasePath());
		
		// Message including path
		String message = getResources().getString(
				R.string.main_import_dialog_message_1)
				+ " '" + importPath.getAbsolutePath() + "'";
		message += "\n\n";
		message += getResources().getString(
				R.string.main_import_dialog_message_2);
		message += "\n\n";
		message += getResources().getString(
				R.string.main_import_dialog_message_3);
		builder.setMessage(message);
		
		//get the passwort again
		final EditText pwInput = new EditText(context);
		pwInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(pwInput);
		
		builder.setPositiveButton(R.string.main_import_dialog_btn_pos,
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// because of the java api, we have to build our own
						// copy method
						File importPath = new File(Environment
								.getExternalStorageDirectory()
								.getAbsolutePath()
								+ "/"
								+ getResources().getString(R.string.app_name)
								+ "/" + DatabaseController.DATABASE_NAME);
						//salt file
						File saltPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath()								+ "/"
								+ getResources().getString(R.string.app_name)
								+ "/"+ "salt");
						CopyTask cTask = new CopyTask();
						cTask.execute(new Object[] { importPath, exportPath, saltPath, pwInput.getText().toString() });
					}
				});
		
		builder.setCancelable(true);
		builder.setNegativeButton(R.string.main_import_dialog_btn_neg,
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		
		final AlertDialog dialog = builder.create();
		
		pwInput.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
				if(s.length()> 0){
					button.setEnabled(true);
				}else{
					button.setEnabled(false);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		dialog.show();
		Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		button.setEnabled(false);
		
		// handle path error
		if (importPath == null || !importPath.exists()) {
			Toast.makeText(
					context,
					getResources().getString(
							R.string.main_import_dialog_error_folder_1)
							+ " '"
							+ importPath.getAbsolutePath()
							+ "' "
							+ getResources().getString(
									R.string.main_import_dialog_error_folder_2),
					Toast.LENGTH_LONG).show();
			dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
		}
	}
	
	private void logOff(){
		Intent intent = new Intent(context, SessionManagerService.class);
		intent.putExtra(Constants.STOP_SERVICE, true);
		context.startService(intent);
	}
}
