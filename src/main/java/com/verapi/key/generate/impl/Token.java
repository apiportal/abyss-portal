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

import com.verapi.key.generate.intf.TokenRemoteIntf;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.key.model.CryptoOperationResult;
import com.verapi.key.model.TokenRequest;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.VertxContextPRNG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;

//import java.rmi.RemoteException;
//import java.security.NoSuchAlgorithmException;
//import java.time.ZonedDateTime;
//import java.time.LocalDateTime;
//import org.joda.time.DateTime;
//import org.joda.time.format.DateTimeFormat;
//import org.joda.time.format.DateTimeFormatter;
//import org.joda.time.format.ISODateTimeFormat;
//import java.time.format.DateTimeFormatter;


/**
 * Token Generation for User Activation and Forgot Password Scenarios
 *
 * @author faik.saglar
 * @version 1.1
 */
public class Token implements TokenRemoteIntf {

    private static final String PRECHECKS_OK = "prechecks.ok";
    private static final String SPLITTER = ":";

    //http://www.oracle.com/technetwork/articles/java/jf14-date-time-2125367.html
    //http://www.joda.org/joda-time/
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Token.class);


    public Token() {
        super();
    }

    /**
     * Unit Test
     *
     * @param args ars
     * @throws UnsupportedEncodingException if base64 encoding has errors
     */
    public static void main(String[] args) {

        final String SEPARATOR = "----------------------------------";
        Token token = new Token();


        AuthenticationInfo authInfo = null;
        try {
            authInfo = token.encodeToken(new TokenRequest("userData123@xyz", 60 * 10));


            LOGGER.trace("{}", authInfo.getToken());
            LOGGER.trace("{}", SEPARATOR);

            AuthenticationInfo authResult = token.decodeAndValidateToken(authInfo.getToken(), authInfo);
            LOGGER.trace("DECODED OUTPUT: {}", authResult.getResultText());
            LOGGER.trace("{}", SEPARATOR);


            String wrongToken = authInfo.getToken().replaceAll("a", "x").replaceAll("e", "z");
            authResult = token.decodeAndValidateToken(wrongToken, authInfo);
            LOGGER.trace("DECODED OUTPUT: {}", authResult.getResultText());
            LOGGER.trace("{}", SEPARATOR);


            //----------------------- USAGE EXAMPLE
            AuthenticationInfo authInfo2 = token.generateToken(60 * 10, "userData123@xyz", Vertx.vertx());

            LOGGER.trace("{}", authInfo2.getToken());
            LOGGER.trace("{}", SEPARATOR);

            AuthenticationInfo authResult2 = token.validateToken(authInfo2.getToken(), authInfo2);
            LOGGER.trace("DECODED OUTPUT2: {}", authResult2.getResultText());
            LOGGER.trace("{}", SEPARATOR);

            String wrongToken2 = authInfo2.getToken().replaceAll("a", "x").replaceAll("e", "z");
            authResult2 = token.validateToken(wrongToken2, authInfo2);
            LOGGER.trace("DECODED OUTPUT2: {}", authResult2.getResultText());
            LOGGER.trace("{}", SEPARATOR);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("main {}", e.getLocalizedMessage());
        }

    }

    /*
    private long zonedDateTimeDifference(ZonedDateTime d1, ZonedDateTime d2, ChronoUnit unit){
        return unit.between(d1, d2);
    }
    */

    /**
     * Print Date Time
     *
     * @param date date to be printed as {@link Instant} type
     * @param name name of date variable to be used as label while printing
     * @author faik.saglar
     */
//private void printDateTime(ZonedDateTime date, String name) {
//System.out.println(name + ":" + date.format(DATETIME_FORMATTER));
//}
//
//private void printDateTime(LocalDateTime date, String name) {
//System.out.println(name + ":" + date.format(DATETIME_FORMATTER));
//}

//private void printDateTime(DateTime date, String name) {
//System.out.println(name + ":" + date.toString(ISO_DATETIME_FORMATTER));
//}
    private void printDateTime(Instant date, String name) {
        //System.out.println(name + ":" + date.toString());
        LOGGER.info(name + ":" + date.toString());
    }

    /**
     * Is Token Expired compared current date time (now)
     *
     * @param receivedExpireDateStr received expire date in {@link String}
     * @return boolean true if expired, false otherwise
     * @author faik.saglar
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
        printDateTime(currentDate, "currentDate ");

        return receivedDate.isBefore(currentDate); //(zonedDateTimeDifference(currentDate, receivedDate, ChronoUnit.SECONDS) < 0);
    }

    /**
     * Is Token Expired compared current date time (now)
     *
     * @param expireDate received expire date in {@link Instant}
     * @return boolean true if expired, false otherwise
     * @author faik.saglar
     */
    private boolean isExpired(Instant expireDate) {

        Instant currentDate = Instant.now();

        printDateTime(expireDate, "expireDate");
        printDateTime(currentDate, "currentDate ");

        return expireDate.isBefore(currentDate);
    }

    /**
     * Generate Token without Encryption Using Vertx PRNG and Hashing
     *
     * @param secondsToExpire seconds to expire
     * @param userData        user data
     * @param vertx           vertx
     * @return {@link AuthenticationInfo}
     * @throws UnsupportedEncodingException if base64 encoding has errors
     * @author faik.saglar
     */
    public AuthenticationInfo generateToken(long secondsToExpire, String userData, Vertx vertx) throws UnsupportedEncodingException {

        ApiKey apiKey = new ApiKey();

        Hash hash = new Hash();

        String userDataBase64 = apiKey.encodeBase64(userData.getBytes(StandardCharsets.UTF_8));
        String userDataBase64Hash = hash.generateHash(userDataBase64);

        String nonceBase64 = VertxContextPRNG.current(vertx).nextString(32);

        //Calculate Expire Date
        //ZonedDateTime expireDate = ZonedDateTime.now();
        //LocalDateTime expireDate = LocalDateTime.now();
        //DateTime expireDate = DateTime.now();
        Instant expireDate = Instant.now();
        //expireDate = expireDate.plusSeconds(secondsToExpire);
        expireDate = expireDate.plusSeconds((int) secondsToExpire);

        //String expireDateStr = expireDate.format(DATETIME_FORMATTER);
        //String expireDateStr = expireDate.toString(ISO_DATETIME_FORMATTER);
        String expireDateStr = expireDate.toString();

        LOGGER.info("expireDateStr: {}", expireDateStr);

        String expireDateStrHash = hash.generateHash(expireDateStr);

        ///////////////////////////////////////////////////////////////////
        // Concat Data

        String input = HEADER_WITH_SPLITTER + nonceBase64 + SPLITTER + userDataBase64Hash + SPLITTER + expireDateStrHash; //TODO  + Hash/Crc + HMAC / Signature ;

        ///////////////////////////////////////////////////////////////////
        // Prepare Token for Export

        String token = apiKey.encodeBase64(input).replaceAll("=", "");

        return new AuthenticationInfo(token, nonceBase64, expireDate, userData);
    }

    private AuthenticationInfo doPreChecksBeforeValidateToken(String token, AuthenticationInfo authInfo) {

        ///////////////////////////////////////////////////////////////////
        //Check authInfo
        if (authInfo == null) {
            return new AuthenticationInfo("Null AuthenticationInfo");
        }

        // Preset Result to FALSE
        authInfo.setValid(false);

        //Check token
        if (token == null) {
            return new AuthenticationInfo("Received Token is null");
        }

        if (token.isEmpty()) {
            return new AuthenticationInfo("Received Token is empty");
        }

        authInfo.setResultText(PRECHECKS_OK);

        return authInfo;

    }

    /**
     * Validate Token
     *
     * @param receivedToken  received Token to be validated
     * @param storedAuthInfo stored Authentication Information
     * @return {@link AuthenticationInfo}
     * @throws UnsupportedEncodingException if base64 encoding has errors
     * @author faik.saglar
     */
    public AuthenticationInfo validateToken(String receivedToken, AuthenticationInfo storedAuthInfo) throws UnsupportedEncodingException {

        storedAuthInfo = doPreChecksBeforeValidateToken(receivedToken, storedAuthInfo);
        if (!(storedAuthInfo.getResultText().equals(PRECHECKS_OK))) {
            return storedAuthInfo;
        }

        ///////////////////////////////////////////////////////////////////
        // Decode Token for Dec
        ApiKey apiKey = new ApiKey();

        byte[] plainBytes = apiKey.decodeBase64(receivedToken);
        //ByteUtils.printByteArray(plainBytes, "plainBytes"); //TODO: Kaldır. For Debug

        String plainStr = new String(plainBytes, StandardCharsets.UTF_8);


        ///////////////////////////////////////////////////////////////////
        // Validate Token String
        ///////////////////////////////////////////////////////////////////

        //Check Header
        if (!(HEADER_WITH_SPLITTER.equals(plainStr.substring(0, LEN_HEADER_WITH_SPLITTER)))) {
            storedAuthInfo.setResultText("HEADER of Received Token is incorrect");
            return storedAuthInfo;
        }

        //TODO Authenticated

        //TODO Check Integrity

        //Split & Check
        String[] strArray = plainStr.split(SPLITTER);
        if (strArray.length != LEN_SPLITTED_ARRAY) {
            storedAuthInfo.setResultText("Received Token is broken");
            return storedAuthInfo;
        }

        // For Debug
        //for (int i = 0; i < strArray.length; i++) {
        //System.out.println(strArray[i]);
        //}


        //Check nonce
        if (!(storedAuthInfo.getNonce().equals(strArray[INDEX_NONCE]))) {
            storedAuthInfo.setResultText("NONCE in Received Token does not match");
            return storedAuthInfo;
        }

        Hash hash = new Hash();

        //Check UserData Match
        String userDataBase64 = apiKey.encodeBase64(storedAuthInfo.getUserData().getBytes(StandardCharsets.UTF_8));
        String userDataBase64Hash = hash.generateHash(userDataBase64);
        if (!(userDataBase64Hash.equals(strArray[INDEX_USER_DATA]))) {
            storedAuthInfo.setResultText("USER DATA in Received Token does not match");
            return storedAuthInfo;
        }

        //Check Expire Date Match
        String expireDateStrHash = hash.generateHash(storedAuthInfo.getExpireDate().toString());
        if (!(expireDateStrHash.equals(strArray[INDEX_EXPIRE_DATE]))) {
            storedAuthInfo.setResultText("EXPIRE DATE in Received Token does not match");
            return storedAuthInfo;
        }

        //Check Expiry
        if (isExpired(storedAuthInfo.getExpireDate())) {
            storedAuthInfo.setResultText("Token has expired");
            return storedAuthInfo;
        }

        storedAuthInfo.setResultText("Received Token is VALID");
        storedAuthInfo.setValid(true);

        return storedAuthInfo;
    }

    /**
     * Encode Token
     *
     * @param tokenRequest {@link TokenRequest}
     * @return {@link AuthenticationInfo}
     * @throws UnsupportedEncodingException if base64 encoding has errors
     * @author faik.saglar
     */
    public AuthenticationInfo encodeToken(TokenRequest tokenRequest) throws UnsupportedEncodingException {

        ///////////////////////////////////////////////////////////////////
        // Input Handling

        String userData = tokenRequest.getUserData();
        long secondsToExpire = tokenRequest.getSecondsToExpire();

        ApiKey apiKey = new ApiKey();

        String userDataBase64 = apiKey.encodeBase64(userData.getBytes(StandardCharsets.UTF_8));

        String nonceBase64;
        nonceBase64 = apiKey.generateRandomKey();

        //Calculate Expire Date
        //ZonedDateTime expireDate = ZonedDateTime.now();
        //LocalDateTime expireDate = LocalDateTime.now();
        //DateTime expireDate = DateTime.now();
        Instant expireDate = Instant.now();
        //expireDate = expireDate.plusSeconds(secondsToExpire);
        expireDate = expireDate.plusSeconds((int) secondsToExpire);

        //String expireDateStr = expireDate.format(DATETIME_FORMATTER);
        //String expireDateStr = expireDate.toString(ISO_DATETIME_FORMATTER);
        String expireDateStr = expireDate.toString();

        ///////////////////////////////////////////////////////////////////
        // Prepare Data for Enc

        String input = HEADER_WITH_SPLITTER + nonceBase64 + SPLITTER + userDataBase64 + SPLITTER + expireDateStr; //TODO  + Hash/Crc ;

        Cryptor aes = Cryptor.getInstance(); //TODO: Switch to A.E.

        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        //ByteUtils.printByteArray(inputBytes, "INPUT"); //TODO: Kaldır. For Debug

        CryptoOperationResult cryptoOperarionResult = aes.enc(inputBytes);
        if (!(cryptoOperarionResult.isValid())) {
            return new AuthenticationInfo(cryptoOperarionResult.getResultText());
        }
        byte[] encBytes = cryptoOperarionResult.getOutputBytes();

        ///////////////////////////////////////////////////////////////////
        // Prepare Cipher Text for Export

        String token = apiKey.encodeBase64(encBytes).replaceAll("=", "");

        return new AuthenticationInfo(token, nonceBase64, expireDate, userData);
    }

    /**
     * Decode and Validate Token
     *
     * @param token    token to be validated
     * @param authInfo stored {@link AuthenticationInfo}
     * @return {@link AuthenticationInfo}
     * @throws UnsupportedEncodingException if base64 encoding has errors
     * @author faik.saglar
     */
    public AuthenticationInfo decodeAndValidateToken(String token, AuthenticationInfo authInfo) throws UnsupportedEncodingException {

        authInfo = doPreChecksBeforeValidateToken(token, authInfo);
        if (!(authInfo.getResultText().equals(PRECHECKS_OK))) {
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

        String plainStr = new String(plainBytes, StandardCharsets.UTF_8);


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
        //System.out.println(strArray[i]);
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

    public AuthenticationInfo decodeAndValidateToken(AuthenticationInfo authInfo) throws UnsupportedEncodingException {

        ///////////////////////////////////////////////////////////////////
        //Check authInfo
        if (authInfo == null) {
            return new AuthenticationInfo("Null AuthenticationInfo");
        } else {
            return decodeAndValidateToken(authInfo.getToken(), authInfo);
        }
    }
}
