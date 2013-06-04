package com.mslab.encryptsms.misc;

/**
 * This class represents sent messages, that are not completely received by the recipient.
 * @author Paul Kramer
 *
 */
public class SentSMS {
	
	public long ID;
	public int State;
	
	/**
	 * Constructor.
	 * @param ID The ID of the message in enciphered database.
	 * @param State The state of the message.
	 */
	public SentSMS (long ID, int State){
		this.ID = ID;
		this.State = State;
	}
	
}
