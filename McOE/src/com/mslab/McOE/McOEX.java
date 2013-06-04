package com.mslab.McOE;

import java.util.Arrays;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Represents a implementation of the McOE-X scheme.
 * @author Paul Kramer
 *
 */
public class McOEX extends McOE{

	@Override
	public byte[] encryptAuthenticate(byte[] Header, byte[] Message,
			int Blocksize, BlockCipher Cipher, final byte[] Key) {
		int BlocksizeInByte = Blocksize / Byte.SIZE;
		
		//proof the keylength
		if(Key.length != BlocksizeInByte)
			throw new IllegalArgumentException();
				
		//init cipher parameters
		KeyParameter keyparam;
		
		//modify length of input parameters
		int oldLength = Header.length;
		BlockCipherPadding padding = new ZeroBytePadding();
		
		Header = ensureBlockSize(Header, BlocksizeInByte);
		padding.addPadding(Header, oldLength);
		
		final int oldLengthOfMessage = Message.length;
		Message = ensureBlockSize(Message, BlocksizeInByte);
		final int LastBlockOfMessageOffset = Message.length - BlocksizeInByte;
		padding.addPadding(Message, oldLengthOfMessage);
		
		//init blockcipher
		BufferedBlockCipher blockCipher = new BufferedBlockCipher(Cipher);
		
		//1.
		byte[] U = new byte[BlocksizeInByte];
		byte[] Tau = new byte[BlocksizeInByte];
		byte[] MStar = new byte[BlocksizeInByte];
		byte[] BlockOfOnes = new byte[BlocksizeInByte];
		Arrays.fill(BlockOfOnes, (byte) 255 );
		byte[] Temp = new byte[BlocksizeInByte];
		byte[] CStar = new byte[BlocksizeInByte];
		byte[] CStarStar = new byte[BlocksizeInByte];
		byte[] Tag = new byte[BlocksizeInByte];
		byte[] tweak = new byte[BlocksizeInByte];
		
		byte[] Ciphertext = new byte[Message.length + BlocksizeInByte]; //+1 for additional tag
		
		int number_of_blocks = getNumberOfBlocks(Header, Blocksize);
		
		//2.
		for(int i = 0; i < (number_of_blocks-1); ++i){ //loop over blocks
			
			tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
			keyparam = new KeyParameter(tweak);
			blockCipher.init(true, keyparam);
			blockCipher.processBytes(Header, i * BlocksizeInByte, BlocksizeInByte, U, 0);
			U = XOR(U, 0, Header, i * BlocksizeInByte, BlocksizeInByte);
		}
		
		//3.
		tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(true, keyparam);
		blockCipher.processBytes(Header, (number_of_blocks-1)*BlocksizeInByte, BlocksizeInByte, Tau, 0);
		
		//4.
		U = XOR(Tau, 0, Header, (number_of_blocks-1)*BlocksizeInByte, BlocksizeInByte);
		
		//5.
		number_of_blocks = getNumberOfBlocks(Message, Blocksize);
		for(int i = 0; i < (number_of_blocks - 1); ++i){
			
			tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
			keyparam = new KeyParameter(tweak);
			blockCipher.init(true, keyparam);
			blockCipher.processBytes(Message, i * BlocksizeInByte, BlocksizeInByte, Ciphertext, i * BlocksizeInByte);
			U = XOR(Message, i * BlocksizeInByte, Ciphertext, i * BlocksizeInByte, BlocksizeInByte);
			
		}

		//6.
		System.arraycopy(Message, LastBlockOfMessageOffset, MStar, 0, oldLengthOfMessage - LastBlockOfMessageOffset);
		System.arraycopy(Tau, 0, MStar, oldLengthOfMessage - LastBlockOfMessageOffset, BlocksizeInByte - (oldLengthOfMessage - LastBlockOfMessageOffset));
		
		//7.
		tweak = XOR(BlockOfOnes, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(true, keyparam);
		blockCipher.processBytes(longToByteArray((oldLengthOfMessage - LastBlockOfMessageOffset), BlocksizeInByte), 0, BlocksizeInByte, Temp, 0);
		
		MStar = XOR(Temp, 0, MStar, 0, BlocksizeInByte);
		
		//8.
		tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(true, keyparam);
		blockCipher.processBytes(MStar, 0, BlocksizeInByte, CStar, 0);
		
		//9.
		System.arraycopy(CStar, 0, Ciphertext, LastBlockOfMessageOffset, oldLengthOfMessage - LastBlockOfMessageOffset);
		System.arraycopy(CStar, oldLengthOfMessage - LastBlockOfMessageOffset, Tag, 0, BlocksizeInByte - (oldLengthOfMessage - LastBlockOfMessageOffset));
		
		//10.
		U = XOR(MStar, 0, CStar, 0, BlocksizeInByte);
		
		//11.
		tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(true, keyparam);
		blockCipher.processBytes(Tau, 0, BlocksizeInByte, CStarStar, 0);
		
		//12.
		System.arraycopy(CStarStar, 0, Tag, BlocksizeInByte - (oldLengthOfMessage - LastBlockOfMessageOffset), (oldLengthOfMessage - LastBlockOfMessageOffset));
		
		//13.
		Ciphertext = resizeArrayToSize(Ciphertext, oldLengthOfMessage + BlocksizeInByte); //resize the array to the message size plus one block
		System.arraycopy(Tag, 0, Ciphertext, oldLengthOfMessage, BlocksizeInByte);
		
		return Ciphertext;
	}

	/**
	 * Decrypts a Message using McOE-X.
	 * @param Header the Header
	 * @param Ciphertext the Ciphertext
	 * @param Tag the Tag, it must be exactly from half of the length of Blocksize
	 * @param Blocksize the Blocksize in Bits
	 * @param Cipher the used BlockCipher e.g. AES
	 * @param Key the key as byte array
	 * @return the plaintext or null if the tag was not valid
	 */
	@Override
	public byte[] decryptAuthenticate(byte[] Header, byte[] Ciphertext,
			byte[] Tag, int Blocksize, BlockCipher Cipher, byte[] Key) {
		int BlocksizeInByte = Blocksize / 8;
		//proof the keylength
		if(Key.length != BlocksizeInByte) throw new IllegalArgumentException();
				
		//init cipher parameters
		BufferedBlockCipher blockCipher = new BufferedBlockCipher(Cipher);
		KeyParameter keyparam;
		
		//modify length of input parameters
		int oldLength = Header.length;
		BlockCipherPadding padding = new ZeroBytePadding();
		
		Header = ensureBlockSize(Header, BlocksizeInByte);
		padding.addPadding(Header, oldLength);
		
		final int oldCiphertextLength = Ciphertext.length;
		Ciphertext = ensureBlockSize(Ciphertext, BlocksizeInByte);
		final int LastBlockPositionInCiphertext = Ciphertext.length - BlocksizeInByte;
		final int lstar = oldCiphertextLength - LastBlockPositionInCiphertext;
		
		byte[] Message = new byte[Ciphertext.length];
		
		//1.
		byte[] U = new byte[BlocksizeInByte];
		byte[] Tau = new byte[BlocksizeInByte];
		byte[] NewTau = new byte[BlocksizeInByte];
		byte[] NewTag = new byte[BlocksizeInByte];
		byte[] MStar = new byte[BlocksizeInByte];
		byte[] BlockOfOnes = new byte[BlocksizeInByte];
		Arrays.fill(BlockOfOnes, (byte) 255 );
		byte[] Temp = new byte[BlocksizeInByte];
		byte[] CStar = new byte[BlocksizeInByte];
		byte[] tweak = new byte[BlocksizeInByte];
		
		int number_of_blocks = getNumberOfBlocks(Header, Blocksize);
		
		//2.
		for(int i = 0; i < (number_of_blocks-1); ++i){ //loop over blocks
			
			tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
			keyparam = new KeyParameter(tweak);
			blockCipher.init(true, keyparam);
			blockCipher.processBytes(Header, i * BlocksizeInByte, BlocksizeInByte, U, 0);
			U = XOR(U, 0, Header, i * BlocksizeInByte, BlocksizeInByte);
		}
		
		//3.
		tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(true, keyparam);
		blockCipher.processBytes(Header, (number_of_blocks-1)*BlocksizeInByte, BlocksizeInByte, Tau, 0);
		
		//4.
		U = XOR(Tau, 0, Header, (number_of_blocks-1)*BlocksizeInByte, BlocksizeInByte);
		
		number_of_blocks = getNumberOfBlocks(Ciphertext, Blocksize);
		
		//5.
		for(int i = 0; i < (number_of_blocks - 1); ++i){
			
			tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
			keyparam = new KeyParameter(tweak);
			blockCipher.init(false, keyparam);
			blockCipher.processBytes(Ciphertext, i * BlocksizeInByte, BlocksizeInByte, Message, i * BlocksizeInByte);
			U = XOR(Message, i * BlocksizeInByte, Ciphertext, i * BlocksizeInByte, BlocksizeInByte);
			
		}
		
		//6.
		System.arraycopy(Ciphertext, LastBlockPositionInCiphertext, CStar, 0, oldCiphertextLength - LastBlockPositionInCiphertext);
		System.arraycopy(Tag, 0, CStar, oldCiphertextLength - LastBlockPositionInCiphertext, BlocksizeInByte - (oldCiphertextLength - LastBlockPositionInCiphertext));
		
		//7.
		tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(false, keyparam);
		blockCipher.processBytes(CStar, 0, BlocksizeInByte, MStar, 0);
		
		//8.
		U = XOR(MStar, 0, CStar, 0, BlocksizeInByte);
		
		//9.
		tweak = XOR(BlockOfOnes, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(true, keyparam);
		blockCipher.processBytes(longToByteArray(oldCiphertextLength - LastBlockPositionInCiphertext, BlocksizeInByte), 0, BlocksizeInByte, Temp, 0);
		MStar = XOR(MStar, 0, Temp, 0, BlocksizeInByte);
		
		//10.
		System.arraycopy(MStar, 0, Message, LastBlockPositionInCiphertext, oldCiphertextLength - LastBlockPositionInCiphertext);
		System.arraycopy(MStar, oldCiphertextLength - LastBlockPositionInCiphertext, NewTau, 0, BlocksizeInByte - (oldCiphertextLength - LastBlockPositionInCiphertext));
		
		//11.
		tweak = XOR(U, 0, Key, 0, BlocksizeInByte);
		keyparam = new KeyParameter(tweak);
		blockCipher.init(true, keyparam);
		blockCipher.processBytes(Tau, 0, BlocksizeInByte, NewTag, 0);
		
		//12.
		if(equalsBlock(NewTau, 0, Tau, 0, BlocksizeInByte - (oldCiphertextLength - LastBlockPositionInCiphertext)) //&&
				//equalsBlock(NewTag, 0, Tag, BlocksizeInByte - lstar, lstar)
				){
			return Message;			
		}
		
		return null;
	}
	
}
