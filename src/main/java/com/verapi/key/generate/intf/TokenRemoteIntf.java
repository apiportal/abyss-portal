/**
 * 
 */
package com.verapi.key.generate.intf;

import java.io.UnsupportedEncodingException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

import com.verapi.key.model.AuthenticationInfo;
import com.verapi.key.model.TokenRequest;

/**
 * @author faik.saglar
 * @version 1.0
 */
public interface TokenRemoteIntf extends Remote {

	public AuthenticationInfo encodeToken(TokenRequest tokenRequest) throws UnsupportedEncodingException, NoSuchAlgorithmException, RemoteException;
	
	public AuthenticationInfo decodeAndValidateToken(String token, AuthenticationInfo authInfo) throws UnsupportedEncodingException, RemoteException;
	public AuthenticationInfo decodeAndValidateToken(AuthenticationInfo authInfo) throws UnsupportedEncodingException, RemoteException;

	
}
