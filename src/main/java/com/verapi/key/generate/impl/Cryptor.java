/**
 * 
 */
package com.verapi.key.generate.impl;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.verapi.key.model.CryptoOperationResult;

/**
 * Encrypt Decrypt Library
 * @author faik.saglar
 * @version 1.0
 */

public class Cryptor {

	private	static final byte[] IV = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
	
	private	static SecretKey aesEncKey = null;
	
	//private	static SecretKey aesMacKey = null;
	
	private static final String GENERATE_ALGORITHM = "AES";
	private static final String CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding";
	//private static final String KEY_FILE = "secret.key";
	private static final int KEY_SIZE = 128; //bit
	//private static final int BLOCK_SIZE = 32; //bytes
	
	private static final IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
	
	//create the singleton object of AES
	private static Cryptor instance = new Cryptor();
	
	private Cryptor() {
		 generateAesKey();
	}
	
	static Cryptor getInstance() {
		return instance;
	}
	
	/**
	 * Generate Aes Key
	 * @author faik.saglar
	 * @param
	 * @return
	 * 
	 */
	private void generateAesKey() { //TODO: input key type: enc / mac
		KeyGenerator keyGenerator;
		
		try {
			keyGenerator = KeyGenerator.getInstance(GENERATE_ALGORITHM);
			keyGenerator.init(KEY_SIZE);
			
			aesEncKey = keyGenerator.generateKey();
			//TODO: generate KCV
			//TODO: use keyblock mechanism
			//TODO: store key / load key
			//ByteUtils.printByteArray(aesEncKey.getEncoded(), "AES ENC KEY"); //TODO: Kaldır
			
		} catch (NoSuchAlgorithmException e1) {
			// TODO: handle exception
			// TODO: log exception
			e1.printStackTrace();
			//throw e1;
		}
	}
	
	/**
	 * Encrypt Input Bytes
	 * @author faik.saglar
	 * @param plainInput
	 * @return {@link CryptoOperationResult}
	 */
	CryptoOperationResult enc (byte[] plainInput) {
		return crypt(plainInput, Cipher.ENCRYPT_MODE);
	}

	/**
	 * Decrypt Input Bytes
	 * @author faik.saglar
	 * @param cipherInput
	 * @return {@link CryptoOperationResult}
	 */
	CryptoOperationResult dec (byte[] cipherInput) {
		return crypt(cipherInput, Cipher.DECRYPT_MODE);
	}
	
	/**
	 * Common Encrypt Decrypt Method
	 * @author faik.saglar
	 * @param inputBytes
	 * @param operationMode (Encrypt / Decrypt)
	 * @return {@link CryptoOperationResult}
	 */
	private CryptoOperationResult crypt (byte[] inputBytes, int operationMode) {
		
		Cipher aesCipher = null;
		//String cipherText = "";
		byte[] outputBytes = null;
		
		try {
			aesCipher = Cipher.getInstance(CRYPT_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new CryptoOperationResult(outputBytes, false, "NoSuchAlgorithmException", e);
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new CryptoOperationResult(outputBytes, false, "NoSuchPaddingException", e);
		}
		try {
			aesCipher.init(operationMode, aesEncKey, ivParameterSpec);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new CryptoOperationResult(outputBytes, false, "InvalidKeyException", e);
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new CryptoOperationResult(outputBytes, false, "InvalidAlgorithmParameterException", e);
		}
		
		try {
			outputBytes = aesCipher.doFinal(inputBytes);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new CryptoOperationResult(outputBytes, false, "IllegalBlockSizeException", e);
			
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new CryptoOperationResult(outputBytes, false, "BadPaddingException", e);
		}
		
		//System.out.println("Len:"+outputBytes.length); //TODO Kaldır
		
		
		return new CryptoOperationResult(outputBytes, true, "SUCCESSFULL", null);
		
	}
	
}
