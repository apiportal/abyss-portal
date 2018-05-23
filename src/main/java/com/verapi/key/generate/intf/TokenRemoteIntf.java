/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Ismet Faik SAGLAR <faik.saglar@verapi.com>, 12 2017
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

	AuthenticationInfo encodeToken(TokenRequest tokenRequest) throws UnsupportedEncodingException, NoSuchAlgorithmException, RemoteException;
	
	AuthenticationInfo decodeAndValidateToken(String token, AuthenticationInfo authInfo) throws UnsupportedEncodingException, RemoteException;
	AuthenticationInfo decodeAndValidateToken(AuthenticationInfo authInfo) throws UnsupportedEncodingException, RemoteException;

	
}
