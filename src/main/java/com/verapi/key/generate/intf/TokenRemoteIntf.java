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

package com.verapi.key.generate.intf;

import com.verapi.key.model.AuthenticationInfo;
import com.verapi.key.model.TokenRequest;

import java.io.UnsupportedEncodingException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

/**
 * @author faik.saglar
 * @version 1.0
 */
public interface TokenRemoteIntf extends Remote {

    AuthenticationInfo encodeToken(TokenRequest tokenRequest) throws UnsupportedEncodingException, NoSuchAlgorithmException, RemoteException;

    AuthenticationInfo decodeAndValidateToken(String token, AuthenticationInfo authInfo) throws UnsupportedEncodingException, RemoteException;

    AuthenticationInfo decodeAndValidateToken(AuthenticationInfo authInfo) throws UnsupportedEncodingException, RemoteException;


}
