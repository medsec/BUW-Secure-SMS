package com.mslab.encryptsms.misc;

import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class is a object-representation of the used conversations. It supports the usage of the 
 * databases by generalising the interface.
 * @author Paul Kramer
 *
 */
public class Conversation implements Comparable<Conversation>, Parcelable{
	public static final String CONVERSATION_ID = "cid";
	
	public Calendar date;
	public String message;
	public long id;
	/**
	 * sent: 0 for received message, 1 for sent message
	 */
	public boolean sent;
	public long contactid;
	/**
	 * status: 	0 ... outbox
	 * 			1 ... sent
	 * 			2 ... delivered
	 * 			3 ... not delivered
	 * 			4 ... error
	 */
	public int status;
	
	public static final int SMS_STATUS_OUTBOX = 0;
	public static final int SMS_STATUS_SENT = 1;
	public static final int SMS_STATUS_DELIVERED = 2;
	public static final int SMS_STATUS_NOT_DELIVERED = 3;
	public static final int SMS_STATUS_ERROR = 4;
	
	/**
	 * Special constructor for this conversation container.
	 * @param date
	 * @param message
	 */
	public Conversation(long date, String message) {
		this.date = Calendar.getInstance();
		this.date.setTimeInMillis(date);
		if(message != null) this.message = new String(message);
		this.sent = false;
		this.contactid = -1l;
		this.status = SMS_STATUS_OUTBOX;
	}
	
	/**
	 * Constructor t create a conversation object from a parcel container.
	 * @param source
	 */
	public Conversation(Parcel source){
		this.date = Calendar.getInstance();
		this.date.setTimeInMillis(source.readLong());
		this.message = source.readString();
		this.id = source.readLong();
		boolean sents[] = new boolean[1];
		source.readBooleanArray(sents);
		sent = sents[0];
		this.contactid = source.readLong();
		this.status = source.readInt();
	}

	@Override
	public int compareTo(Conversation another) {
		return (int)(another.date.getTimeInMillis() - this.date.getTimeInMillis());
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(date.getTimeInMillis());
		dest.writeString(message);
		dest.writeLong(id);
		dest.writeBooleanArray(new boolean[]{sent});
		dest.writeLong(contactid);
		dest.writeInt(status);
	}
	
	/**
	 * Necessary parcel class to create a parcel from the conversation.
	 * @author Paul Kramer
	 *
	 */
	public class ConversationCreator implements Parcelable.Creator<Conversation> {
	      public Conversation createFromParcel(Parcel source) {
	            return new Conversation(source);
	      }
	      public Conversation[] newArray(int size) {
	            return new Conversation[size];
	      }
	}
	
}
