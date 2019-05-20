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

import com.verapi.key.generate.intf.HashRemoteIntf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;


/**
 * @author faik.saglar
 * @version 1.1
 */
public class Hash implements HashRemoteIntf {

    private static final int ITERATIONS = 1000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 32; // bytes
    private static final Random SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder();
    private static final Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();
    private static final Logger LOGGER = LoggerFactory.getLogger(Hash.class);


    public Hash() {
        super();
    }

    /**
     * Unit Test
     *
     * @param args args
     * @author faik.saglar
     */
    public static void main(String[] args) {

        Hash hash = new Hash();

        String hashStr = hash.generateSaltedPasswordHash("password");

        LOGGER.trace("{}", hashStr);

        LOGGER.trace("{}", hash.isSaltedPasswordHashCorrect("enteredPassword", hashStr));

        LOGGER.trace("{}", hash.isSaltedPasswordHashCorrect("password", hashStr));

        LOGGER.trace("{}", hash.generateRandomPassword(8));

    }

    /*
     * http://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
     * https://www.owasp.org/index.php/Hashing_Java
     * https://www.owasp.org/index.php/Password_Storage_Cheat_Sheet
     * https://docs.oracle.com/javase/7/docs/api/java/security/MessageDigest.html
     * https://adambard.com/blog/3-wrong-ways-to-store-a-password/
     * http://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html
     * http://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
     *  http://stackoverflow.com/questions/2860943/how-can-i-hash-a-password-in-java
     *  http://security.stackexchange.com/questions/4781/do-any-security-experts-recommend-bcrypt-for-password-storage/6415#6415
     *   !!!!to avoid DDOS check the username before checking the password. Then if there have been multiple failed login attempts in a row for that user (say, 10 of them) reject the password without even checking it unless the account has "cooled off" for a few minutes. This way an account can be DDOS'd but not the whole server, and it makes even horribly weak passwords impossible to brute force without an offline attack.
     */

    /**
     * Returns the hash of input data
     *
     * @param inputData data to be used for calculating hash
     * @return hash {@link String}
     * @author faik.saglar
     */
    public String generateHash(String inputData) {

        String digestText = null;

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256"); //TODO: Singleton ya da Pool yapılmalı mı?

            byte[] hashBytes = md.digest((inputData).getBytes("UTF-8"));

            digestText = base64UrlEncoder.encodeToString(hashBytes); //TODO: is thread-safe?

        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("generateHash NoSuchAlgorithmException error: {} \n error stack: {}", e.getLocalizedMessage(), e.getStackTrace());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("generateHash UnsupportedEncodingException error: {} \n error stack: {}", e.getLocalizedMessage(), e.getStackTrace());
        }

        return digestText;

    }

    /**
     * Returns random salt to be used for password hashing
     *
     * @return salt byte[]
     * @author faik.saglar
     */
    private byte[] generateSalt() {

        byte[] saltBytes = new byte[SALT_LENGTH];

        SECURE_RANDOM.nextBytes(saltBytes);

        //ByteUtils.printByteArray(saltBytes, "saltBytes"); //TEST

        return saltBytes;
    }

    /**
     * Returns string formatted salted hash of input password
     *
     * @param password clear password to be hashed {@link String}
     * @return salted Password Hash {@link String}
     * @author faik.saglar
     */
    public String generateSaltedPasswordHash(String password) {

        return generateSaltedPasswordHash(password, generateSalt());
    }

    /**
     * Returns string formatted salted hash of input password
     *
     * @param password  clear password to be hashed as {@link String}
     * @param saltBytes random salt to be used for password hashing in byte[]
     * @return salted Password Hash {@link String}
     * @author faik.saglar
     */
    private String generateSaltedPasswordHash(String password, byte[] saltBytes) {

        char[] passwordChars = password.toCharArray();

        byte[] hashedPassword;

        String saltPlusHashedPasswordString;

        PBEKeySpec spec = null;
        SecretKey key;

        try {
            spec = new PBEKeySpec(
                    passwordChars,
                    saltBytes,
                    ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            key = skf.generateSecret(spec);
            hashedPassword = key.getEncoded();

            byte[] saltPlusHashedPassword = new byte[SALT_LENGTH + hashedPassword.length];
            System.arraycopy(saltBytes, 0, saltPlusHashedPassword, 0, SALT_LENGTH);
            System.arraycopy(hashedPassword, 0, saltPlusHashedPassword, SALT_LENGTH, hashedPassword.length);

            saltPlusHashedPasswordString = base64UrlEncoder.encodeToString(saltPlusHashedPassword).replaceAll("=", "");

            //System.out.println(saltPlusHashedPasswordString); //TEST
            //ByteUtils.printByteArray(saltPlusHashedPassword, "saltPlusHashedPassword"); //TEST

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // TODO: handle exception
            LOGGER.error("generateSaltedPasswordHash error: {} \n error stack: {}", e.getLocalizedMessage(), e.getStackTrace());
            throw new RuntimeException(e);
        } finally { //Sensitive data should be cleared after you have used it (set the array elements to zero).
            spec.clearPassword();
//   try {
//    key.destroy();  //javax.security.auth.DestroyFailedException at javax.security.auth.Destroyable.destroy(Unknown Source)
//   } catch (DestroyFailedException e) {
//    // TODO Auto-generated catch block
//    e.printStackTrace();
//   }
        }

        return saltPlusHashedPasswordString;

    }

    /**
     * Extract salt from stored salted password hash
     *
     * @param storedSaltedPasswordHash stored salted password hash as {@link String}
     * @return salt random salt used for password hashing in byte[]
     * @author faik.saglar
     */
    private byte[] extractSalt(String storedSaltedPasswordHash) {

        byte[] decodedBytes = base64UrlDecoder.decode(storedSaltedPasswordHash);

        //ByteUtils.printByteArray(decodedBytes, "decodedBytes"); //TEST

        byte[] storedSalt = new byte[SALT_LENGTH];
        System.arraycopy(decodedBytes, 0, storedSalt, 0, SALT_LENGTH);

        //ByteUtils.printByteArray(storedSalt, "storedSalt"); //TEST

        return storedSalt;
    }

    /**
     * Check given password
     *
     * @param enteredPassword          clear password to be checked for correctness in {@link String} format
     * @param storedSaltedPasswordHash stored salted password hash as {@link String}
     * @return is Correct boolean
     * @author faik.saglar
     */
    public boolean isSaltedPasswordHashCorrect(String enteredPassword, String storedSaltedPasswordHash) {

        if (enteredPassword == null || enteredPassword.isEmpty()) {
            return false;
        }

        String enteredPasswordHash = generateSaltedPasswordHash(enteredPassword, extractSalt(storedSaltedPasswordHash)).replaceAll("=", "");
        enteredPassword = null; //Delete password

        LOGGER.trace("Check equality:*********************");
        LOGGER.trace("{}", storedSaltedPasswordHash);
        LOGGER.trace("{}", enteredPasswordHash);

        return storedSaltedPasswordHash.equals(enteredPasswordHash);

    }

    /**
     * Generates a random password of a given length, using letters and digits.
     * Reference: http://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
     *
     * @param length the length of the password
     * @return a random password
     */
    public String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int c = SECURE_RANDOM.nextInt(62);
            if (c <= 9) {
                sb.append(String.valueOf(c));
            } else if (c < 36) {
                sb.append((char) ('a' + c - 10));
            } else {
                sb.append((char) ('A' + c - 36));
            }
        }
        return sb.toString();
    }

}
