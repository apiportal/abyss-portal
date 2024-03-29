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

package com.verapi.key.generate.impl;

import com.verapi.key.model.CryptoOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Encrypt Decrypt Library
 *
 * @author faik.saglar
 * @version 1.0
 */

class Cryptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cryptor.class);

    private static final byte[] IV = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
    private static final String GENERATE_ALGORITHM = "AES";

    private static final String CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 128; //bit
    private static final IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
    private static final String CRYPTO_OPERATION_RESULT_ERROR_ERROR_STACK = "CryptoOperationResult error: {} \n error stack: {}";
    private static SecretKey aesEncKey;
    private static Cryptor instance = new Cryptor();

    private Cryptor() {
        generateAesKey();
    }

    static Cryptor getInstance() {
        return instance;
    }

    /**
     * Generate Aes Key
     *
     * @author faik.saglar
     */
    private void generateAesKey() { //TODO: input key type: enc / mac
        KeyGenerator keyGenerator;

        try {
            keyGenerator = KeyGenerator.getInstance(GENERATE_ALGORITHM);
            keyGenerator.init(KEY_SIZE);

            aesEncKey = keyGenerator.generateKey();
            //TODO: generate KCV
            //TODO: use keyblock mechanism
            //TODO: store key / load key
            //ByteUtils.printByteArray(aesEncKey.getEncoded(), "AES ENC KEY"); //TODO: Kaldır

        } catch (NoSuchAlgorithmException e1) {
            // TODO: handle exception
            // TODO: log exception
            LOGGER.error("generateAesKey error: {} \n error stack: {}", e1.getLocalizedMessage(), e1.getStackTrace());
            //throw e1;
        }
    }

    /**
     * Encrypt Input Bytes
     *
     * @param plainInput plain Input
     * @return {@link CryptoOperationResult}
     * @author faik.saglar
     */
    CryptoOperationResult enc(byte[] plainInput) {
        return crypt(plainInput, Cipher.ENCRYPT_MODE);
    }

    /**
     * Decrypt Input Bytes
     *
     * @param cipherInput cipher Input
     * @return {@link CryptoOperationResult}
     * @author faik.saglar
     */
    CryptoOperationResult dec(byte[] cipherInput) {
        return crypt(cipherInput, Cipher.DECRYPT_MODE);
    }

    /**
     * Common Encrypt Decrypt Method
     *
     * @param inputBytes    input Bytes
     * @param operationMode (Encrypt / Decrypt)
     * @return {@link CryptoOperationResult}
     * @author faik.saglar
     */
    private CryptoOperationResult crypt(byte[] inputBytes, int operationMode) {

        Cipher aesCipher;

        try {
            aesCipher = Cipher.getInstance(CRYPT_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(CRYPTO_OPERATION_RESULT_ERROR_ERROR_STACK, e.getLocalizedMessage(), e.getStackTrace());
            return new CryptoOperationResult(inputBytes, false, "NoSuchAlgorithmException", e);
        } catch (NoSuchPaddingException e) {
            LOGGER.error(CRYPTO_OPERATION_RESULT_ERROR_ERROR_STACK, e.getLocalizedMessage(), e.getStackTrace());
            return new CryptoOperationResult(inputBytes, false, "NoSuchPaddingException", e);
        }
        try {
            aesCipher.init(operationMode, aesEncKey, ivParameterSpec);
        } catch (InvalidKeyException e) {
            LOGGER.error(CRYPTO_OPERATION_RESULT_ERROR_ERROR_STACK, e.getLocalizedMessage(), e.getStackTrace());
            return new CryptoOperationResult(inputBytes, false, "InvalidKeyException", e);
        } catch (InvalidAlgorithmParameterException e) {
            LOGGER.error(CRYPTO_OPERATION_RESULT_ERROR_ERROR_STACK, e.getLocalizedMessage(), e.getStackTrace());
            return new CryptoOperationResult(inputBytes, false, "InvalidAlgorithmParameterException", e);
        }

        byte[] outputBytes;
        try {
            outputBytes = aesCipher.doFinal(inputBytes);
        } catch (IllegalBlockSizeException e) {
            LOGGER.error(CRYPTO_OPERATION_RESULT_ERROR_ERROR_STACK, e.getLocalizedMessage(), e.getStackTrace());
            return new CryptoOperationResult(inputBytes, false, "IllegalBlockSizeException", e);

        } catch (BadPaddingException e) {
            LOGGER.error(CRYPTO_OPERATION_RESULT_ERROR_ERROR_STACK, e.getLocalizedMessage(), e.getStackTrace());
            return new CryptoOperationResult(inputBytes, false, "BadPaddingException", e);
        }

        return new CryptoOperationResult(outputBytes, true, "SUCCESSFULL", null);
    }

}
