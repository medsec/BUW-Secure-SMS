package com.mslab.encryptsms.misc;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.mslab.encryptsms.R;

/**
 * This class represents a contact.
 * @author Paul Kramer
 *
 */
public class Contact implements Parcelable, Cloneable{
	public String name;
	public String phonenumber;
	public String email;
	public boolean isSelected;
	public long id;
	public byte[] secret;
	/**
	 * Values are:
	 * 0 ... waiting for exchange
	 * 1 ... exchange done, key not accepted
	 * 2 ... exchange done, key was accepted
	 */
	public int exchange_done;
	//static fields
	public static final int WAITING = 0;
	public static final int NOT_ACCEPTED = 1;
	public static final int ACCEPTED = 2;
	
	/**
	 * Default constructor.
	 */
	public Contact(){
		this.name = "default";
		this.email = "none";
		this.phonenumber = "0000";
		this.isSelected = false;
		this.id = -1l;
		this.secret = new byte[32];
		this.exchange_done = 0;
	}
	
//	public Contact(Contact otherContact){
//		this.name = otherContact.name;
//		this.email = otherContact.email;
//		this.phonenumber = otherContact.phonenumber;
//		this.isSelected = otherContact.isSelected;
//		this.id = otherContact.id;
//		this.secret = new byte[32];
//		System.arraycopy(otherContact.secret, 0, this.secret, 0, 32);
//	}
	
	/**
	 * Special constructor.
	 * @param name The name of the contact.
	 * @param phonenumber the Phonenumber of the contact. The Representation by "+countrycode phonenumber" is suggested.
	 */
	public Contact(String name, String phonenumber){
		this.name = name;
		this.phonenumber = phonenumber;
		this.email = "none";
		this.isSelected = false;
		this.id = -1l;
		this.secret = new byte[32];
		this.exchange_done = 0;
	}
	
	/**
	 * Parcelable constructor method
	 * @param parcel The parcel to construct a contact from.
	 */
	public Contact(Parcel parcel){
		this.name = parcel.readString();
		this.phonenumber = parcel.readString();
		this.email = parcel.readString();
		this.isSelected = (parcel.readInt()==1);
		this.id = parcel.readLong();
		this.secret = new byte[32];
		parcel.readByteArray(this.secret);
		this.exchange_done = parcel.readInt();
		
	}
	
	@Override
	public String toString(){
		return name + "\n" + phonenumber + "\n" + email;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		return (this.id == ((Contact)o).id 
				|| this.name.equalsIgnoreCase(((Contact)o).name));
	}
	
	/**
	 * Copies the contact.
	 */
	@Override
	public Contact clone(){
		try
		{
			return (Contact)super.clone();
		}catch(CloneNotSupportedException cnse){
			cnse.printStackTrace();
		}
		return null;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(phonenumber);
		dest.writeString(email);
		dest.writeInt(isSelected ? 1 : 0);
		dest.writeLong(id);
		dest.writeByteArray(secret);
		dest.writeInt(exchange_done);
	}
	
	/**
	 * Parcel creator to create a parcelable type.
	 */
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

		@Override
		public Contact createFromParcel(Parcel source) {
			return new Contact(source);
		}

		@Override
		public Contact[] newArray(int size) {
			return new Contact[size];
		}
		
	};
	
	/**
	 * Method to generate the secret fingerprint. This method should be replaced by a secure one.
	 * @param context The application context
	 * @return The fingerprint string, which contains the first, 15. and 31. Byte of the secret in hex representation.
	 */
	public String getSecretFingerprint(Context context){
		String print = "";
		
		if(secret.length != 32) return context.getString(R.string.sms_key_exchange_key_error);
		
		print += String.format("%02x", secret[0]&0xff);
		print += String.format("%02x", secret[15]&0xff);
		print += String.format("%02x", secret[31]&0xff);
		
		return print;
	}
	
}
