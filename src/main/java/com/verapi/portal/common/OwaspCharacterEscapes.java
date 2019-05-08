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