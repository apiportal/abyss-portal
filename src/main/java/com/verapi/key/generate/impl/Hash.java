/**
 * 
 */
package com.verapi.key.generate.impl;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.verapi.key.generate.intf.HashRemoteIntf;


/**
 * @author faik.saglar
 * @version 1.1
 */
public class Hash implements HashRemoteIntf{

	private static Logger logger = LoggerFactory.getLogger(Hash.class);
	
	private static final int ITERATIONS = 1000;
	private static final int KEY_LENGTH = 256; // bits
	private static final int SALT_LENGTH = 32; // bytes
	
	private static final Random SECURE_RANDOM = new SecureRandom();
	
	private static final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder();
	private static final Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();


	public Hash() {
		super();
	}

	/**
	 * @author faik.saglar
	 * @param inputData
	 * @return hash {@link String}
	 */
	public String generateHash(String inputData) {
		
		String digestText = null;
		
		try {
			
			MessageDigest md = MessageDigest.getInstance("SHA-256"); //TODO: Singleton ya da Pool yapılmalı mı? 
			
			byte[] hashBytes = md.digest((inputData).getBytes("UTF-8"));
			
			digestText = base64UrlEncoder.encodeToString(hashBytes); //TODO: is thread-safe?
		
		} catch (NoSuchAlgorithmException e) {
			logger.error("generateHash NoSuchAlgorithmException: " + e.getLocalizedMessage());
		} catch (UnsupportedEncodingException e) {
			logger.error("generateHash UnsupportedEncodingException: " + e.getLocalizedMessage());
		}
		
		return digestText;
		
	}	
	
	/*
	 * http://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
	 * https://www.owasp.org/index.php/Hashing_Java
	 * https://www.owasp.org/index.php/Password_Storage_Cheat_Sheet
	 * https://docs.oracle.com/javase/7/docs/api/java/security/MessageDigest.html
	 * https://adambard.com/blog/3-wrong-ways-to-store-a-password/
	 * http://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html
	 * http://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
	 * 	http://stackoverflow.com/questions/2860943/how-can-i-hash-a-password-in-java
	 * 	http://security.stackexchange.com/questions/4781/do-any-security-experts-recommend-bcrypt-for-password-storage/6415#6415
	 * 		!!!!to avoid DDOS check the username before checking the password. Then if there have been multiple failed login attempts in a row for that user (say, 10 of them) reject the password without even checking it unless the account has "cooled off" for a few minutes. This way an account can be DDOS'd but not the whole server, and it makes even horribly weak passwords impossible to brute force without an offline attack.
	 */
	
	/**
	 * @author faik.saglar
	 * @return salt byte[]
	 */
	private byte[] generateSalt() {
		
		byte[] saltBytes = new byte[SALT_LENGTH];

		SECURE_RANDOM.nextBytes(saltBytes);

	    //ByteUtils.printByteArray(saltBytes, "saltBytes"); //TEST
	    
		return saltBytes;
	}

	
	/**
	 * @author faik.saglar
	 * @param password {@link String}
	 * @return salted Password Hash {@link String}
	 */
	public String generateSaltedPasswordHash(String password) {
		
		return generateSaltedPasswordHash(password, generateSalt());
	}
	
	/**
	 * @author faik.saglar
	 * @param password {@link String}
	 * @param saltBytes byte[]
	 * @return salted Password Hash {@link String}
	 */
	private String generateSaltedPasswordHash(String password, byte[] saltBytes) {
		
		char[] passwordChars = password.toCharArray();
		
		byte[] hashedPassword = null;
		
		String saltPlusHashedPasswordString;
		
		PBEKeySpec spec = null;
		SecretKey key = null;

		try {
		    spec = new PBEKeySpec(
		        passwordChars,
		        saltBytes,
		        ITERATIONS,
		        KEY_LENGTH
		    );
		    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		    key = skf.generateSecret(spec);
		    hashedPassword = key.getEncoded();
		    
		    byte[] saltPlusHashedPassword = new byte[SALT_LENGTH+hashedPassword.length];
		    System.arraycopy(saltBytes, 0, saltPlusHashedPassword , 0, SALT_LENGTH);
		    System.arraycopy(hashedPassword, 0, saltPlusHashedPassword, SALT_LENGTH, hashedPassword.length);
		    
		    saltPlusHashedPasswordString = base64UrlEncoder.encodeToString(saltPlusHashedPassword).replaceAll("=", "");
		    
		    //System.out.println(saltPlusHashedPasswordString); //TEST
		    //ByteUtils.printByteArray(saltPlusHashedPassword, "saltPlusHashedPassword"); //TEST
		    
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally { //Sensitive data should be cleared after you have used it (set the array elements to zero).
			spec.clearPassword();
//			try {
//				key.destroy();  //javax.security.auth.DestroyFailedException at javax.security.auth.Destroyable.destroy(Unknown Source)
//			} catch (DestroyFailedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}

		return saltPlusHashedPasswordString;
		
	}

	/**
	 * @author faik.saglar
	 * @param storedSaltedPasswordHash {@link String}
	 * @return salt byte[]
	 */
	public byte[] extractSalt(String storedSaltedPasswordHash) {
		
		byte[] decodedBytes = base64UrlDecoder.decode(storedSaltedPasswordHash);
		
		//ByteUtils.printByteArray(decodedBytes, "decodedBytes"); //TEST
		
		byte[] storedSalt = new byte[SALT_LENGTH];
		System.arraycopy(decodedBytes, 0, storedSalt, 0, SALT_LENGTH);
		
		//ByteUtils.printByteArray(storedSalt, "storedSalt"); //TEST
		
		return storedSalt;
	}
	
	/**
	 * @author faik.saglar
	 * @param enteredPassword {@link String}
	 * @param storedSaltedPasswordHash {@link String}
	 * @return is Correct boolean
	 */
	public boolean isSaltedPasswordHashCorrect(String enteredPassword, String storedSaltedPasswordHash) {
		
		if (enteredPassword == null | enteredPassword.isEmpty()) {
			return false;
		}
		
		String enteredPasswordHash = generateSaltedPasswordHash(enteredPassword, extractSalt(storedSaltedPasswordHash)).replaceAll("=", "");
		enteredPassword = null; //Delete password
		
		System.out.println("Check equality:*********************");
		System.out.println(storedSaltedPasswordHash);
		System.out.println(enteredPasswordHash);
		
		return storedSaltedPasswordHash.equals(enteredPasswordHash);
		
	}
	
	
	/**
	 * Generates a random password of a given length, using letters and digits.
	 * Reference: http://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
	 *
	 * @param length
	 *            the length of the password
	 *
	 * @return a random password
	 */
	public String generateRandomPassword(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int c = SECURE_RANDOM.nextInt(62);
			if (c <= 9) {
				sb.append(String.valueOf(c));
			} else if (c < 36) {
				sb.append((char) ('a' + c - 10));
			} else {
				sb.append((char) ('A' + c - 36));
			}
		}
		return sb.toString();
	}
	
	/**
	 * @author faik.saglar
	 * @param args
	 * @throws RemoteException 
	 */
	public static void main(String[] args) throws RemoteException {

		Hash hash = new Hash();
		
		String hashStr = hash.generateSaltedPasswordHash("password");
		
		System.out.println(hashStr);
		
		System.out.println(hash.isSaltedPasswordHashCorrect("enteredPassword", hashStr));
		
		System.out.println(hash.isSaltedPasswordHashCorrect("password", hashStr));
		
		System.out.println(hash.generateRandomPassword(8));
		
	}

}