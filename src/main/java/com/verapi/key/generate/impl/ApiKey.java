/*
 * Copyright 2019 Verapi Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verapi.key.generate.impl;

import java.io.UnsupportedEncodingException;


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
	
	static final String UTF8_ENCODING = "UTF8";
	
	ApiKey() {
		super();
	}
	
	String encodeBase64(String input) throws UnsupportedEncodingException {
		
		return base64UrlEncoder.encodeToString(input.getBytes(UTF8_ENCODING));
	}
	
	String encodeBase64(byte[] inputBytes) {
		
		return base64UrlEncoder.encodeToString(inputBytes);	
	
	}

	byte[] decodeBase64(String input) {
		//TODO IllegalArgumentException
		return base64UrlDecoder.decode(input);   //decodeToString(input.getBytes("UTF8"));
	}
	
/*	byte[] decodeBase64(byte[] inputBytes) {
		//TODO IllegalArgumentException		
		return base64UrlDecoder.decode(inputBytes); //encodeToString(inputBytes);	
	
	}*/
	
	/**
	 * Generate Base64 Encoded Api Key
	 * 
	 * @author faik.saglar
	 * @return base64 url encoded random api key
	 * 
	 */
/*	public String generateApiKey() throws RemoteException { 
		
		UUID uuid = UUID.randomUUID(); //TODO: is thread-safe?

        byte[] uuidByteArray = new byte[16]; 
        
        System.arraycopy(ByteUtils.longToBytes(uuid.getMostSignificantBits()), 0, uuidByteArray, 0, 8); //TODO: is thread-safe?
        System.arraycopy(ByteUtils.longToBytes(uuid.getLeastSignificantBits()), 0, uuidByteArray, 8, 8);
        
        return base64UrlEncoder.encodeToString(uuidByteArray).replaceAll("=", ""); //TODO: is thread-safe?
	}*/
	
	public String generateRandomKey() {
		
		UUID uuid = UUID.randomUUID(); //TODO: is thread-safe?

        byte[] uuidByteArray = new byte[16]; 
        
        System.arraycopy(ByteUtils.longToBytes(uuid.getMostSignificantBits()), 0, uuidByteArray, 0, 8); //TODO: is thread-safe?
        System.arraycopy(ByteUtils.longToBytes(uuid.getLeastSignificantBits()), 0, uuidByteArray, 8, 8);
        
        return base64UrlEncoder.encodeToString(uuidByteArray).replaceAll("=", ""); //TODO: is thread-safe?
	}
	
	public static void main(String[] args) {
		
		ApiKey apiKey = new ApiKey();
		
        String uuidAllBase64Str = apiKey.generateRandomKey();
        
        System.out.println("generateAPiKey():" +  uuidAllBase64Str);
		
	}
}
