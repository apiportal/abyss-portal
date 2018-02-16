/**
 * 
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
	 * @param outputBytes
	 * @param isValid
	 * @param resultText
	 * @param exception
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
