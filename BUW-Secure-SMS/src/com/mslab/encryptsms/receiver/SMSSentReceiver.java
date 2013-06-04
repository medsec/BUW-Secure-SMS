package com.mslab.encryptsms.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.mslab.encryptsms.misc.Constants;
import com.mslab.encryptsms.misc.Conversation;
import com.mslab.encryptsms.misc.SMSSentDataSource;
import com.mslab.encryptsms.services.SessionManagerService;

/**
 * This receiver will be noticed about sms sent information, like errors, radio problems or sms sent message.
 * @author Paul Kramer
 *
 */
public class SMSSentReceiver extends BroadcastReceiver {
	
	/**
	 * Intent filter string to filter for SMS sent messages from this application.
	 */
	public static final String SMS_SENT = "com.mslab.encryptsms.SMS_SENT";
	
	@Override
    public void onReceive(Context context, Intent intent) {
		
		//open database
		SMSSentDataSource datasource = new SMSSentDataSource(context);
		datasource.open();
		
		//get bundle
		Bundle bundle = intent.getExtras();
		long conversationID = bundle.getLong(Conversation.CONVERSATION_ID, -1l);
		
        switch (getResultCode())
        {
            case Activity.RESULT_OK:
                Toast.makeText(context, "SMS sent", 
                        Toast.LENGTH_SHORT).show();
                datasource.createSentSMS(conversationID, Conversation.SMS_STATUS_SENT);
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                Toast.makeText(context, "Generic failure", 
                        Toast.LENGTH_SHORT).show();
                datasource.createSentSMS(conversationID, Conversation.SMS_STATUS_ERROR);
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                Toast.makeText(context, "No service", 
                        Toast.LENGTH_SHORT).show();
                datasource.createSentSMS(conversationID, Conversation.SMS_STATUS_ERROR);
                Intent serviceIntent = new Intent(context, SessionManagerService.class);
                serviceIntent.putExtra(Constants.RESEND_SMS, true);
                serviceIntent.putExtra(Conversation.CONVERSATION_ID, conversationID);
                context.startService(serviceIntent);
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                Toast.makeText(context, "Null PDU", 
                        Toast.LENGTH_SHORT).show();
                datasource.createSentSMS(conversationID, Conversation.SMS_STATUS_ERROR);
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                Toast.makeText(context, "Radio off", 
                        Toast.LENGTH_SHORT).show();
                datasource.createSentSMS(conversationID, Conversation.SMS_STATUS_ERROR);
                break;
        }
        
        datasource.close();
    }
}
