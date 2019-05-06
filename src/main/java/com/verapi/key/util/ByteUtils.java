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
