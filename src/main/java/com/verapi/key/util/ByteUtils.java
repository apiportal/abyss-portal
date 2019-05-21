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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * @author faik.saglar
 */
public final class ByteUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteUtils.class);
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);    //TODO: is thread safe?

    private ByteUtils() {
        throw new IllegalStateException("Utility class");
    }

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
        LOGGER.trace("Byte Array of {} [{} bytes]: ", variableName, bytes.length);
        for (byte b : bytes) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("%02X", b));
            }
        }
    }
}
