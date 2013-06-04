package com.mslab.McOE.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.ThreefishCipher;
import org.junit.Test;

import com.mslab.McOE.GenericMcOE;
import com.mslab.McOE.GenericMcOETagSplit;
import com.mslab.McOE.McOE;
import com.mslab.McOE.McOEX;


public class McOETest {
	
	static byte[] Key = "This is an absolute ultimate Key".getBytes();
	static byte[] Key128 = "This is an absol".getBytes();
	
	@Test
	public void testXOR() {
		byte[] left = {(byte) 170, (byte) 170, (byte) 170};
		byte[] right = {(byte) 85, (byte) 85, (byte) 85};
		
		byte[] result = {(byte) 255, (byte) 255, (byte) 255};
		
		McOE mcoe = new GenericMcOE();
		
		assertArrayEquals(result, mcoe.XOR(left, 0, right, 0, 3));
	}
	
	@Test
	public void testXORwithOffset() {
		byte[] left = {(byte) 123, (byte) 170, (byte) 255, (byte) 45};
		byte[] right = {(byte) 8, (byte) 5, (byte) 85};
		
		byte[] result = {(byte) 250, (byte) 120};
		
		McOE mcoe = new GenericMcOE();
		
		assertArrayEquals(result, mcoe.XOR(left, 2, right, 1, 2));
	}
	
	@Test
	public void testSingleByteToLongArray(){
		byte[] input = {(byte) 170};
		long[] result = {-6196953087261802496l};
		
		McOE mcoe = new GenericMcOE();
		assertArrayEquals(result, mcoe.byteToLongArray(input));
		
	}

	@Test
	public void testByteToLongArray(){
		byte[] input = {0, 0, 0, 0, (byte) 170, (byte)34, (byte)75, (byte)11, 0, 0, 0, 0, (byte)27, (byte)15, (byte) 146, (byte)123};
		long[] result = {2854374155l, 454005371l};
		
		McOE mcoe = new GenericMcOE();
		
		assertArrayEquals(result, mcoe.byteToLongArray(input));
		
	}
	
	@Test
	public void testEqualsBlock(){
		byte[] left = {(byte) 123, (byte) 170, (byte) 255, (byte) 45};
		byte[] right = {(byte) 8, (byte) 170, (byte) 255};
		
		McOE mcoe = new GenericMcOE();
		
		assertEquals(true, mcoe.equalsBlock(left, 1, right, 1, 2));
		
	}
	
	@Test
	public void testEqualsBlockFail(){
		byte[] left = {(byte) 123, (byte) 170, (byte) 255, (byte) 45};
		byte[] right = {(byte) 8, (byte) 170, (byte) 255};
		
		McOE mcoe = new GenericMcOE();
		
		assertEquals(false, mcoe.equalsBlock(left, 1, right, 0, 3));
		
	}
	
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testEqualsBlockLength(){
		byte[] left = {(byte) 123, (byte) 170, (byte) 255, (byte) 45};
		byte[] right = {(byte) 8, (byte) 170, (byte) 255};
		
		McOE mcoe = new GenericMcOE();
		
		assertEquals(true, mcoe.equalsBlock(left, 1, right, 2, 2));
		
	}
	
	@Test
	public void textEnsureBlockSize(){
		byte[] Input = new byte[5];
		Input[1] = (byte)13;
		
		McOE mcoe = new GenericMcOE();
		
		Input = mcoe.ensureBlockSize(Input, 32);
		
		assertEquals(27, Input.length - 5);
		assertEquals(32, Input.length);
		assertEquals((byte)13, Input[1]);
		
	}
	
	@Test
	public void testEnDeCryption(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOE();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionLongHeader(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[48];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOE();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionEmptyString(){
		
		byte[] Message = "".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOE();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionLongString(){
		
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOE();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionCorruptedTag(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOE();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
//		for(int i = 0; i < Blocksize / 8; ++i){
//			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
//		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertEquals(true, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key) == null);
		
	}
	
	@Test
	public void testEnDeCryptionSMSString(){
		
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At v".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOE();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnsureBlocksize(){
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam".getBytes();

		McOE mcoe = new GenericMcOE();
		
		Message = mcoe.ensureBlockSize(Message, 32);
		
		assertEquals(96, Message.length);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testKeylengthErrorMcOEXEn(){
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At v".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		byte[] Key = "bla".getBytes();
		
		McOE mcoe;
		mcoe = new McOEX();
		mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testKeylengthErrorMcOEXDe(){
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		byte[] Key = "bla".getBytes();
		byte[] Tag = null, CiphertextWithoutTag = null;
		
		McOE mcoe;
		mcoe = new McOEX();
		mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testKeylengthErrorMcOETSEn(){
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At v".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		byte[] Key = "bla".getBytes();
		
		McOE mcoe;
		
		mcoe = new GenericMcOETagSplit();
		mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testKeylengthErrorMcOETSDe(){
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		byte[] Key = "bla".getBytes();
		byte[] Tag = null, CiphertextWithoutTag = null;
		
		McOE mcoe;
		mcoe = new GenericMcOETagSplit();
		mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testKeylengthErrorMcOEGenericEn(){
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At v".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		byte[] Key = "bla".getBytes();
		
		McOE mcoe;
		mcoe = new GenericMcOE();
		mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testKeylengthErrorMcOEGenericDe(){
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		byte[] Key = "bla".getBytes();
		byte[] Tag = null, CiphertextWithoutTag = null;
		
		McOE mcoe;
		
		mcoe = new GenericMcOE();
		mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key);
	}
	
	@Test
	public void testLongToByteArray(){
		
		McOE mcoe = new GenericMcOE();
		
		byte[] Result = {0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, (byte) 1,(byte)115, (byte)40, (byte)204, (byte) 0};
		
		assertArrayEquals(Result, mcoe.longToByteArray(6227020800l, 32));
	}
	
	@Test
	public void testResizeArrayToSize(){
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam".getBytes();
		byte[] Result = "Lorem ipsum dolo".getBytes();
		McOE mcoe = new GenericMcOE();
		
		assertEquals(16, mcoe.resizeArrayToSize(Message, 16).length);
		assertArrayEquals(Result, mcoe.resizeArrayToSize(Message, 16));
		
		assertEquals(25, mcoe.resizeArrayToSize(Result, 25).length);
		
		
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testResizeKeyToSize(){
		byte[] key = {15, 32, 11, 99, 127, 85, 69, 13, 0, 0, 1, 23, 17, 19};
		byte[] result = {15, 32, 11, 99, 127, 85, 69, 13, 0, 0, 1, 23};
		int Blocksize = 128;
		assertArrayEquals(result, McOE.resizeKey(key, 96, Blocksize));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testResizeKeyToSizeFailure1(){
		byte[] key = null;
		int Blocksize = 128;
		McOE.resizeKey(key, 1, Blocksize);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testResizeKeyToSizeFailure2(){
		byte[] key = null;
		int Blocksize = 128;
		McOE.resizeKey(key, 129, Blocksize);
	}
	
	@Test
	public void testGenerateIV(){
		assertEquals(8, McOE.generateIV(8).length);
		assertEquals(7, McOE.generateIV(7).length);
		
		assertEquals(8, McOE.generateIV(8, 10).length);
		assertEquals(7, McOE.generateIV(7, 10).length);
		
		assertEquals(7, McOE.generateIV(7, 1).length);
		assertEquals(1, McOE.generateIV(1, 0).length);
		assertEquals(1, McOE.generateIV(1, 9).length);
		assertEquals(8, McOE.generateIV(8, 63).length);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGenerateIVFailureWithoutZero(){
		McOE.generateIV(9, 10);
		McOE.generateIV(9);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGenerateIVFailure(){
		McOE.generateIV(9);
	}
	
	
	@Test
	public void testResizeArray(){
		byte[] input = {11, 17, 28, 37, 99, 0, 0};
		byte[] expected = {11, 17, 28, 37, 99};
		assertEquals(5, McOE.resizeArray(input).length);
		assertArrayEquals(expected, McOE.resizeArray(input));
		assertEquals(true, McOE.resizeArray(null) == null);
	}
	
	@Test
	public void testEnDeCryptionTagSplitting(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOETagSplit();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingLongHeader(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[48];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOETagSplit();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingEmptyMessage(){
		
		byte[] Message = "".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOETagSplit();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingLongString(){
		
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOETagSplit();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplitSMSString(){
		
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At v".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOETagSplit();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingCorruptedTag(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOETagSplit();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
//		for(int i = 0; i < Blocksize / 8; ++i){
//			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
//		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertEquals(true, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key) == null);
		
	}
	
	@Test
	public void testEnDeCryptionTagSplitTwoBlocks(){
		
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed dia".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new ThreefishCipher();
		
		McOE mcoe = new GenericMcOETagSplit();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, 32);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingX(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new AESEngine();
		
		McOE mcoe = new McOEX();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, Blocksize/8);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingXLongHeader(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[48];
		int Blocksize = 256;
		
		BlockCipher Cipher = new AESEngine();
		
		McOE mcoe = new McOEX();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, Blocksize/8);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingXemptyMessage(){
		
		byte[] Message = "".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new AESEngine();
		
		McOE mcoe = new McOEX();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, Blocksize/8);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingXlongMessage(){
		
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new AESEngine();
		
		McOE mcoe = new McOEX();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, Blocksize/8);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingXSMSgMessage(){
		
		byte[] Message = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At v".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new AESEngine();
		
		McOE mcoe = new McOEX();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, Blocksize/8);
		
		for(int i = 0; i < Blocksize / 8; ++i){
			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingX128Bits(){
		
		byte[] Message = "testtest".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 128;
		
		byte[] Key = {-26, 120, 119, -15, -91, -68, 114, 109, 38, -70, 90, -5, -51, -117, -99, -77, -121, 122, -70, -44, -96, -48, 17, -21, -104, -24, -123, -26, 35, 75, -46, 27};
		
		BlockCipher Cipher = new AESEngine();
		
		McOE mcoe = new McOEX();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, McOE.resizeKey(Key, Blocksize, Blocksize));
		
		System.out.println(Arrays.toString(Ciphertext));
		
		byte[] Tag = new byte[Blocksize / Byte.SIZE / 2];
		Message = mcoe.ensureBlockSize(Message, Blocksize/8);
		
		System.arraycopy(Ciphertext, Ciphertext.length - Blocksize / Byte.SIZE, Tag, 0, Tag.length);
		
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/Byte.SIZE];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertArrayEquals(Message, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, McOE.resizeKey(Key, Blocksize, Blocksize)));
		
	}
	
	@Test
	public void testEnDeCryptionTagSplittingXCorruptedTag(){
		
		byte[] Message = "Hello".getBytes();
		byte[] Header = new byte[4];
		int Blocksize = 256;
		
		BlockCipher Cipher = new AESEngine();
		
		McOE mcoe = new McOEX();
		
		byte[] Ciphertext = mcoe.encryptAuthenticate(Header, Message, Blocksize, Cipher, Key);
		
		byte[] Tag = new byte[Blocksize / 8];
		Message = mcoe.ensureBlockSize(Message, Blocksize/8);
		
//		for(int i = 0; i < Blocksize / 8; ++i){
//			Tag[i] = Ciphertext[Ciphertext.length - ((Blocksize/8)-i)];
//		}
		byte[] CiphertextWithoutTag = new byte[Ciphertext.length - Blocksize/8];
		System.arraycopy(Ciphertext, 0, CiphertextWithoutTag, 0, CiphertextWithoutTag.length);
		
		assertEquals(true, mcoe.decryptAuthenticate(Header, CiphertextWithoutTag, Tag, Blocksize, Cipher, Key) == null);
		
	}
	
	@Test
	public void testIVCountZeroBits(){
		byte[] IV = new byte[8];
		
		//Test #1
		System.arraycopy(ByteBuffer.allocate(8).putLong(0xFFFFFF000FFFFFFFL).array(), 0, IV, 0, IV.length);
		assertEquals(12, McOE.countNumberOfZeroBitsInFlow(IV));
		
		//Test #2
		System.arraycopy(ByteBuffer.allocate(8).putLong(0xFFFFFC000FFFFFFFL).array(), 0, IV, 0, IV.length);
		assertEquals(14, McOE.countNumberOfZeroBitsInFlow(IV));
		
		//Test #3
		System.arraycopy(ByteBuffer.allocate(8).putLong(0xFFFFFFFFFFFFFFL).array(), 0, IV, 0, IV.length);
		assertEquals(8, McOE.countNumberOfZeroBitsInFlow(IV));
				
		//Test #4
		System.arraycopy(ByteBuffer.allocate(8).putLong(0xFFFFFFFFFFFFFF00L).array(), 0, IV, 0, IV.length);
		assertEquals(8, McOE.countNumberOfZeroBitsInFlow(IV));
		
		//Test #5
		System.arraycopy(ByteBuffer.allocate(8).putLong(0xFFF000DE0000003FL).array(), 0, IV, 0, IV.length);
		assertEquals(27, McOE.countNumberOfZeroBitsInFlow(IV));
		
		//Test #6
		System.arraycopy(ByteBuffer.allocate(8).putLong(0xFF0000FFFFFF00FFL).array(), 0, IV, 0, IV.length);
		assertEquals(16, McOE.countNumberOfZeroBitsInFlow(IV));
	}
}
