package com.mslab.encryptsms.misc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.mslab.encryptsms.R;

/**
 * This class helps to show a notification in notification bar.
 * @author Paul Kramer
 *
 */
public class SMSNotifierHelper {
    private static final int NOTIFY_1 = 0x1001;
    /**
     * Adds a notification to the notification bar.
     * @param caller The calling activity.
     * @param serviceToLaunch The service to launch.
     * @param title The shown title of the notification.
     * @param msg The messagte of the notification.
     * @param numberOfEvents The number of events for this notification, currently this is not used.
     * @param flashLed Flag for flashong an led after a notification was set. This does not work on all devices.
     * @param vibrate Flag for vibrating after receiving a message.
     */
    public static void sendNotification(Context caller, Class<?> serviceToLaunch, String title, String msg, int numberOfEvents, boolean flashLed, boolean vibrate) {
        NotificationManager notifier = (NotificationManager) caller.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent toLaunch = new Intent(caller, serviceToLaunch);
//      toLaunch.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_NEW_TASK);
        toLaunch.putExtra(Constants.SHOW_RECEIVED_SMS, true);
        PendingIntent intentBack = PendingIntent.getService(caller, 4711, toLaunch, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				caller)
				.setContentText("New Messages")
				.setContentTitle("Encrypted Message")
				.setSmallIcon(R.drawable.ic_launcher)
				.setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_VIBRATE| Notification.DEFAULT_SOUND)
//				.setLargeIcon(
//						BitmapFactory.decodeResource(caller.getResources(),
//								R.drawable.ic_launcher))
				.setContentIntent(intentBack).setAutoCancel(true);
//		.addAction(R.drawable.ic_launcher, "show", intentBack).build();// new
														// Notification(R.drawable.ic_launcher,
														// "",
														// System.currentTimeMillis());
		final Notification notify = builder.build();

//        notify.when = System.currentTimeMillis();
//        notify.number = numberOfEvents;
//        notify.flags |= Notification.FLAG_AUTO_CANCEL;
		
		 // Hide the notification after its selected
//	    notify.flags |= Notification.FLAG_AUTO_CANCEL;

//        if (flashLed) {
//        // add lights
//            notify.flags |= Notification.FLAG_SHOW_LIGHTS;
//            notify.ledARGB = Color.CYAN;
//            notify.ledOnMS = 500;
//            notify.ledOffMS = 500;
//        }
//
//        if (vibrate) {
//            notify.vibrate = new long[] {100, 200, 200, 200, 200, 200, 1000, 200, 200, 200, 1000, 200};
//        }

        
//        notify.setLatestEventInfo(caller, title, msg, intentBack);
        notifier.notify(NOTIFY_1, notify);
    }
    
    /**
     * Removes the notification programatically from notification bar.
     * @param context the application context.
     */
    public static void cancelNotification(Context context){
    	NotificationManager notifier = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	notifier.cancel(NOTIFY_1);
    }
}
