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
package com.verapi.key.util;

import java.nio.ByteBuffer;

/**
 * @author faik.saglar
 *
 */
public class ByteUtils {
	
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);    //TODO: is thread safe?

    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip 
        return buffer.getLong();
    }
    
	public static void printByteArray(byte[] bytes, String variableName) {
        //byte[] bytes = str.getBytes();
        System.out.print("Byte Array of "+ variableName + " ["+ bytes.length +" bytes]: ");
        for(byte b: bytes){
            //System.out.print(b +" ");
            System.out.printf("%02X ", b);
        }
        System.out.println();
	}

/*	private static void convertByteArray(byte[] bytes, String variableName) {
        
        for(byte b: bytes){
            //System.out.print(b +" ");
            System.out.printf("%02X ", b);
        }
	}*/
}
