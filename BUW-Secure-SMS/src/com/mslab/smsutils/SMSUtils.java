package com.mslab.smsutils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;


public class SMSUtils {
	
	/**
	 * Re-encodes an ASCII string to represent 7-bit char encoding as used in SMS. 
	 * Generates a new array with less or equal length.
	 * @param eightBitsPerChar A byte array, where every character is encoded as 8 bits. 
	 * @return A byte array, where every character is encoded as 7 bits.
	 */
	public static byte[] compressStringTo7BitPerCharEncoding(byte[] eightBitsPerChar) {
		int inputLength = eightBitsPerChar.length;
		
		int rest = inputLength % 8;
		
		if (inputLength % 8 != 0) {
			inputLength -= inputLength % 8;
		}
		
		int resultLength = (int)Math.ceil((7.f * (float)eightBitsPerChar.length) / 8.f);
		
		byte[] sevenBitsPerChar = new byte[resultLength];
		long eightBytes;
		final byte sevenbits = 0x7F; 
		final int eightbits = 0xFF; 
		
		for (int i = 0, j = 0; i < inputLength; ) {
			eightBytes = (long)(eightBitsPerChar[i++] & sevenbits) << 57
				| (long)(eightBitsPerChar[i++] & sevenbits) << 50
				| (long)(eightBitsPerChar[i++] & sevenbits) << 43
				| (long)(eightBitsPerChar[i++] & sevenbits) << 36
				| (long)(eightBitsPerChar[i++] & sevenbits) << 29
				| (long)(eightBitsPerChar[i++] & sevenbits) << 22
				| (long)(eightBitsPerChar[i++] & sevenbits) << 15
				| (long)(eightBitsPerChar[i++] & sevenbits) << 8;
			
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 56) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 48) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 40) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 32) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 24) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 16) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 8) & eightbits);
		}
		
		//handle the rest
		int offset = 57;
		eightBytes = 0L;
		for(int i = 0; i < rest; ++i){
			eightBytes |= (long)(eightBitsPerChar[inputLength + i] & sevenbits) << (offset-i*7);
		}
//		System.out.println(Long.toBinaryString(eightBytes));
		for(int i = 0; i < rest; ++i){
			sevenBitsPerChar[resultLength - rest + i] = (byte)((eightBytes >>> ((offset-1) - i * 8)) & eightbits);
		}
		
		return sevenBitsPerChar;
	}
	
	/**
	 * Re-encodes an UTF-8 byte to represent 7-bit char encoding as used in SMS. 
	 * Generates a new array with less or equal length.
	 * @param eightBitsPerChar A byte array, where every character is encoded as 8 bits. 
	 * @return A byte array, where every character is encoded as 7 bits.
	 */
	public static byte[] compressBytesTo7BitPerCharEncoding(byte[] eightBitsPerChar) {
		final int[] masks = {0x40, 0x60, 0x70, 0x78, 0x7C, 0x7E, 0x7F};
		int inputLength = eightBitsPerChar.length;
		
		if (inputLength % 8 != 0) {
			inputLength -= inputLength % 8;
		}
		
		int resultLength = (int)Math.floor((7.f * (float)eightBitsPerChar.length) / 8.f);
		
		int restbytes = resultLength % 7;
		int restbits = resultLength % 7;
		
		byte[] sevenBitsPerChar = new byte[resultLength];
		long eightBytes;
		final byte sevenbits = 0x7F; 
		final int eightbits = 0xFF; 
		
		for (int i = 0, j = 0; i < inputLength; ) {
			eightBytes = (long)(eightBitsPerChar[i++] & sevenbits) << 57
				| (long)(eightBitsPerChar[i++] & sevenbits) << 50
				| (long)(eightBitsPerChar[i++] & sevenbits) << 43
				| (long)(eightBitsPerChar[i++] & sevenbits) << 36
				| (long)(eightBitsPerChar[i++] & sevenbits) << 29
				| (long)(eightBitsPerChar[i++] & sevenbits) << 22
				| (long)(eightBitsPerChar[i++] & sevenbits) << 15
				| (long)(eightBitsPerChar[i++] & sevenbits) << 8;
			
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 56) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 48) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 40) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 32) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 24) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 16) & eightbits);
			sevenBitsPerChar[j++] = (byte)((eightBytes >>> 8) & eightbits);
		}
		
		//handle the rest
		int offset = 57;
		eightBytes = 0L;
		for(int i = 0; i < restbytes; ++i){
			eightBytes |= (long)(eightBitsPerChar[inputLength + i] & sevenbits) << (offset-i*7);
//			System.out.println(Long.toBinaryString(eightBytes));
		}
//		System.out.println(Long.toBinaryString(eightBytes));
		offset = 57;
		for(int i = 0; i < restbytes; ++i){
			sevenBitsPerChar[resultLength - restbytes + i] = (byte)((eightBytes >>> ((offset-1) - i * 8)) & eightbits);
		}
		//handle restbits
		if(restbits > 0){
			eightBytes = 0l;
			eightBytes |= (long)(eightBitsPerChar[eightBitsPerChar.length - 1] & masks[restbits-1]) >> (7 - restbits);
			sevenBitsPerChar[sevenBitsPerChar.length-1] |= (byte)eightBytes;//(byte)((eightBytes >>> restbits) & masks[restbits-1]);
//			System.out.println(Long.toBinaryString(eightBytes));
		}
		
		return sevenBitsPerChar;
	}
	
	/**
	 * Re-encodes an ASCII string to represent 8-bit char encoding as usual. 
	 * Generates a new array with greater or zero length. The String contains n * 7 bit data in maximum. If the input contains
	 * more data, please see expandByteTo8BitPerChar() method.
	 * @param sevenBitsPerChar A byte array, where every character is encoded as 7 bits. 
	 * @return A byte array, where every character is encoded as 8 bits. A zero is appended as the new MSB 
	 * to every 7-bit value.
	 */
	public static byte[] expandStringTo8BitPerChar(byte[] sevenBitsPerChar) {
		final int sevenbits = 0x7F; 
		final int eightbits = 0xFF; 
		int inputLength = (int)Math.floor(8.f * (float)sevenBitsPerChar.length / 7.f);
		final byte[] eightBitsPerChar = new byte[inputLength];
		long eightBytes;
		
		int rest = inputLength%8;
		inputLength -= rest;
		
		for (int i = 0, j = 0; i < inputLength; ) {
			eightBytes = (long)(sevenBitsPerChar[j++] & eightbits) << 48
				| (long)(sevenBitsPerChar[j++] & eightbits) << 40
				| (long)(sevenBitsPerChar[j++] & eightbits) << 32
				| (long)(sevenBitsPerChar[j++] & eightbits) << 24
				| (long)(sevenBitsPerChar[j++] & eightbits) << 16
				| (long)(sevenBitsPerChar[j++] & eightbits) << 8
				| (long)(sevenBitsPerChar[j++] & eightbits);
			
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 49) & sevenbits);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 42) & sevenbits);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 35) & sevenbits);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 28) & sevenbits);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 21) & sevenbits);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 14) & sevenbits);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 7) & sevenbits);
			eightBitsPerChar[i++] = (byte)(eightBytes & sevenbits);
		}
		
		int offset = 48;
		eightBytes = 0l;
		for(int i = 0; i < rest; ++i){
			eightBytes |= (long)(sevenBitsPerChar[inputLength*7/8 + i] & eightbits) << (offset - i * 8);
		}
//		System.out.println(Long.toBinaryString(eightBytes));
		for(int i = 0; i < rest; ++i){
			eightBitsPerChar[inputLength + i] = (byte)((eightBytes >>> ((offset + 1) - i * 7)) & sevenbits);
		}
		
		return eightBitsPerChar;
	}
	
	/**
	 * Re-encodes an 8-bit byte string to represent 8-bit char encoding as usual. 
	 * Generates a new array with greater or zero length.
	 * @param sevenBitsPerChar A byte array, where every character is encoded as 7 bits. 
	 * @return A byte array, where every character is encoded as 8 bits. A zero is appended as the new MSB 
	 * to every 7-bit value.
	 */
	public static byte[] expandBytesTo8BitPerChar(byte[] sevenBitsPerChar) {
		final int[] masks = {0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, 0xFF};
		
		int inputLength = (int)Math.ceil(8.f * (float)sevenBitsPerChar.length / 7.f);
		
		final byte[] eightBitsPerChar = new byte[inputLength];
		long eightBytes;
		
		int restbytes = sevenBitsPerChar.length%7;

		int restbits = sevenBitsPerChar.length%7;
		
		inputLength -= Math.ceil(restbytes * 8.f / 7.f);
		
		for (int i = 0, j = 0; j < (sevenBitsPerChar.length - restbytes); ) {
			eightBytes = (long)(sevenBitsPerChar[j++] & masks[7]) << 48
				| (long)(sevenBitsPerChar[j++] & masks[7]) << 40
				| (long)(sevenBitsPerChar[j++] & masks[7]) << 32
				| (long)(sevenBitsPerChar[j++] & masks[7]) << 24
				| (long)(sevenBitsPerChar[j++] & masks[7]) << 16
				| (long)(sevenBitsPerChar[j++] & masks[7]) << 8
				| (long)(sevenBitsPerChar[j++] & masks[7]);
			
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 49) & masks[6]);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 42) & masks[6]);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 35) & masks[6]);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 28) & masks[6]);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 21) & masks[6]);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 14) & masks[6]);
			eightBitsPerChar[i++] = (byte)((eightBytes >>> 7) & masks[6]);
			eightBitsPerChar[i++] = (byte)(eightBytes & masks[6]);
		}
		
		//handle the rest bytes
		int offset = 48;
		eightBytes = 0l;
		for(int i = 0; i < restbytes; ++i){
			eightBytes |= (long)(sevenBitsPerChar[sevenBitsPerChar.length - restbytes + i] & masks[7]) << (offset - i * 8);
//			System.out.println(Long.toBinaryString(eightBytes));
		}
//		System.out.println(Long.toBinaryString(eightBytes));
		offset = 48;
		for(int i = 0; i < restbytes; ++i){
			eightBitsPerChar[inputLength + i] = (byte)((eightBytes >>> ((offset + 1) - i * 7)) & masks[6]);
		}
		
		//handle the rest bits
		if(restbits > 0){
			eightBytes = 0l;
			eightBytes |= (long)(sevenBitsPerChar[sevenBitsPerChar.length - 1] & masks[restbits-1]) << (7-restbits);
			eightBitsPerChar[eightBitsPerChar.length-1] |= (byte)eightBytes;//(byte)((eightBytes >>> restbits) & masks[restbits-1]);
//			System.out.println(Long.toBinaryString(eightBytes));
		}
		
		return eightBitsPerChar;
	}
	
	/**
	 * Appends 10* + length padding to the input. If the input's length is a multiple of the blocksize, 
	 * 32 bits for padding are added. If it is not a multiple, we fill the current block and add 32 bit.
	 * The method returns always a byte array of length n * blocksize + 4 (in bytes, 1 <= n).
	 * @param input - the input message
	 * @param Blocksize - the blocksize in bits
	 * @return the padded array
	 */
//	public static byte[] appendPadding(byte[] input, final int Blocksize){
//		int blocksize_in_byte = Blocksize / 8;
//		int number_of_blocks = (int)Math.ceil((float)input.length / (float)blocksize_in_byte);
//		int outputlength = number_of_blocks * blocksize_in_byte + 4;
//		byte[] output = new byte[outputlength];
//		
//		System.arraycopy(input, 0, output, 0, input.length);
//		
//		int selectedByte = input.length - 1;
//		int i = 0;
//		for(i = 0; i < 8; ++i){
//			if( ((output[selectedByte] >> i) & 0x01) == 1 ){
//				 break;
//			}
//		}
//		//if the current byte is fully used, we add the padding one byte above
//		if(i == 0){
//			selectedByte++;
//			i = 7;
//		}else{
//			i = (i + 7) % 8;
//		}
//		
//		output[selectedByte] |= (1 << (i));
//		//adding 10* padding done, now we add the length of the message
//		
//		ByteBuffer bb = ByteBuffer.allocate(2);
//		bb.order(ByteOrder.LITTLE_ENDIAN);
//		bb.putShort((short)input.length);
//		
//		output[output.length-2] = bb.get(1);
//		output[output.length-1] = bb.get(0);
//		
//		return output;
//	}
	public static byte[] appendPadding32(byte[] input, final int Blocksize){
		int blocksize_in_byte = Blocksize / 8;
		int number_of_blocks = (int)Math.ceil((float)input.length / (float)blocksize_in_byte);
		int outputlength = number_of_blocks * blocksize_in_byte + 4;
		byte[] output = new byte[outputlength];
		
		System.arraycopy(input, 0, output, 0, input.length);
		
		int selectedByte = input.length;
		//append 0x80
		output[selectedByte] |= 0x80;
		//adding 10* padding done, now we add the length of the message
		
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short)input.length);
		
		output[output.length-2] = bb.get(1);
		output[output.length-1] = bb.get(0);
		
		return output;
	}
	
	/**
	 * Appends a 8 bit 10* padding to the input. The output will be a byte array of multiple length of block size plus one half block.
	 * @param input - the input to be padded
	 * @param Blocksize - the block size in bits
	 * @return the padded message
	 */
	public static byte[] appendPadding8(byte[] input, final int Blocksize){
		int blocksize_in_byte = Blocksize / Byte.SIZE;
		int number_of_blocks = (int)Math.ceil((float)(input.length - 7) / (float)blocksize_in_byte);
		int outputlength = number_of_blocks * blocksize_in_byte + 8;
		byte[] output = new byte[outputlength];
		
		System.arraycopy(input, 0, output, 0, input.length);
		
		int selectedByte = input.length;
		//append 0x08
		output[selectedByte] = (byte)0x80;
		
		return output;
	}
	
	/**
	 * Appends a 8 bit 10* padding to the input. The output will be a byte array of SMS length. That means the result will be of length 120, 254, 388, ... bytes.
	 * One SMS: [896 bits = 7 * 128 MSG][56 bits MSG][8 bits Padding]
	 * Multiple SMS: [896 bits = 7 * 128 MSG][56 bits MSG][1024 bits = 8 * 128 MSG] ... [8 bits Padding] 
	 * @param input - the input to be padded
	 * @param Blocksize - the block size in bits
	 * @return the padded message
	 */
	public static byte[] appendPadding8SMSLength(byte[] input){
		final int blocksize_in_byte = 16; // 128 Bits
		int number_of_blocks = (int)Math.ceil((float)(input.length - 7) / (float)blocksize_in_byte);
		//expand to full SMS
		if(number_of_blocks < 7){
			number_of_blocks = 7;
		}else 
			if(((number_of_blocks - 7) % 8) != 0){
				number_of_blocks +=  (8 - ((number_of_blocks - 7) % 8)); // 8 blocks for second message
		}
		int outputlength = number_of_blocks * blocksize_in_byte + 8;
		byte[] output = new byte[outputlength];
		
		System.arraycopy(input, 0, output, 0, input.length);
		
		int selectedByte = input.length;
		//append 0x08
		output[selectedByte] = (byte)0x80;
		
		return output;
	}
	
	/**
	 * Appends a 4 bit 10* padding to the input. This is represented as one half byte.
	 * @param input - the input to pad
	 * @param Blocksize - the block size in bits
	 * @return the padded message
	 */
	public static byte[] appendPadding4(byte[] input, final int Blocksize){
		int blocksize_in_byte = Blocksize / 8;
		int number_of_blocks = (int)Math.ceil((float)(input.length - 4) / (float)blocksize_in_byte);
		int outputlength = number_of_blocks * blocksize_in_byte + 4;
		byte[] output = new byte[outputlength];
		
		System.arraycopy(input, 0, output, 0, input.length);
		
		int selectedByte = input.length-1;
		if(selectedByte < 0)
			selectedByte = 0;
		//append 0x08
		if((output[selectedByte] & 0x08) == 0){
			output[selectedByte] |= 0x08;
		}else{
			++selectedByte;
			output[selectedByte] |= 0x80;
		}
		
		return output;
	}
	
	/**
	 * Remove the length and 10* padding from input. It further proofs, if the length in 
	 * padding and the output length are equal. Otherwise it will return a null object.
	 * @param input - the length and 10* padded input
	 * @return the result without padding or null, if the length does not fit to each other
	 */
	public static byte[] removePadding32(byte[] input){
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.put(input[input.length-1]);
		bb.put(input[input.length-2]);
		int probableLength = bb.getShort(0);
		
		int inputByte = 0;
		for(int i = input.length-3; i >= 0; --i){//for all bytes
			if(input[i] == (byte)0x80){
				inputByte = i;
				break;
			}
		}
		
		byte[] output = new byte[inputByte];
		System.arraycopy(input, 0, output, 0, inputByte);
		
		if(output.length == probableLength){
			return output;
		}
		
		return null;
		
	}
	
	/**
	 * Removes 8 bit 10* padding from input. The output will be shrinked to the size without padding.
	 * @param input - the padded input
	 * @return the unpadded result
	 */
	public static byte[] removePadding8(byte[] input){
		int inputByte = 0;
		for(inputByte = input.length - 1; inputByte >= 0; --inputByte){
			if((input[inputByte] & 0x80) == 0x80){
				break; //padding byte found
			}
		}
		
		if(inputByte < 0)
			inputByte = 0;
		byte[] output = new byte[inputByte];
		System.arraycopy(input, 0, output, 0, inputByte);
		
		return output;
	}
	
	/**
	 * Removes 4 bit 10* padding from input. It may returns a byte array that is one byte shorter than the input.
	 * @param input - the padded input
	 * @return the unpadded result
	 */
	public static byte[] removePadding4(byte[] input){
		int inputByte = 0;
		for(inputByte = input.length - 1; inputByte >= 0; --inputByte){
			if((input[inputByte] & 0x08) == 0x08){
				//the last byte was used for padding
				input[inputByte] = (byte) (input[inputByte] & ~ 0x08);
				break;
			}else 
				if((input[inputByte] & 0x80) == 0x80){
				//one byte was appended for padding
				--inputByte;
				break;
			}
		}
		
		byte[] output = new byte[inputByte+1];
		System.arraycopy(input, 0, output, 0, inputByte+1);
		//if input is a zero byte, the unpadded message was empty
		if(output.length == 1 && input[0] == (byte)0)
			output = new byte[0];
		
		return output;
	}
	
//	//Index: ASCII value, value -> GSM
	private static char[] GSM_encoding = {'@', '£', '$', '¥', 'è', 'é', 'ù', 'ì', 'ò', 'Ç', '\n', 'Ø', 'ø', '\r', 'Å', 'å',
		'Δ', '_', 'Φ', 'Γ', 'Λ', 'Ω', 'Π', 'Ψ', 'Σ', 'Θ', 'Ξ', '[', 'Æ', 'æ', 'ß', 'É', 
		' ', '!', '"', '#', ']', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?',
		'{', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'Ä', 'Ö', 'Ñ', 'Ü', '}',
		'¿', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
		'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'ä', 'ö', 'ñ', 'ü', 'à'};
	
	//the ESQ - char could not be used
	//in german network O2 (Telefonica) there are Problemns sending the chars '¤', '¡', '§', they will be transformed to:
	//'¤' -> 'o', we replace it by ']'
	//'¡' -> '¿', we replace it by '{'
	//'§' -> '_', we replace it by '}'
	//by the provider
	
//	private static int[] GSM_encoding = {64, 163, 36, 165, 232, 233, 249, 236, 242, 199, 10, 216, 248, 13, 197, 229, 196, 95, 214, 195, 203, 217, 208, 216, 211, 200, 206, 27, 198, 230, 223, 201, 32, 33, 34, 35, 164, 
//			37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 161, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
//			196, 214, 209, 220, 167, 191, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 228, 246, 241, 252, 224};
	/**
	 * Converts a given ASCII string to GSM 03.38 compatible encoding.
	 * @param ascii - the input String in 7-bit ASCII encoding
	 * @return a gsm compatible String
	 */
	public static String convertToGSM(String ascii){
		String gsm = "";
		try{
			for(int i = 0; i < ascii.length();++i){
//				System.out.println(", "+(int)ascii.charAt(i));
				gsm += GSM_encoding[(int)ascii.charAt(i)];
			}
		}catch(ArrayIndexOutOfBoundsException aiob){
			throw new IllegalArgumentException("byte value is larger than 7 bits");
		}
		return gsm;
	}
	
	/**
	 * Converts a given GSM 03.38 string to ASCII compatible encoding.
	 * @param ascii - the input String in 7-bit GSM 03.38 encoding
	 * @return a ASCII String
	 */
	public static String convertToASCII(String gsm){
		HashMap<Character, Integer> charset = new HashMap<Character, Integer>();
		for(int i = 0; i < GSM_encoding.length; ++i){
			charset.put(GSM_encoding[i], i);
		}
		
		String ascii = "";
		
		for(int i = 0; i < gsm.length(); ++i){
			ascii += (char)charset.get(gsm.charAt(i)).intValue();
		}
		
		return ascii;
	}
	
	/**
	 * Converts any 8-Bit character ASCII string to a Base123 string, without the difficult characters.
	 * ATTENTION: leading zeros will be lost!
	 * @param ascii - the input
	 * @return a Base123 coded string
	 */
	
	private static char[] Base123_encoding = {'£', '$', '¥', 'è', 'é', 'ù', 'ì', 'ò', 'Ç', '\n', 'Ø', 'ø', '\r', 'Å', 'å',
		'Δ', '_', 'Φ', 'Γ', 'Λ', 'Ω', 'Π', 'Ψ', 'Σ', 'Θ', 'Ξ', 'Æ', 'æ', 'ß', 'É', 
		' ', '!', '"', '#', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?',
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'Ä', 'Ö', 'Ñ', 'Ü',
		'¿', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
		'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'ä', 'ö', 'ñ', 'ü', 'à'};
	
	public static String encodeBase123(byte[] ascii){
		
//		System.out.println("Ascii: "+Arrays.toString(ascii.getBytes()));
		
		ArrayList<Byte> output = new ArrayList<Byte>();
		
		BigInteger input = new BigInteger(1, ascii);
		
//		System.out.println("BigInt: "+input.toString());
		
		while(input.compareTo(BigInteger.ZERO) > 0){
			output.add(input.mod(BigInteger.valueOf(123)).byteValue());
			input = input.divide(BigInteger.valueOf(123));;
		}
		
		//debug
//		System.out.println(output.toString());
		
		StringBuilder outputstring = new StringBuilder();
		for(byte b : output){
			outputstring.append(Base123_encoding[(int)b]);
		}
		
		return outputstring.toString();
	}
	
	/**
	 * Decodes Base123 encoded string into an ascii string.
	 * @param base123encoded - base123 encoded string
	 * @return an ascii string
	 */
	public static byte[] decodeBase123(String base123encoded){
		if(!isProbablyBase123Encoded(base123encoded)) return new byte[0];
		
		HashMap<Character, Integer> charset = new HashMap<Character, Integer>();
		for(int i = 0; i < Base123_encoding.length; ++i){
			charset.put(Base123_encoding[i], i);
		}
		
		StringBuilder ascii = new StringBuilder();
		//convert to ascii
		for(int i = 0; i < base123encoded.length(); ++i){
			ascii.append((char)charset.get(base123encoded.charAt(i)).intValue());
		}
		
		//debug
//		System.out.println(Arrays.toString(ascii.getBytes()));
		

		//reverse the ascii string
		ascii = new StringBuilder(new StringBuffer(ascii.toString()).reverse().toString());
		BigInteger result = BigInteger.valueOf(0);
		byte[] input = ascii.toString().getBytes();
		
		//convert to biginteger
		int i = 0;
		result = BigInteger.valueOf(0);
		while(i < input.length){
			result = result.add(BigInteger.valueOf(input[i]));
			result = BigInteger.valueOf(123).multiply(result);
			++i;
		}
		result = result.divide(BigInteger.valueOf(123));
		
		//cut off leading zero byte
		byte[] output = result.toByteArray();
		if(output.length > 0 && output[0] == 0){
			byte[] temp = new byte[output.length-1];
			System.arraycopy(output, 1, temp, 0, temp.length);
			output = temp;
		}
		
//		System.out.println("BitInt: "+result.toString());
//		System.out.println("ASCII: "+Arrays.toString(output));
		
		return output;
	}
	
	/**
	 * This method checks if the input is probably base123 encoded.
	 * @param input the inputstring to check
	 * @return true if it is, false otherwise
	 */
	public static boolean isProbablyBase123Encoded(String input){
		boolean result = true;
		CharSequence[] chars = {"@", "¤", "¡", "§", Character.toString((char)((byte)0x1b))};

		for( CharSequence cs : chars ){
			result &= !input.contains(cs);
		}
		
		return result;
	}

	/**
	 * This method removes all zeros in series, if the series are longer than 1 byte. The series will be replaces with '0' + '0' + number of zeros in series
	 * @param input - the unencoded input
	 * @return the same byte array with encoded zeros
	 */
	public static byte[] appendZeroEncoding(byte[] input){
		ArrayList<Byte> temp = new ArrayList<Byte>();
		
		for(int i = 0; i < input.length; ++i){
			if(input[i] != 0){
				temp.add(input[i]);
			}else{
				//count the number of following zeros
				int count = 0;
				for(int j = 0; (j + i) < input.length; ++j){
					if(input[i + j] == 0){
						++count;
					}else{
						break;
					}
				}
				if(count == 1){
					temp.add(input[i]);
				}else{
					temp.add((byte)0);
					temp.add((byte)0);
					temp.add((byte)count);
					i += count -1;
				}
			}
		}
		byte[] output = new byte[temp.size()];
		
		for(int i = 0; i < output.length; ++i){
			output[i] =  temp.get(i);
		}
		return output;
	}
	
	/**
	 * Removes the first applied zero length encoding.
	 * @param input - the zero length encoded input
	 * @return the output without encoding
	 */
	public static byte[] removeZeroEncoding(byte[] input){
		ArrayList<Byte> temp = new ArrayList<Byte>();
		
		for(int i = 0; i < input.length; ++i){
			if(input[i] != 0){
				temp.add(input[i]);
			}else 
				if((i+1) < input.length){
					if(input[i+1] != 0){
						temp.add(input[i]);
					}else{
						//two zeros in series
						i += 2;
						int count = input[i];
						while(count > 0){
							--count;
							temp.add((byte)0);
						}
				}
			}else{
				temp.add(input[i]);
			}
		}
		
		byte[] output = new byte[temp.size()];
		for(int i = 0; i < output.length; ++i){
			output[i] =  temp.get(i);
		}
		return output;
	}
}
