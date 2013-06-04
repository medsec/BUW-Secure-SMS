package com.mslab.McOE;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersForThreefish;

/**
 * A implementation of the generic McOE scheme.
 * @author Paul Kramer
 *
 */
public class GenericMcOE extends McOE{

	@Override
	public byte[] encryptAuthenticate(byte[] Header, byte[] Message, int Blocksize, BlockCipher Cipher, byte[] Key) {

		int BlocksizeInByte = Blocksize / 8;
		
		//proof the keylength
		if(Key.length != BlocksizeInByte) throw new IllegalArgumentException();
		
		//init cipher parameters
		KeyParameter keyparam = new KeyParameter(Key);
		long[] tweak;
		
		
		//modify length of input parameters
		int oldLength = Header.length;
		BlockCipherPadding padding = new ZeroBytePadding();
		
		Header = ensureBlockSize(Header, BlocksizeInByte);
		padding.addPadding(Header, oldLength);
		
		oldLength = Message.length;
		Message = ensureBlockSize(Message, BlocksizeInByte);
		padding.addPadding(Message, oldLength);
		
		byte[] U = new byte[BlocksizeInByte];
		byte[] Tau = new byte[BlocksizeInByte];
		
		byte[] Ciphertext = new byte[Message.length + BlocksizeInByte]; //+1 for additional tag
		
		
		CipherParameters paramsCipherParameters;
		
		int number_of_blocks = getNumberOfBlocks(Header, Blocksize);
		
		for(int i = 0; i < (number_of_blocks-1); ++i){ //loop over blocks
			
			tweak = byteToLongArray(U);
			paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
			Cipher.init(true, paramsCipherParameters);
			Cipher.processBlock(Header, i * BlocksizeInByte, U, 0);
			U = XOR(U, 0, Header, i * BlocksizeInByte, BlocksizeInByte);
			
		}
		
		tweak = byteToLongArray(U);
		paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
		Cipher.init(true, paramsCipherParameters);
		Cipher.processBlock(Header, (number_of_blocks-1)*BlocksizeInByte, Tau, 0);
		
		U = XOR(Tau, 0, Header, ((Header.length / BlocksizeInByte)-1) * BlocksizeInByte, BlocksizeInByte);
		
		number_of_blocks = getNumberOfBlocks(Message, Blocksize);
		
		for(int i = 0; i < number_of_blocks; ++i){
			
			tweak = byteToLongArray(U);
			paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
			Cipher.init(true, paramsCipherParameters);
			Cipher.processBlock(Message, i * BlocksizeInByte, Ciphertext, i * BlocksizeInByte);
			U = XOR(Message, i * BlocksizeInByte, Ciphertext, i * BlocksizeInByte, BlocksizeInByte);
			
		}
		
		tweak = byteToLongArray(U);
		paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
		Cipher.init(true, paramsCipherParameters);
		Cipher.processBlock(Tau, 0, Ciphertext, number_of_blocks * BlocksizeInByte);
		
		
		return Ciphertext;
	}

	@Override
	public byte[] decryptAuthenticate(byte[] Header, byte[] Ciphertext,
			byte[] Tag, int Blocksize, BlockCipher Cipher, byte[] Key) {
		
		int BlocksizeInByte = Blocksize / 8;
		
		//proof the keylength
		if(Key.length != BlocksizeInByte) throw new IllegalArgumentException();
		
		//init cipher parameters
		KeyParameter keyparam = new KeyParameter(Key);
		long[] tweak;
		
		//modify length of input parameters
		int oldLength = Header.length;
		BlockCipherPadding padding = new ZeroBytePadding();
		
		Header = ensureBlockSize(Header, BlocksizeInByte);
		padding.addPadding(Header, oldLength);

		int number_of_blocks = getNumberOfBlocks(Header, Blocksize);

		byte[] U = new byte[Blocksize / Byte.SIZE];
		byte[] Tau = new byte[Blocksize / Byte.SIZE];
		
		byte[] newTag = new byte[Blocksize / Byte.SIZE];
		
		byte[] Message = new byte[Ciphertext.length];
		
		CipherParameters paramsCipherParameters;
		
		for(int i = 0; i < (number_of_blocks-1); ++i){ //loop over blocks
			
			tweak = byteToLongArray(U);
			paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
			Cipher.init(true, paramsCipherParameters);
			Cipher.processBlock(Header, i * BlocksizeInByte, U, 0);
			U = XOR(U, 0, Header, i * BlocksizeInByte, BlocksizeInByte);
			
		}
		
		tweak = byteToLongArray(U);
		paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
		Cipher.init(true, paramsCipherParameters);
		Cipher.processBlock(Header, (number_of_blocks-1)*BlocksizeInByte, Tau, 0);
		
		U = XOR(Tau, 0, Header, ((Header.length / BlocksizeInByte)-1) * BlocksizeInByte, BlocksizeInByte);
		
		number_of_blocks = getNumberOfBlocks(Ciphertext, Blocksize);
		
		for(int i = 0; i < number_of_blocks; ++i){
			
			tweak = byteToLongArray(U);
			paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
			Cipher.init(false, paramsCipherParameters);
			Cipher.processBlock(Ciphertext, i * BlocksizeInByte, Message, i * BlocksizeInByte);
			U = XOR(Message, i * BlocksizeInByte, Ciphertext, i * BlocksizeInByte, BlocksizeInByte);
			
		}
		
		tweak = byteToLongArray(U);
		paramsCipherParameters = new ParametersForThreefish(keyparam, ParametersForThreefish.Threefish256, tweak);
		Cipher.init(true, paramsCipherParameters);
		Cipher.processBlock(Tau, 0, newTag, 0);
		
		//everything was ok
		if(equalsBlock(Tag, 0, newTag, 0, BlocksizeInByte)){
			return Message;
		}
		
		//problem occured
		return null;
	}

}
