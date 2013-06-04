package com.mslab.encryptsms.misc;

import java.math.BigInteger;
import java.security.PublicKey;

/**
 * Helper class to store received QR-codes.
 * @author Paul Kramer
 *
 */
public class QRCodeReturn {
	public Contact contact;
	public PublicKey pubkey;
	public BigInteger p;
	public BigInteger g;
	public int direction;
}
