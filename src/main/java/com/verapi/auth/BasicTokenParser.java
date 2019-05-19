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
package com.verapi.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.util.Base64;


/**
 * @author faik.saglar
 *
 */
public class BasicTokenParser {
    private static Logger logger = LoggerFactory.getLogger(BasicTokenParser.class);

    private static Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder();

    /**
     * Http Basic Auth Token Parser
     *
     * @param authorizationBasicToken token to be parsed having "Basic base64(user:pass)" format
     * @return {@link BasicTokenParseResult}
     */
    public static BasicTokenParseResult authorizationBasicTokenParser(String authorizationBasicToken) {
        if (authorizationBasicToken==null||authorizationBasicToken.isEmpty()) {
            logger.error("authorizationBasicToken is null or empty:"+authorizationBasicToken);
            return new BasicTokenParseResult(true, "", "");
        }

        String[] pieces = authorizationBasicToken.split(" "); //May contain "Basic / Bearer" SPACE before TOKEN
        logger.info("pieces.length:"+pieces.length);
        logger.info(pieces[0]);

        if (pieces.length < 2) {
            logger.error("token type is null or empty:"+authorizationBasicToken);
            return new BasicTokenParseResult(true, "", "");
        }

        if (!(pieces[0].equals("Basic"))) {
            logger.error("token type is wrong:"+authorizationBasicToken);
            return new BasicTokenParseResult(true, "", "");
        }

        String credentials;
        try {
            byte[] bytes = DatatypeConverter.parseBase64Binary(pieces[pieces.length-1]); // TODO: Jaxb bağımsızlığı için; DatatypeConverter.parseBase64Binary bu sınıfın yerine başka bir tane bul

            credentials = new String(bytes, "UTF8");//TODO: Charset
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            logger.error("Exception while converting bytes of authorizationBasicToken to String:"+e.getMessage());
            return new BasicTokenParseResult(true, "", "");
        }
        String[] parts = credentials.split(":"); //parts => user name | password

        if (parts.length < 2)
        {
            logger.error("authorization header does not contain (:) splitter -> "+authorizationBasicToken);
            return new BasicTokenParseResult(true, "", "");
        }
        return new BasicTokenParseResult(false, parts[0], parts[1]);
    }

    /**
     * Http Basic Auth Bearer Token Parser
     *
     * @param authorizationBearerToken token to be parsed having "Bearer base64(token)" format
     * @return BearerTokenParseResult
     */
    public static BearerTokenParseResult authorizationBearerTokenParser(String authorizationBearerToken) {
        if (authorizationBearerToken==null||authorizationBearerToken.isEmpty()) {
            logger.error("authorizationBearerToken is null or empty:"+authorizationBearerToken);
            return new BearerTokenParseResult(true, "");
        }

        String[] pieces = authorizationBearerToken.split(" "); //May contain "Basic / Bearer" SPACE before TOKEN
        logger.info("pieces.length:"+pieces.length);
        logger.info(pieces[0]);
        if (pieces.length < 2) {
            logger.error("token type is null or empty:"+authorizationBearerToken);
            return new BearerTokenParseResult(true, "");
        }

        if (pieces[0].equals("Bearer")) {
            return new BearerTokenParseResult(false, pieces[1]);
        } else {
            logger.error("token type is wrong:"+authorizationBearerToken);
            return new BearerTokenParseResult(true, "");
        }
    }

    public static String basicTokenEncoder(String username, String password, Boolean addBasic) {

        String token = username + ":" + password;

        String encodedToken = base64UrlEncoder.encodeToString(token.getBytes());

        //TODO: Remove - which one is better
        logger.info("basicTokenEncoder - DatatypeConverter.printBase64Binary:" + DatatypeConverter.printBase64Binary(token.getBytes()));
        logger.info("basicTokenEncoder - base64UrlEncoder:" + encodedToken);

        if (addBasic) {
            return "Basic " + encodedToken;
        }

        return encodedToken;
    }
}