package com.mslab.encryptsms.misc;

import java.math.BigInteger;

import com.mslab.encryptsms.LoginActivity;
import com.mslab.encryptsms.services.SessionManagerService;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

/**
 * This class contains all constants.
 * @author Paul Kramer
 *
 */
public class Constants {
	
	//intent bundle data identifiers
	public static final String CONTACT = "contact";
	public static final String PASSWORD_HASH = "password";
	public static final String DB_PASSWORD_HASH = "dbpassword";
	public static final String PASSWORD_SALT = "passwordsalt";
	public static final String DATABASE_SALT = "dbsalt";
	public static final String SMS_RECEIVED = "smsreceived";
	public static final String SHOW_RECEIVED_SMS = "showreceivedsms";
	public static final String RESEND_SMS = "resendsms";
	public static final String STOP_SERVICE = "stopservice";
	public static final String STOP_SERVICE_PANIC = "stopservicepanic";
	public static final String STOP_SERVICE_BY_ALARM = "stopservicebyalarm";
	public static final String AUTO_LOGOFF = "autologoff";
	public static final String AUTO_LOGOFF_TIME = "autologofftime";
	public static final String USER_ALIVE_MESSAGE = "useralive";
	public static final String SMS_KEY_EXCHANGE = "smskeyexchange";
	public static final String SMS_KEY = "smskey";
	
	//Diffie-Hellman prime and generator
	public static final BigInteger p = new BigInteger("16936889854326521013269465155959058134495847961920798786326042397726632070995522520236378648390795706136647333000386532921755146231934497531435210208224774759385808088646456240937099348639914635486138880226912318381027010231748297784028544281948850794830511772281079907131848015162116076314535109663916327557330792021720938984215346226697887034296382337307614286875593291383820674640783467767672627442280898399961369153213057251035400558330571993930494338773983043428742978553125694559805024955901735377598361948450951512050919381715622087621578900396626506233489509576642618202825196322134428051856833067651072706491");
	public static final BigInteger g = new BigInteger("31157147220753426420630254469500509991143333108004739524029944762370456378612745936921000856537072833877342325074916137061525961454901270683284803268128449607830360466672232425127923086604074501687916134344509538579641584931430531530535994460515063473631287003962370218188338633385495501012892227696126120244724120733826177075509797087629932186412412585500654995663823450522716297353566661408450026533572249793423670726623085359521038907036923137167487533669573297175383453681145110320707099374803469111358248356700143081529544137890887289560059694216676816015704731324896253429057685614879889088418830540913513484199");
	public static final int REQUEST_CHANGE_PW = 121;
	
	//intent request codes
	

	/**
	 * Panic Button Action, this method will be called, if the panic button is triggered.
	 * @param panicButton - the button that triggers the action
	 * @param context - the activity context
	 */
	public static void setPanicAction(Button panicButton, final Context context){
		panicButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// stopp Service
				Intent intent = new Intent(context, SessionManagerService.class);
				intent.putExtra(Constants.STOP_SERVICE_PANIC, true);
				context.startService(intent);
				
				intent = new Intent(context, LoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				context.startActivity(intent);
			}
		});
	}
	
	/**
	 * Concatenates two byte arrays and returns one new large array.
	 * @param left - the left part of the new array
	 * @param right - the right part of the new array
	 * @return the concatenated array
	 */
	public static byte[] concatenateArrays(byte[] left, byte[] right){
		byte[] result = new byte[left.length + right.length];
		System.arraycopy(left, 0, result, 0, left.length);
		System.arraycopy(right, 0, result, left.length, right.length);
		return result;
	}
}
