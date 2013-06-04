package com.mslab.McOE;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.crypto.BlockCipher;

/**
 * 
 * McOE abstract Base Class. It defines the structure of all McOE inherited classes and some static tools. 
 * @author Paul Kramer
 *
 */
public abstract class McOE {
	
	/**
	 * Encrypts a Message using McOE.
	 * @param Header the Header
	 * @param Message the Message
	 * @param Blocksize block ciphers block size in bits
	 * @param Cipher the current cipher
	 * @return the ciphertext
	 */
	public abstract byte[] encryptAuthenticate(byte[] Header, byte[] Message, int Blocksize, BlockCipher Cipher, byte[] Key);
	
	/**
	 * Decrypts a Message using McOE.
	 * @param Header the Header
	 * @param Ciphertext the Ciphertext
	 * @param Tag the Tag
	 * @param Blocksize the Blocksize in Bits
	 * @param Cipher the used BlockCipher e.g. AES
	 * @param Key the key as byte array
	 * @return the plaintext or null if the tag was not valid
	 */
	public abstract byte[] decryptAuthenticate(byte[] Header, byte[] Ciphertext, byte[] Tag, int Blocksize,  BlockCipher Cipher, byte[] Key);
	
	/**
	 * Returns the number of message blocks.
	 * @param Message the message
	 * @param Blocksize the current block size of the cipher in bit
	 * @return the number of blocks
	 */
	public int getNumberOfBlocks(byte[] Message, int Blocksize){
		return Message.length * Byte.SIZE / Blocksize;
	}
	
	/**
	 * XOR - xor's the input arrays bytewise and returns the result, both input arrays should have BytesToHandle bytes till the end 
	 * @param Left input array
	 * @param LeftOffset the beginning offset of the left array
	 * @param Right input array
	 * @param RightOffset the beginning offset of the right array
	 * @param BytesToHandle
	 * @return left xor right
	 */
	public byte[] XOR(byte[] Left, int LeftOffset, byte[] Right, int RightOffset, int BytesToHandle){
		byte[] result = new byte[BytesToHandle];
		
		for(int i = 0; i < BytesToHandle; ++i){
			result[i] = (byte) (Left[i + LeftOffset] ^ Right[i + RightOffset]);
		}
		
		return result;
	}
	
	/**
	 * Converts byte array to long array
	 * @param Input the input as byte array
	 * @return an array of long values containing the given byte array. If the bytearray was shorter 
	 * than a mulitple of longs datatypoe length, the length will be filled to a multiple number of long type.
	 */
	public long[] byteToLongArray(byte[] Input){
		
		int Blocks = (int)Math.ceil(((float)Input.length * 8f / (float)Long.SIZE));
		
		//expand Input to long corresponding length
		if(Input.length < Blocks * Long.SIZE / 8){
			byte[] tempinput = new byte[Blocks * Long.SIZE / 8];
			for(int i = 0; i < Input.length; ++i){
				tempinput[i] = Input[i];
			}
			Input = tempinput;
		}
		
		long[] output = new long[Blocks];
		
		int bitmask = 0xFF;
		
		for(int i = 0; i < Blocks; ++i){
			output[i] |= (((long)Input[8*i] & bitmask) << 56);
			output[i] |= (((long)Input[8*i+1] & bitmask) << 48);
			output[i] |= (((long)Input[8*i+2] & bitmask) << 40);
			output[i] |= (((long)Input[8*i+3] & bitmask) << 32);
			output[i] |= (((long)Input[8*i+4] & bitmask) << 24);
			output[i] |= (((long)Input[8*i+5] & bitmask) << 16);
			output[i] |= (((long)Input[8*i+6] & bitmask) << 8);
			output[i] |= (((long)Input[8*i+7] & bitmask));
		}
		
		return output;
	}
	
	/**
	 * Determines if two blocks are equal
	 * @param Left the left hand input
	 * @param LeftOffset the offset in this input
	 * @param Right the right hand input
	 * @param RightOffset the offset in this input
	 * @param BytesToHandle the number of bytes to check for equality
	 * @return true, if the blocks are equal, false otherwise
	 */
	public boolean equalsBlock(byte[] Left, int LeftOffset, byte[] Right, int RightOffset, int BytesToHandle){

		boolean result = true;
		
		while(BytesToHandle > 0 && result){
			result &= (Left[LeftOffset + BytesToHandle - 1] == Right[RightOffset + BytesToHandle - 1]);
			--BytesToHandle;
		}
		
		return result;
	}
	
	/**
	 * Enlarges the Input to multiple of blocksize
	 * @param Input the input byte array
	 * @param BlocksizeInByte the block size in bytes
	 * @return the new, longer array
	 */
	public byte[] ensureBlockSize(byte[] Input, int BlocksizeInByte){
		int bytesToAppend = (int)((int) Math.ceil(((float)Input.length / (float)BlocksizeInByte)) * (BlocksizeInByte) - Input.length);
		
		if(Input.length == 0){
			bytesToAppend = BlocksizeInByte;
		}
		
		byte[] output = new byte[Input.length + bytesToAppend];
		for(int i = 0; i < Input.length; ++i){
			output[i] = Input[i];
		}
		
		//swap
		return output;
	}
	
	/**
	 * Converts a long input value to a byte array of given length
	 * @param Value the long input value
	 * @param BlocksizeInBytes the block size in bytes
	 * @return a byte array containing the value
	 */
	public byte[] longToByteArray(long Value, int BlocksizeInBytes){
		byte[] result = new byte[BlocksizeInBytes];
		
        System.arraycopy(ByteBuffer.allocate(8).putLong(Value).array(), 0, result, result.length - 8, 8);
		
		return result;
	}
	
	/**
	 * Resizes the input array to the given size, without any respect of the content.
	 * @param Input the input byte array
	 * @param Size the number of bytes the new array should hold
	 * @return the resized output array containing the bytes of the input array
	 */
	public byte[] resizeArrayToSize(byte[] Input, int Size){
		byte[] output = new byte[Size];
		if(Size <= Input.length){
			System.arraycopy(Input, 0, output, 0, Size);
		}else{
			System.arraycopy(Input, 0, output, 0, Input.length);
		}
		return output;
	}
	
	/**
	 * Resizes the Key to new Size
	 * @param Key the old key
	 * @param Size the new Size in bit
	 * @param Blocksize the Blocksize in bit
	 * @return new Key with given size
	 * @throws IllegalArgumentException if size does not fit in blocks
	 */
	public static byte[] resizeKey(byte[] Key, int Size, int Blocksize) throws IllegalArgumentException{
		if((Size % 8) != 0 || Size < Blocksize){
			throw new IllegalArgumentException();
		}
		byte[] newkey = new byte[Size/8];
		System.arraycopy(Key, 0, newkey, 0, Size/8);
		return newkey;
	}
	
	/**
	 * Cuts all null bytes following the last byte != 0.
	 * @param input the byte array to resize
	 * @return the resized array
	 */
	public static byte[] resizeArray(byte[] input){
		if(input == null) return null;
		int position = 0; 
		for(int i = 0; i < input.length; ++i){
			if(input[i] != 0) position = i;
		}
		byte[] result = new byte[position+1];
		System.arraycopy(input, 0, result, 0, position+1);
		return result;
	}
	
	/**
	 * Generates an up to 64 Bits long IV. This IV is probably random, with maximum number of zero bits.
	 * @param length the length the IV should have, up to 8 Bytes
	 * @param number_of_zero_bits the maximum number of zero bits in one flow in IV
	 * @return the IV
	 */
	public static byte[] generateIV(int length, int number_of_zero_bits){
		if(length > 8) throw new IllegalArgumentException("Length should be 8 Bytes maximum.");
		byte[] IV = new byte[length];
		
		SecureRandom sr = null;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");

			int i = 0;
			while(i < length){
				ByteBuffer bb = ByteBuffer.allocate(8);  
				byte[] part = bb.putLong(sr.nextLong()).array();
				int bytesToCopy = 8;
				if(length - i < 8) bytesToCopy = length - i;
				System.arraycopy(part, 0, IV, i, bytesToCopy);
				i += 8;
			}
			

			while(countNumberOfZeroBitsInFlow(IV) > number_of_zero_bits){
				i = 0;
				while(i < length){
					ByteBuffer bb = ByteBuffer.allocate(8);  
					byte[] part = bb.putLong(sr.nextLong()).array();
					int bytesToCopy = 8;
					if(length - i < 8) bytesToCopy = length - i;
					System.arraycopy(part, 0, IV, i, bytesToCopy);
					i += 8;
				}
			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return IV;
	}
	
	/**
	 * Generates an up to 64 Bits long IV. This IV is probably random.
	 * @param length the length the IV should have, up to 8 Bytes
	 * @return the IV
	 */
	public static byte[] generateIV(int length){
		if(length > 8) throw new IllegalArgumentException("Length should be 8 Bytes maximum.");
		byte[] IV = new byte[length];
		
		SecureRandom sr = null;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");

			int i = 0;
			while(i < length){
				ByteBuffer bb = ByteBuffer.allocate(8);  
				byte[] part = bb.putLong(sr.nextLong()).array();
				int bytesToCopy = 8;
				if(length - i < 8) bytesToCopy = length - i;
				System.arraycopy(part, 0, IV, i, bytesToCopy);
				i += 8;
			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return IV;
	}
	
	/**
	 * Counts the number of zero bit in one flow. It needs at least one zero byte to start counting.
	 * @param input the IV
	 * @return the Number of zero bits
	 */
	public static int countNumberOfZeroBitsInFlow(byte[] input){
		int result = 0;
		
		for(int i = 0; i < input.length; ++i){
			if(input[i] == 0){
				int localresult = 0;
				localresult +=8;
				//test the byte in front of first zero byte in this sequence
				if(i > 0){
					int j = 0;
					while(((input[i-1] >> j) & (byte)0x01) == 0){
						++localresult;
						++j;
					}
				}
				//test the following bytes in this sequence
				++i;
				while(i < input.length && input[i] == 0){
					++i;
					localresult += 8;
				}
				//test the last byte in this sequence
				if(i < input.length){
					int j = 7;
					while(((input[i] >> j) & (byte)0x01) == 0){
						++localresult;
						--j;
					}
				}
				//test if the current sequence is longer than the longest
				if(localresult > result) result = localresult;
			}
		}
		
		return result;
	}
}
