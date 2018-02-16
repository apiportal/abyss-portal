/**
 * 
 */
package com.verapi.key.generate.intf;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author faik.saglar
 *
 */
public interface HashRemoteIntf extends Remote {
	
	public String generateHash(String inputData) throws RemoteException;
	
	public String generateSaltedPasswordHash(String password) throws RemoteException;
	
	public boolean isSaltedPasswordHashCorrect(String enteredPassword, String storedSaltedPasswordHash) throws RemoteException;

	public String generateRandomPassword(int length) throws RemoteException;
	
}
