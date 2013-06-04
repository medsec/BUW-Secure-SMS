package com.mslab.encryptsms.misc;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;

import org.bouncycastle.crypto.digests.Skein;

/**
 * Helper class for a Diffie-Hellman keyexchange.
 * @author Paul Kramer
 *
 */
public class DHKeyGen {
	
	/**
	 * Constant for the field of the prime P.
	 */
	public static String P = "P";
	/**
	 * Constant for the field of the generator G.
	 */
	public static String G = "G";
	/**
	 * Constant identifier for a following public key.
	 */
	public static String PUBLIC_KEY = "PK";
	
	/**
	 * Generates a new keypair with given bitlength. We recommend a bitlength of at least 2048 Bits.
	 * @param bitLength The bitlength of the random numbers of the keypair.
	 * @return A new keypair.
	 */
	public DHKeyPair getKeyPair(int bitLength) {
		
		SecureRandom srnd = new SecureRandom();
		final BigInteger p = BigInteger.probablePrime(bitLength, srnd);
		final BigInteger g = BigInteger.probablePrime(bitLength, srnd);
		
		DHKeyPair aPair = new DHKeyPair();
		aPair.p = p;
		aPair.g = g;
		DHParameterSpec dhParams = new DHParameterSpec(p, g);
		try {
			KeyPairGenerator keyGen = KeyPairGenerator
					.getInstance("DH", "BC");
			keyGen.initialize(dhParams, new SecureRandom());
			aPair.keypair = keyGen.generateKeyPair();
		} catch (Exception e) {
			
		}
		
		return aPair;
	}
	
	/**
	 * Generates a new keypair from given prime and generator.
	 * @param p The prime.
	 * @param g The generator.
	 * @return A new Diffie-Hellman keypair.
	 */
	public DHKeyPair getKeyPair(BigInteger p, BigInteger g) {
		
		DHKeyPair aPair = new DHKeyPair();
		aPair.p = p;
		aPair.g = g;
		DHParameterSpec dhParams = new DHParameterSpec(p, g);
		try {
			KeyPairGenerator keyGen = KeyPairGenerator
					.getInstance("DH", "BC");
			keyGen.initialize(dhParams, new SecureRandom());
			aPair.keypair = keyGen.generateKeyPair();
		} catch (Exception e) {
			
		}
		
		return aPair;
	}

	/**
	 * Determines the shared secret from the given input. 
	 * @param ownKeyPair The own keypair.
	 * @param foreignKey The foreign public key.
	 * @return The shared secret, what is a 256 Bit long Skein hash.
	 */
	public byte[] getSecret(KeyPair ownKeyPair, PublicKey foreignKey){
		KeyAgreement aKeyAgree = null;
    	Skein hash = null;
    	try {
			aKeyAgree = KeyAgreement.getInstance("DH");
			aKeyAgree.init(ownKeyPair.getPrivate());
	    	aKeyAgree.doPhase(foreignKey, true);
	    	hash = new Skein(256, 256);
	    	byte[] secret = aKeyAgree.generateSecret();
	    	hash.update(secret, 0, secret.length);
		} catch (Exception e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			return null;
		}
    	return hash.doFinal();
	}
	
	/**
	 * Helper class to represent a Diffie-Hellman keypair.
	 * @author Paul Kramer
	 *
	 */
	public class DHKeyPair{
		public KeyPair keypair;
		public BigInteger p;
		public BigInteger g;
		
		/**
		 * Constructor, all fields will be set with the given values.
		 * @param keypair The Keypair.
		 * @param p The prime.
		 * @param g The generator.
		 */
		public DHKeyPair (KeyPair keypair, BigInteger p, BigInteger g){
			this.keypair = keypair;
			this.p = p;
			this.g = g;
		}
		
		/**
		 * Default constructor, all fields will be set to null.
		 */
		public DHKeyPair (){
			this.keypair = null;
			this.p = null;
			this.g = null;
		}
	}
	
}
