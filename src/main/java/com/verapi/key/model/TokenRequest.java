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