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
public interface ApiKeyRemoteIntf extends Remote {

	public String generateRandomKey() throws RemoteException; 
	
}
