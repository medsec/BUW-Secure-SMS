package com.mslab.encryptsms.misc;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Helper class to store received, encrypted messages.
 * @author Paul Kramer
 *
 */
public class ReceivedSMS implements Parcelable {
	
	public static byte ENCRYPTED_BYTE_7 = (byte)0x3F; //7-Bit encoded encrypted byte in ASCII (?)
	public static byte ENCRYPTED_BYTE_8 = (byte)0x25; //8-Bit encoded encrypted byte in ASCII (%)
	public static byte KEY_EXCHANGE_BYTE = (byte)0x26; //(&)

	public long Date;
	public byte[] Message;
	public String Phone;
	
	/**
	 * Constructor.
	 * @param Date The receive date and time im ms since 1970.
	 * @param Message The enciphered message.
	 * @param Phone The phone number of the contact.
	 */
	public ReceivedSMS(long Date, byte[] Message, String Phone){
		this.Date = Date;
		this.Message = Message;
		this.Phone = Phone;
	}
	
	/**
	 * Constructor to create a received sms from parceled object.
	 * @param source
	 */
	public ReceivedSMS(Parcel source){
		this.Date = source.readLong();
		this.Message = new byte[source.readInt()];
		source.readByteArray(this.Message);
		this.Phone = source.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(Date);
		dest.writeInt(Message.length);
		dest.writeByteArray(this.Message);
		dest.writeString(Phone);
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * Creator, to parcel this object.
	 */
	public static final Parcelable.Creator<ReceivedSMS> CREATOR = new Creator<ReceivedSMS>(){

		@Override
		public ReceivedSMS createFromParcel(Parcel source) {
			return new ReceivedSMS(source);
		}

		@Override
		public ReceivedSMS[] newArray(int size) {
			return new ReceivedSMS[size];
		}
		
	};
	
	/**
	 * This method checks if the message is a probably encrypted message by testing the encrypted byte
	 * @return true if it is probably encrypted otherwise it returns false
	 */
	public boolean isProbablyEncryptedSMS(){
		
		//check length
		//compress length to 7 bit chars minus 9 chars for IV and encrypted byte mod block size
//		if(((Message.length()*7/8) - 9) % 16 != 0 ) return false;
		if((Message.length - 9) % 16 != 0 ) return false;
		
		//check encrypted byte
//		byte[] messagebytes = SMSUtils.compressBytesTo7BitPerCharEncoding(Message.getBytes());
//		byte first_byte = messagebytes[0];
		byte first_byte = Message[0];
		if(first_byte == ENCRYPTED_BYTE_7 || first_byte == ENCRYPTED_BYTE_8){
			return true;
		}
		
		return false;
	}

	/**
	 * This method checks if the incoming sms contains a public key of another contact by testing the first two bytes.
	 * @return true if it is probably a key, otherwise false
	 */
	public boolean isProbablyKeyExchangeSMS(){
		if(Message[0] != KEY_EXCHANGE_BYTE) return false;
		return true;
	}
	
	/**
	 * Because of the Base123 encoding it may happens, that leading zeros will
	 * be removed. The ensure the richt SMS length, this method append as much
	 * zero bytes between detection byte and IV as needed.
	 */
	public void repairSMSLength(){
		int diff = 16 - ((Message.length - 9) % 16);
		if(1 <= diff && diff <= 7){
			byte[] temp = new byte[Message.length + diff];
			temp[0] = Message[0];
			System.arraycopy(Message, 1, temp, 1 + diff, Message.length-1);
			Message = temp;
		}
	}

}
