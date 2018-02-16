/**
 * Token Generation for User Activation & Forgot Password Scenarios
 */
package com.verapi.key.generate.impl;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
//import java.time.ZonedDateTime;
//import java.time.LocalDateTime;
import java.time.Instant;
//import org.joda.time.DateTime;
//import org.joda.time.format.DateTimeFormat;
//import org.joda.time.format.DateTimeFormatter;
//import org.joda.time.format.ISODateTimeFormat;

//import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.verapi.key.model.AuthenticationInfo;
import com.verapi.key.model.CryptoOperationResult;
import com.verapi.key.model.TokenRequest;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.VertxContextPRNG;

import com.verapi.key.generate.intf.TokenRemoteIntf;


/**
 * @author faik.saglar
 * @version 1.1
 */
public class Token implements TokenRemoteIntf{

	private static Logger logger = LoggerFactory.getLogger(Token.class);
	
	//http://www.oracle.com/technetwork/articles/java/jf14-date-time-2125367.html
	//http://www.joda.org/joda-time/	
	
	private static final String SPLITTER = ":";
	//private static final String DATETIME_FORMAT = "uuuuMMdd'T'HHmmss.SSSxx"; //Java ZonedDateTime
	//private static final String DATETIME_FORMAT = "yyyyMMdd'T'HHmmss.SSSZ"; //Joda Time
	//private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATETIME_FORMAT);
	//private static final DateTimeFormatter ISO_DATETIME_FORMATTER = ISODateTimeFormat.dateTime();
	private static final String HEADER = "xV24Rbgh43P";
	private static final String HEADER_WITH_SPLITTER = HEADER + SPLITTER;
	private static final int LEN_HEADER_WITH_SPLITTER = HEADER_WITH_SPLITTER.length();
	private static final int LEN_SPLITTED_ARRAY = 4;
	//private static final int INDEX_HEADER = 0;
	private static final int INDEX_NONCE = 1;
	private static final int INDEX_USER_DATA = 2;
	private static final int INDEX_EXPIRE_DATE = 3;
	
	
	public Token() {
		super();
	}

	/**
	 * @author faik.saglar
	 * @param date
	 * @param name
	 */
//	private void printDateTime(ZonedDateTime date, String name) {
//		System.out.println(name + ":" + date.format(DATETIME_FORMATTER));
//	}
//	
//	private void printDateTime(LocalDateTime date, String name) {
//		System.out.println(name + ":" + date.format(DATETIME_FORMATTER));
//	}
	
//	private void printDateTime(DateTime date, String name) {
//		System.out.println(name + ":" + date.toString(ISO_DATETIME_FORMATTER));
//	}

	private void printDateTime(Instant date, String name) {
		//System.out.println(name + ":" + date.toString());
		logger.info(name+":"+date.toString());
	}
	
	/*
	private long zonedDateTimeDifference(ZonedDateTime d1, ZonedDateTime d2, ChronoUnit unit){
	    return unit.between(d1, d2);
	}
	*/
	
	/**
	 * Is Token Expired
	 * @author faik.saglar
	 * @param receivedExpireDateStr
	 * @return boolean
	 */
	private boolean isExpired(String receivedExpireDateStr) {
		
		//ZonedDateTime receivedDate;
		//DateTime receivedDate;
		Instant receivedDate;
		try {
			//receivedDate = ZonedDateTime.parse(receivedExpireDateStr, DATETIME_FORMATTER);
			//receivedDate = DateTime.parse(receivedExpireDateStr, ISO_DATETIME_FORMATTER);
			receivedDate = Instant.parse(receivedExpireDateStr);
		} catch (DateTimeParseException e) {
			return true;
		}
		
		//ZonedDateTime currentDate = ZonedDateTime.now();
		//DateTime currentDate = DateTime.now();
		Instant currentDate = Instant.now();
		
		printDateTime(receivedDate, "receivedDate");
		printDateTime(currentDate,  "currentDate ");
		
		return receivedDate.isBefore(currentDate); //(zonedDateTimeDifference(currentDate, receivedDate, ChronoUnit.SECONDS) < 0);
	}
	
	
	/**
	 * Encode Token without Encryption Using Vertx PRNG and Hashing
	 * @author faik.saglar
	 * @return {@link AuthenticationInfo}
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	public AuthenticationInfo encodeToken(long secondsToExpire, String userData, Vertx vertx) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		
		//userData = username
		
		ApiKey apiKey = new ApiKey();
		
		Hash hash = new Hash();
		
		String nonceBase64 = VertxContextPRNG.current(vertx).nextString(32);
		
		//Calculate Expire Date
		//ZonedDateTime expireDate = ZonedDateTime.now();
		//LocalDateTime expireDate = LocalDateTime.now();
		//DateTime expireDate = DateTime.now(); 
		Instant expireDate = Instant.now();
		//expireDate = expireDate.plusSeconds(secondsToExpire);
		expireDate = expireDate.plusSeconds((int)secondsToExpire);
		
		//String expireDateStr = expireDate.format(DATETIME_FORMATTER);
		//String expireDateStr = expireDate.toString(ISO_DATETIME_FORMATTER);
		String expireDateStr = expireDate.toString();
		
		logger.info("expireDateStr:"+expireDateStr);
		
		String expireDateStrHash = hash.generateHash(expireDateStr);
		
		///////////////////////////////////////////////////////////////////
		// Concat Data 

		String input = HEADER_WITH_SPLITTER + nonceBase64 + SPLITTER + expireDateStrHash; //TODO  + Hash/Crc + HMAC / Signature ;
		
		///////////////////////////////////////////////////////////////////
		// Prepare Token for Export
		
		String token = apiKey.encodeBase64(input).replaceAll("=", "");
		
		AuthenticationInfo authenticationInfo = new AuthenticationInfo(token, nonceBase64, expireDate, userData);
		
		return authenticationInfo;
	}

	
	
	/**
	 * Encode Token
	 * @author faik.saglar
	 * @param tokenRequest
	 * @return {@link AuthenticationInfo}
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	public AuthenticationInfo encodeToken(TokenRequest tokenRequest) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		
		///////////////////////////////////////////////////////////////////
		// Input Handling
		
		String userData = tokenRequest.getUserData();
		long secondsToExpire = tokenRequest.getSecondsToExpire();
		
		ApiKey apiKey = new ApiKey();
		
		String userDataBase64 = apiKey.encodeBase64(userData.getBytes(ApiKey.UTF8_ENCODING));
		
		String nonceBase64;
		try {
			nonceBase64 = apiKey.generateRandomKey();
		} catch (RemoteException e) {
			e.printStackTrace();
			return new AuthenticationInfo(Token.class.getName() + ".encodeToken - Exception:" + e.getClass().getName() + " Message:" + e.getMessage());
		}
		
		//Calculate Expire Date
		//ZonedDateTime expireDate = ZonedDateTime.now();
		//LocalDateTime expireDate = LocalDateTime.now();
		//DateTime expireDate = DateTime.now(); 
		Instant expireDate = Instant.now();
		//expireDate = expireDate.plusSeconds(secondsToExpire);
		expireDate = expireDate.plusSeconds((int)secondsToExpire);
		
		//String expireDateStr = expireDate.format(DATETIME_FORMATTER);
		//String expireDateStr = expireDate.toString(ISO_DATETIME_FORMATTER);
		String expireDateStr = expireDate.toString();		
		
		///////////////////////////////////////////////////////////////////
		// Prepare Data for Enc

		String input = HEADER_WITH_SPLITTER + nonceBase64 + SPLITTER + userDataBase64 + SPLITTER + expireDateStr; //TODO  + Hash/Crc ;
		
		Cryptor aes = Cryptor.getInstance(); //TODO: Switch to A.E.
		
		byte[] inputBytes = input.getBytes(ApiKey.UTF8_ENCODING);
		
		//ByteUtils.printByteArray(inputBytes, "INPUT"); //TODO: Kaldır. For Debug
		
		CryptoOperationResult cryptoOperarionResult = aes.enc(inputBytes);
		if (!(cryptoOperarionResult.isValid())) {
			return new AuthenticationInfo(cryptoOperarionResult.getResultText());
		}
		byte[] encBytes = cryptoOperarionResult.getOutputBytes();
		
		///////////////////////////////////////////////////////////////////
		// Prepare Cipher Text for Export
		
		String token = apiKey.encodeBase64(encBytes).replaceAll("=", "");
		
		AuthenticationInfo authenticationInfo = new AuthenticationInfo(token, nonceBase64, expireDate, userData);
		
		return authenticationInfo;
	}
	
	/**
	 * Decode and Validate Token
	 * @author faik.saglar
	 * @param token
	 * @param authInfo
	 * @return {@link AuthenticationInfo}
	 * @throws UnsupportedEncodingException
	 */
	public AuthenticationInfo decodeAndValidateToken(String token, AuthenticationInfo authInfo) throws UnsupportedEncodingException  {

		///////////////////////////////////////////////////////////////////
		//Check authInfo
		if (authInfo == null) {
			return new AuthenticationInfo("Null AuthenticationInfo");
		}
		
		// Preset Result to FALSE
		authInfo.setValid(false);
		
		//Check token
		if (token == null || token.isEmpty()) {
			authInfo.setResultText("Received Token is null or empty");
			return authInfo;
		}

		
		///////////////////////////////////////////////////////////////////
		// Decode Token for Dec
		ApiKey apiKey = new ApiKey();
		
		byte[] decodedCipherBytes = apiKey.decodeBase64(token);
		//ByteUtils.printByteArray(decodedCipherBytes, "decodedCipherBytes"); //TODO: Kaldır. For Debug
		
		
		///////////////////////////////////////////////////////////////////
		// Decrypt & Convert to String

		Cryptor aes = Cryptor.getInstance();
		CryptoOperationResult cryptoOperarionResult = aes.dec(decodedCipherBytes);
		if (!(cryptoOperarionResult.isValid())) {
			authInfo.setResultText(cryptoOperarionResult.getResultText());
			return authInfo;
		}
		byte[] plainBytes = cryptoOperarionResult.getOutputBytes();
		//ByteUtils.printByteArray(plainBytes, "PLAIN"); //TODO: Kaldır.
		
		String plainStr = new String(plainBytes, ApiKey.UTF8_ENCODING);
		
		
		///////////////////////////////////////////////////////////////////
		// Validate Token String
		///////////////////////////////////////////////////////////////////
		
		//Check Header
		if (!(HEADER_WITH_SPLITTER.equals(plainStr.substring(0, LEN_HEADER_WITH_SPLITTER)))) {
			authInfo.setResultText("HEADER of Received Token is incorrect");
			return authInfo;
		}

		//TODO Authenticated
		
		//TODO Check Integrity
		
		//Split & Check
		String[] strArray = plainStr.split(SPLITTER);
		if (strArray.length != LEN_SPLITTED_ARRAY) {
			authInfo.setResultText("Received Token is broken");
			return authInfo;
		}

		//TODO Kaldır. For Debug
		//for (int i = 0; i < strArray.length; i++) {
		//	System.out.println(strArray[i]);
		//}

		
		//Check nonce
		if (!(authInfo.getNonce().equals(strArray[INDEX_NONCE]))) {
			authInfo.setResultText("NONCE in Received Token does not match");
			return authInfo;
		}
		
		//Check UserData
		String encodedUserData = apiKey.encodeBase64(authInfo.getUserData());
		if (!(encodedUserData.equals(strArray[INDEX_USER_DATA]))) {
			authInfo.setResultText("USER DATA in Received Token does not match");
			return authInfo;
		}
		
		//Check Expiry
		if (isExpired(strArray[INDEX_EXPIRE_DATE])) {
			authInfo.setResultText("Received Token has expired");
			return authInfo;
		}
		
		authInfo.setResultText("Received Token is VALID");
		authInfo.setValid(true);
		
		return authInfo;
	}
	
	public AuthenticationInfo decodeAndValidateToken(AuthenticationInfo authInfo) throws UnsupportedEncodingException  {

		///////////////////////////////////////////////////////////////////
		//Check authInfo
		if (authInfo == null) {
			return new AuthenticationInfo("Null AuthenticationInfo");
		} else
			return decodeAndValidateToken(authInfo.getToken(), authInfo);
		
	}
		
	/**
	 * @param args
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {

		Token token = new Token();
		
		AuthenticationInfo authInfo = token.encodeToken(new TokenRequest("userData123@xyz", 60*10));
		
		System.out.println(authInfo.getToken());
		System.out.println("----------------------------------");
		
		AuthenticationInfo authResult = token.decodeAndValidateToken(authInfo.getToken(), authInfo);
		System.out.println("DECODED OUTPUT:"+authResult.getResultText());
		System.out.println("----------------------------------");
		
		
		String wrongToken = authInfo.getToken().replaceAll("a", "x").replaceAll("e", "z");
		authResult = token.decodeAndValidateToken(wrongToken, authInfo);
		System.out.println("DECODED OUTPUT:"+authResult.getResultText());
		System.out.println("----------------------------------");
		
		
	}

}
