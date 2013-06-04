package com.mslab.encryptsms.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.mslab.encryptsms.misc.Conversation;
import com.mslab.encryptsms.misc.SMSSentDataSource;

/**
 * This receiver will be noticed, if a sms delivered intent was received. It sends a message to the sms state database controller to update the state.
 * @author Paul Kramer
 *
 */
public class SMSDeliveredReceiver extends BroadcastReceiver {
	
	public static final String SMS_DELIVERED = "com.mslab.encryptsms.SMS_DELIVERED";
	
	@Override
    public void onReceive(Context context, Intent intent) {
		//open database
		SMSSentDataSource datasource = new SMSSentDataSource(context);
		datasource.open();
		
		//handle intent
        switch (getResultCode())
        {
            case Activity.RESULT_OK:
                Toast.makeText(context, "SMS delivered", 
                        Toast.LENGTH_SHORT).show();
                datasource.updateSentStatus(intent.getLongExtra(Conversation.CONVERSATION_ID, -1l), Conversation.SMS_STATUS_DELIVERED);
                break;
            case Activity.RESULT_CANCELED:
                Toast.makeText(context, "SMS not delivered", 
                        Toast.LENGTH_SHORT).show();
                datasource.updateSentStatus(intent.getLongExtra(Conversation.CONVERSATION_ID, -1l), Conversation.SMS_STATUS_NOT_DELIVERED);
                break;                        
        }
        //close database
        datasource.close();
    }
}
