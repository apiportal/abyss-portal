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
package com.verapi.key.model;

import java.io.Serializable;
//import java.time.LocalDateTime;
//import java.time.ZonedDateTime;
//import org.joda.time.DateTime;
import java.time.Instant;


/**
 * @author faik.saglar
 * @version 1.0
 *
 */
public class AuthenticationInfo implements Serializable{
	
	private static final long serialVersionUID = -5705493131142227985L;
	
	private String token;
	private String nonce;
	//private ZonedDateTime expireDate;
	//private DateTime expireDate;
	private Instant expireDate;
	private String userData;
	
	private boolean isValid;
	private String resultText;
	
	/**
	 * @author faik.saglar
	 * @param token token
	 * @param nonce nonce
	 * @param expireDate expire date
	 * @param userData user data
	 */
	//public AuthenticationInfo(String token, String nonce, ZonedDateTime expireDate, String userData) {
	//public AuthenticationInfo(String token, String nonce, DateTime expireDate, String userData) {
	public AuthenticationInfo(String token, String nonce, Instant expireDate, String userData) {
		this.token = token;
		this.nonce = nonce;
		this.expireDate = expireDate;
		this.userData = userData;
		this.isValid = false;
		this.resultText = "Token Generation SUCCESSFUL";
	}

	/**
	 * @author faik.saglar
	 * @param token token
	 * @param nonce nonce
	 * @param expireDate expire date
	 * @param userData user data
	 * @param isValid is valid
	 * @param resultText result text
	 */
	//public AuthenticationInfo(String token, String nonce, ZonedDateTime expireDate, String userData,
	//public AuthenticationInfo(String token, String nonce, DateTime expireDate, String userData,
	public AuthenticationInfo(String token, String nonce, Instant expireDate, String userData,
			boolean isValid, String resultText) {
		this.token = token;
		this.nonce = nonce;
		this.expireDate = expireDate;
		this.userData = userData;
		this.isValid = isValid;
		this.resultText = resultText;
	}
	
	/**
	 * @author faik.saglar
	 * @param resultText result text
	 */
	public AuthenticationInfo(String resultText) {
		this.token = "";
		this.nonce = "";
		this.expireDate = null;
		this.userData = "";
		
		this.isValid = false;
		this.resultText = resultText;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @return the nonce
	 */
	public String getNonce() {
		return nonce;
	}

	/**
	 * @return the expireDate
	 */
	//public ZonedDateTime getExpireDate() {
	//public DateTime getExpireDate() {
	public Instant getExpireDate() {
		return expireDate;
	}

	/**
	 * @return the userData
	 */
	public String getUserData() {
		return userData;
	}

	/**
	 * @return the isValid
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * @param isValid the isValid to set
	 */
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	/**
	 * @return the resultText
	 */
	public String getResultText() {
		return resultText;
	}

	/**
	 * @param resultText the resultText to set
	 */
	public void setResultText(String resultText) {
		this.resultText = resultText;
	}

}
