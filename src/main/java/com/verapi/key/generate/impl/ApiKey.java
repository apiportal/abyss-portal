/**
 * 
 */
package com.verapi.key.generate.impl;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;


import java.util.Base64;
import java.util.UUID;

import com.verapi.key.generate.intf.ApiKeyRemoteIntf;
import com.verapi.key.util.ByteUtils;

/**
 * @author faik.saglar
 * @version 1.0
 */
public class ApiKey implements ApiKeyRemoteIntf {

	private Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder();
	private Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();
	
	public static final String UTF8_ENCODING = "UTF8";
	
	public ApiKey() {
		super();
	}
	
	String encodeBase64(String input) throws UnsupportedEncodingException {
		
		return base64UrlEncoder.encodeToString(input.getBytes(UTF8_ENCODING));
	}
	
	String encodeBase64(byte[] inputBytes) throws UnsupportedEncodingException {
		
		return base64UrlEncoder.encodeToString(inputBytes);	
	
	}

	byte[] decodeBase64(String input) throws UnsupportedEncodingException {
		//TODO IllegalArgumentException
		return base64UrlDecoder.decode(input);   //decodeToString(input.getBytes("UTF8"));
	}
	
	byte[] decodeBase64(byte[] inputBytes) {
		//TODO IllegalArgumentException		
		return base64UrlDecoder.decode(inputBytes); //encodeToString(inputBytes);	
	
	}
	
	/**
	 * Generate Base64 Encoded Api Key
	 * 
	 * @author faik.saglar
	 * 
	 */
/*	public String generateApiKey() throws RemoteException { 
		
		UUID uuid = UUID.randomUUID(); //TODO: is thread-safe?

        byte[] uuidByteArray = new byte[16]; 
        
        System.arraycopy(ByteUtils.longToBytes(uuid.getMostSignificantBits()), 0, uuidByteArray, 0, 8); //TODO: is thread-safe?
        System.arraycopy(ByteUtils.longToBytes(uuid.getLeastSignificantBits()), 0, uuidByteArray, 8, 8);
        
        return base64UrlEncoder.encodeToString(uuidByteArray).replaceAll("=", ""); //TODO: is thread-safe?
	}*/
	
	public String generateRandomKey() throws RemoteException { 
		
		UUID uuid = UUID.randomUUID(); //TODO: is thread-safe?

        byte[] uuidByteArray = new byte[16]; 
        
        System.arraycopy(ByteUtils.longToBytes(uuid.getMostSignificantBits()), 0, uuidByteArray, 0, 8); //TODO: is thread-safe?
        System.arraycopy(ByteUtils.longToBytes(uuid.getLeastSignificantBits()), 0, uuidByteArray, 8, 8);
        
        return base64UrlEncoder.encodeToString(uuidByteArray).replaceAll("=", ""); //TODO: is thread-safe?
	}
	
	public static void main(String[] args) throws RemoteException {
		
		ApiKey apiKey = new ApiKey();
		
        String uuidAllBase64Str = apiKey.generateRandomKey();
        
        System.out.println("generateAPiKey():" +  uuidAllBase64Str);
		
	}
}
