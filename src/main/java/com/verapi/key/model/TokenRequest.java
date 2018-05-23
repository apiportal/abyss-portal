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

/**
 * @author faik.saglar
 * @version 1.0
 *
 */
public class TokenRequest implements Serializable{
	
	private static final long serialVersionUID = -3876122451170298004L;
	
	private String userData;
	private long secondsToExpire;

	/**
	 * @param userData user data
	 * @param secondsToExpire seconds to expire
	 */
	public TokenRequest(String userData, long secondsToExpire) {
		this.userData = userData;
		this.secondsToExpire = secondsToExpire;
	}

	/**
	 * @return the userData
	 */
	public String getUserData() {
		return userData;
	}

	/**
	 * @return the secondsToExpire
	 */
	public long getSecondsToExpire() {
		return secondsToExpire;
	}
}