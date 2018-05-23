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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author faik.saglar
 *
 */
public interface ApiKeyRemoteIntf extends Remote {

	String generateRandomKey() throws RemoteException;
	
}
