package com.mslab.encryptsms.services;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;

import com.mslab.encryptsms.LoginActivity;
import com.mslab.encryptsms.R;
import com.mslab.encryptsms.ReadSMSActivity;
import com.mslab.encryptsms.SMSKeyExchangeActivity;
import com.mslab.encryptsms.ShowConversationActivity;
import com.mslab.encryptsms.WriteSMSActivity;
import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Contact;
import com.mslab.encryptsms.misc.Conversation;
import com.mslab.encryptsms.misc.ConversationsDataSource;
import com.mslab.encryptsms.misc.ReceivedSMS;
import com.mslab.encryptsms.misc.SMSDataStorage;
import com.mslab.encryptsms.misc.SMSNotifierHelper;
import com.mslab.encryptsms.receiver.SMSDeliveredReceiver;
import com.mslab.encryptsms.receiver.SMSSentReceiver;
import com.mslab.smsutils.SMSUtils;


/**
 * This service represents the background logic of the whole application. It manages passwords and incoming messages.
 * @author Paul Kramer
 *
 */
public class SessionManagerService extends Service{
	
	private byte[] mPassword = null;
	private final IBinder mBinder = new ServiceBinder();
	private Context context;
	
	private String IN_SERVICE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	
	private static int AUTO_STOP_REQUEST_CODE = 123;
	
	// Define a handler and a broadcast receiver
	private final Handler mHandler = new Handler();
	private final BroadcastReceiver mInServiceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Handle reciever
			String mAction = intent.getAction();
			
			if (mAction.equals(IN_SERVICE_ACTION)) {
				if (unsendMessages.size() == 0) {
					//take all unsend messages from database
					ConversationsDataSource dataSource = new ConversationsDataSource(context);
					dataSource.open(mPassword);
					ArrayList<Conversation> conversationslist = dataSource.getAllOutboxConversations();
					for(Conversation conversation : conversationslist){
						unsendMessages.add(conversation.id);
					}
					dataSource.close();
				}
				for (long conversationID : unsendMessages) {
					resendSMS(conversationID);
				}
				unsendMessages.clear();
			}
		}
	};
	
	private ArrayList<Long> unsendMessages = new ArrayList<Long>();
	
	// Flag if receiver is registered 
	private boolean mReceiversRegistered = false;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		context = SessionManagerService.this;
		System.out.println("onCreate");
		
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		System.out.println("onStart");

		Bundle bundle = null;
		if(intent != null) bundle = intent.getExtras();
		if(bundle != null){
		
		if(bundle.containsKey(Constants.DB_PASSWORD_HASH)){
			mPassword = bundle.getByteArray(Constants.DB_PASSWORD_HASH);
			
			// Register Sync Recievers
			  IntentFilter intentToReceiveFilter = new IntentFilter();
			  intentToReceiveFilter.addAction(IN_SERVICE_ACTION);
			  this.registerReceiver(mInServiceReceiver, intentToReceiveFilter, null, mHandler);
			  mReceiversRegistered = true;
		}else if(bundle.containsKey(Constants.SMS_RECEIVED)){
			//show notification
			SMSNotifierHelper
			.sendNotification(
					context,
					SessionManagerService.class,
					context.getResources()
							.getString(
									R.string.sms_receiver_title_sms_notification),
					context.getResources()
							.getString(
									R.string.sms_receiver_message_sms_notification),
					2, true, true);
			if (mPassword != null) { // service already running
			
				// send sms received broadcast
				Intent broadcast = new Intent();
				broadcast
						.setAction(ShowConversationActivity.SHOW_SMS_BROADCAST);
				sendBroadcast(broadcast);
				
			} else {// service is not running, show notification and stop
					// service to save energy
				stopSelf();
			}
		}else if(bundle.containsKey(Constants.RESEND_SMS)){
			//encipher SMS and send again
			unsendMessages.add(bundle.getLong(Conversation.CONVERSATION_ID));
		}else if(bundle.containsKey(Constants.STOP_SERVICE)){
			//remove all old alarms from alarmmanager
			stopAutoLogoff();
            //stop the service
			stopSelf();
		}else if(bundle.containsKey(Constants.USER_ALIVE_MESSAGE)){
			//activate alarm manager for auto logoff
			Calendar cur_cal = Calendar.getInstance();
            cur_cal.setTimeInMillis(System.currentTimeMillis());
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int autologofftime = Integer.parseInt(preferences.getString(Constants.AUTO_LOGOFF_TIME, ""+600));
            cur_cal.add(Calendar.SECOND, autologofftime);
            Intent alarmIntent = new Intent(SessionManagerService.this, SessionManagerService.class);
            alarmIntent.putExtra(Constants.STOP_SERVICE_BY_ALARM, true);
            PendingIntent pi = PendingIntent.getService(SessionManagerService.this, AUTO_STOP_REQUEST_CODE, alarmIntent, 0);
            AlarmManager alarm_manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarm_manager.set(AlarmManager.RTC_WAKEUP, cur_cal.getTimeInMillis(), pi);
		}else if(bundle.containsKey(Constants.SHOW_RECEIVED_SMS)){
			if(mPassword != null){ //service already running, show sms
				Intent startIntent = new Intent(this, ReadSMSActivity.class);
				startIntent.putExtra(Constants.SMS_RECEIVED, true);
				startIntent.putExtra(Constants.DB_PASSWORD_HASH, mPassword);
				startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startIntent);
			}else{
				//service is not running, go to login screen
				Intent startIntent = new Intent(this, LoginActivity.class);
				startIntent.putExtra(Constants.SMS_RECEIVED, true);
				startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startIntent);
				//stopp the service
				stopSelf();
			}
		}else if(bundle.containsKey(Constants.STOP_SERVICE_BY_ALARM)){
			stopSelf();
			Intent stopIntent = new Intent(SessionManagerService.this, LoginActivity.class);
			stopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			stopIntent.putExtra(Constants.AUTO_LOGOFF, true);
			startActivity(stopIntent);
		}else if(bundle.containsKey(Constants.STOP_SERVICE_PANIC)){
			stopAutoLogoff();
            //stop the service
			stopSelf();
		}else if(bundle.containsKey(Constants.SMS_KEY_EXCHANGE)){
			//show notification
			SMSNotifierHelper
			.sendNotification(
					context,
					SessionManagerService.class,
					context.getResources()
							.getString(
									R.string.sms_receiver_title_sms_notification),
					context.getResources()
							.getString(
									R.string.sms_receiver_message_sms_notification_key_exchange),
					2, true, true);
			if (mPassword != null) { // service already running
				
				//remove SMS from Database
				SMSDataStorage datastorage = new SMSDataStorage(context);
				datastorage.open();
				ReceivedSMS rsms = bundle.getParcelable(Constants.SMS_KEY);
				datastorage.deleteReceivedSMS(rsms);
				datastorage.close();
			
				Intent startIntent = new Intent(context, SMSKeyExchangeActivity.class);
				startIntent.putExtra(Constants.SMS_KEY_EXCHANGE, true);
				startIntent.putExtra(Constants.SMS_KEY, bundle.getParcelable(Constants.SMS_KEY));
				startIntent.putExtra(Constants.DB_PASSWORD_HASH, mPassword);
				startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startIntent);
								
			} else {// service is not running, show notification and stop
					// service to save energy
				stopSelf();
			}
		}
		}
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public class ServiceBinder extends Binder {
	   public SessionManagerService getService() {
	      return SessionManagerService.this;
	    }
	  }
	
//	private boolean checkWheterActivityIsInForeground(){
//		ArrayList<String> runningactivities = new ArrayList<String>();
//
//		ActivityManager activityManager = (ActivityManager)getBaseContext().getSystemService (Context.ACTIVITY_SERVICE); 
//
//		List<RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE); 
//
//		    for (int i1 = 0; i1 < services.size(); i1++) { 
//		        runningactivities.add(0,services.get(i1).topActivity.toString());  
//		    } 
//
//		    if(runningactivities.contains("ComponentInfo{com.mslab/com.mslab.encryptsms.ShowConversationActivity}")==true){
//		        Toast.makeText(getBaseContext(),"Activity is in foreground, active",1000).show(); 
//		        return true;
//		    }
//		    
//		return false;
//	}
//	
//	private void displaySMS(){
//		checkWheterActivityIsInForeground();
//	}

	/**
	 * Gets the stores password.
	 * @return The stored password.
	 * @deprecated
	 */
	public byte[] getPassword() {
		return mPassword;
	}
	
	@Override
	public void onDestroy() {
		
		// Make sure you unregister your receivers when you pause your activity
		if (mReceiversRegistered) {
			unregisterReceiver(mInServiceReceiver);
			mReceiversRegistered = false;
		}
		super.onDestroy();
		System.out.println("onDestroy");
	}
	
	/**
	 * This method reactivates the auto logoff timer. The given context must be the context of the activity which sends this message.
	 * @param context - the context of the origin activity
	 */
	public static void alive(Context context){
		Intent intent = new Intent(context, SessionManagerService.class);
		intent.putExtra(Constants.USER_ALIVE_MESSAGE, true);
		context.startService(intent);
	}
	
	/**
	 * Removes the alarm from Android alarmmanager.
	 */
	private void stopAutoLogoff(){
		//remove all old alarms from alarmmanager
		Intent alarmIntent = new Intent(SessionManagerService.this, SessionManagerService.class);
//        alarmIntent.putExtra(Constants.STOP_SERVICE_BY_ALARM, true);
        PendingIntent pi = PendingIntent.getService(SessionManagerService.this, AUTO_STOP_REQUEST_CODE, alarmIntent, 0);
        AlarmManager alarm_manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarm_manager.cancel(pi);
	}
	
	private void resendSMS(long ConversationID) {
		// save the sms into database
		ConversationsDataSource datasource = new ConversationsDataSource(
				context);
		datasource.open(mPassword);
		Conversation conversation = datasource.getConversation(ConversationID);
		Contact contact = datasource.getContact(conversation);
		datasource.close();
		
		SmsManager sms = SmsManager.getDefault();
		
		byte[] ciphertext = WriteSMSActivity.encryptMessage(contact, conversation.message);
		ciphertext = SMSUtils.appendZeroEncoding(ciphertext);
		
		ArrayList<String> parts = sms.divideMessage(SMSUtils
				.convertToGSM(new String(ciphertext)));
		ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliveredPIs = new ArrayList<PendingIntent>();
		
		Bundle bundle = new Bundle();
		bundle.putLong(Conversation.CONVERSATION_ID, conversation.id);
		
		for (int i = 0; i < parts.size(); ++i) {
			bundle.putInt("part", i);
			
			Intent sentIntent = new Intent(SMSSentReceiver.SMS_SENT);
			sentIntent.putExtras(bundle);
			Intent deliveredIntent = new Intent(
					SMSDeliveredReceiver.SMS_DELIVERED);
			deliveredIntent.putExtras(bundle);
			
			sentPIs.add(PendingIntent.getBroadcast(this, 0, sentIntent,
					PendingIntent.FLAG_ONE_SHOT));
			deliveredPIs.add(PendingIntent.getBroadcast(this, 1,
					deliveredIntent, PendingIntent.FLAG_ONE_SHOT));
		}
		
		sms.sendMultipartTextMessage(contact.phonenumber, null, parts, sentPIs,
				deliveredPIs);
	}
}