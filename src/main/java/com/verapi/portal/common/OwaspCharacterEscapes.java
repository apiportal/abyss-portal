/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Rob Winch, 10 2018
 *
 */

package com.verapi.portal.common;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 *
 * @author Rob Winch
 * Ref: https://github.com/rwinch/spring-jackson-owasp/blob/master/spring-jackson-owasp-java/src/main/java/sample/OwaspConfig.java
 *
 */
public class OwaspCharacterEscapes extends CharacterEscapes {
    private final int[] ESCAPES;

    public OwaspCharacterEscapes() {
        ESCAPES = standardAsciiEscapesForJSON();
        for(int i=0;i<ESCAPES.length;i++) {
            if(!(Character.isAlphabetic(i) || Character.isDigit(i))) {
                ESCAPES[i] = CharacterEscapes.ESCAPE_CUSTOM;
            }
        }
    }

    @Override
    public SerializableString getEscapeSequence(int ch) {
        String unicode = String.format("\\u%04x", ch);

       // ***** System.out.print("\\u"+Integer.toHexString(0x10000 | ch).substring(1).toUpperCase());


        return new SerializedString(unicode);
    }

    @Override
    public int[] getEscapeCodesForAscii() {
        return ESCAPES;
    }

}