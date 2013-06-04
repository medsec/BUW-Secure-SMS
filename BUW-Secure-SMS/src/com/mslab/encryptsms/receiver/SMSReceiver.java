package com.mslab.encryptsms.receiver;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.PhonenumberDataSource;
import com.mslab.encryptsms.misc.ReceivedSMS;
import com.mslab.encryptsms.misc.SMSDataStorage;
import com.mslab.encryptsms.services.SessionManagerService;
import com.mslab.smsutils.SMSUtils;


/**
 * This receiver receives all incoming messages an extracts all messages which are encrypted.
 * @author Paul Kramer
 *
 */
public class SMSReceiver extends BroadcastReceiver {
	
	public static String NEW_SMS = "new_sms";
	private final int REC_SMS_NOTIFICATION = 122;
	
	private volatile String password;
	
	private SessionManagerService mService;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		// ---get the SMS message passed in---
		Bundle bundle = intent.getExtras();
		SmsMessage[] msgs = null;
		String str = "";
		if (bundle != null) {
			//test if it is not an SMS but only the password
			if(bundle.containsKey(Constants.DB_PASSWORD_HASH)){
				password = bundle.getString(Constants.DB_PASSWORD_HASH);
			}else{
				
				// ---retrieve the SMS message received and save it into
				// database---
				SMSDataStorage datastorage = new SMSDataStorage(context);
				datastorage.open();
				
				PhonenumberDataSource phonenumbers = new PhonenumberDataSource(
						context);
				phonenumbers.open();
				
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];
				String knownContact = null;
				for (int i = 0; i < msgs.length; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					str += "SMS from " + msgs[i].getOriginatingAddress();
					str += " :";
					str += msgs[i].getMessageBody().toString();
					str += "\n";
				}
				
				SmsMessage sms = msgs[0];
				String sender = msgs[0].getOriginatingAddress();
				String body = "";
				try {
				  if (msgs.length == 1 || sms.isReplace()) {
				    body = sms.getDisplayMessageBody();
				  } else {
				    StringBuilder bodyText = new StringBuilder();
				    for (int i = 0; i < msgs.length; i++) {
				      bodyText.append(msgs[i].getMessageBody());
				    }
				    body = bodyText.toString();
				  }
				} catch (Exception e) {

				}
				//decode Base123
				byte zeroByte = (byte)(body.charAt(0));
				byte[] decode = SMSUtils.decodeBase123(body.substring(1, body.length()));
				byte[] temp = new byte[decode.length + 1];
				System.arraycopy(decode, 0, temp, 1, decode.length);
				temp[0] = zeroByte;
				
				ReceivedSMS rsms = new ReceivedSMS(
						msgs[0].getTimestampMillis(), 
						temp,
						sender);
				//append leading zeros
				if(temp[0] != ReceivedSMS.KEY_EXCHANGE_BYTE) rsms.repairSMSLength();
				
				if (sender != null) {
					knownContact = phonenumbers.containsPhoneNumber(sender);
					// check if it is an sms from a known contact
					// check if the length is a multiple of 128 Bit block size
					//check if it is probably encrypted
					if (knownContact != null &&
							rsms.isProbablyEncryptedSMS()) { 
						datastorage.createReceivedSMS(rsms);
					}else{
						//the sms is potentially a key exchange sms
						if(rsms.isProbablyKeyExchangeSMS()){
							datastorage.createReceivedSMS(rsms);
							Intent serviceIntent = new Intent(context, SessionManagerService.class);
							serviceIntent.putExtra(Constants.SMS_KEY_EXCHANGE, true);
							serviceIntent.putExtra(Constants.SMS_KEY, rsms);
							context.startService(serviceIntent);
						}
					}
				}
					
				
				datastorage.close();
				phonenumbers.close();
				
				if (knownContact != null && rsms.isProbablyEncryptedSMS()) { // SMS come from an known user -
											// ignore it
					abortBroadcast();
					displayNotification(context);
				}else if(rsms.isProbablyKeyExchangeSMS()){
					abortBroadcast();
				}
				
			}
		}
	}
	
	private boolean isMyServiceRunning(Context context) {
	    ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	    	System.out.println(service.service.getClassName());
	        if (SessionManagerService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	private void displayNotification(Context context){
		
		Intent intent = new Intent(context, SessionManagerService.class);
		intent.putExtra(Constants.SMS_RECEIVED, true);
		context.startService(intent);
		
	}
	
}
