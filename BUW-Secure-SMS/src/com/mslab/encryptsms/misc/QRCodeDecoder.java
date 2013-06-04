package com.mslab.encryptsms.misc;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.util.encoders.Base64;

import com.mslab.encryptsms.QRScanActivity;

/**
 * This class decodes the QR-codes used for key and contact details exchange.
 * @author Paul Kramer
 *
 */
public class QRCodeDecoder {
	
	/**
	 * Decodes the QR-code and returns a QR-code object containing all data.
	 * @param rawdata The raw QR-code data.
	 * @return The QR-code object.
	 */
	//Format: MECARD:N:Your Name;TEL:0123456;EMAIL:yourname@googlemail.com;SKEY:asdsfnccsdunch;;
	public static QRCodeReturn decodeQRCodeContents(String rawdata){
		String[] elements = rawdata.split(";");
		
		QRCodeReturn returnValue = new QRCodeReturn();
		
		returnValue.contact = new Contact();
				
		for(String element : elements){
			String[] detail = element.split(":");
			if(detail.length > 0){
				if(detail.length == 3 && detail[1].equals("N")){ //Name
					returnValue.contact.name = detail[2];
				}else if(detail[0].equals("TEL")){
					returnValue.contact.phonenumber = detail[1];
				}else if(detail[0].equals("EMAIL")){
					returnValue.contact.email = detail[1];
				}else if(detail[0].equals(QRScanActivity.DIRECTION)){
					returnValue.direction = Integer.parseInt(detail[1]);
				}else if(detail[0].equals(DHKeyGen.P)){
					returnValue.p = new BigInteger(removeEscapeSequence(detail[1]));
				}else if(detail[0].equals(DHKeyGen.G)){
					returnValue.g = new BigInteger(removeEscapeSequence(detail[1]));
				}else if(detail[0].equals(DHKeyGen.PUBLIC_KEY)){
					int begin = rawdata.indexOf(DHKeyGen.PUBLIC_KEY+":")+DHKeyGen.PUBLIC_KEY.length()+1;
					int end = rawdata.indexOf(";[A-Z]{1,}:", begin);
					if(end < 0) end = rawdata.length()-2;
					String pk = rawdata.substring(begin, end);
					try {
						X509EncodedKeySpec x509enc = new X509EncodedKeySpec(Base64.decode(pk.getBytes()));
						KeyFactory kf = KeyFactory.getInstance("DH", "BC");
						returnValue.pubkey = kf.generatePublic(x509enc);
					} catch (NumberFormatException nfe) {
						nfe.printStackTrace();
					} catch (NoSuchAlgorithmException nsae){
						nsae.printStackTrace();
					} catch (NoSuchProviderException nspe){
						nspe.printStackTrace();
					} catch (InvalidKeySpecException ikse) {
						ikse.printStackTrace();
					}
				}
			}
		}
	
		return returnValue;
	}
	
	/**
	 * Removes necessary escape sequences from the raw QR-code.
	 * @param input The raw code.
	 * @return Input with removed escape sequences.
	 */
	public static String removeEscapeSequence(String input){
		String result = "";
		
		if(input.contains(":\\") || input.contains(";\\")){
			
			input = input.replace(":\\", ":");
			result = input.replace(";\\", ";");
			
		}else{
			return input;
		}
		
		return result;
	}
	
	private int countEscapeCharacters(String input, int begin, int lengthWithoutEscape, String[] escapesequences){
		int charscount = 0;
		int index = begin;
		for(String escape : escapesequences){
			while(index < input.length() && lengthWithoutEscape > 0){
				int newindex = input.indexOf(escape, index);
				if(newindex < 0) break;
				lengthWithoutEscape -= newindex - index;
				charscount += 2;
				index = newindex + 2;
			}
			index = begin;
		}
		return charscount;
	}
}
