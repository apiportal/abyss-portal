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

/**
 * @author faik.saglar
 *
 */
public class CryptoOperationResult {

	private byte[] outputBytes;
	private boolean isValid;
	private String resultText;
	private Exception exception;	
	/**
	 * @param outputBytes output bytes
	 * @param isValid is valid
	 * @param resultText result text
	 * @param exception exception
	 */
	public CryptoOperationResult(byte[] outputBytes, boolean isValid, String resultText, Exception exception) {
		this.outputBytes = outputBytes;
		this.isValid = isValid;
		this.resultText = resultText;
		this.exception = exception;
	}

	/**
	 * @return the outputBytes
	 */
	public byte[] getOutputBytes() {
		return outputBytes;
	}

	/**
	 * @return the isValid
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * @return the resultText
	 */
	public String getResultText() {
		return "Crypto Operation Result - " + resultText;
	}

	/**
	 * @return the exception
	 */
	public Exception getException() {
		return exception;
	}
	
	
}
