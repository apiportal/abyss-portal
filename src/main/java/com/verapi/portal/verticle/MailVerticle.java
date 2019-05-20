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

package com.verapi.portal.verticle;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;

public class MailVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailVerticle.class);

    private MailClient mailClient;

    private final String hrefHost = Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_HREF_HOST, "apiportal.com");//TODO:
    private final String hrefPort = String.valueOf(Config.getInstance().getConfigJsonObject().getInteger(Constants.MAIL_HREF_PORT, 80));


    public void start() {
        LOGGER.trace("MailClient is starting");

        MailConfig mailConfig = new MailConfig()
                .setHostname(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_HOST, "verapi-com.mail.protection.outlook.com")) //"dev.apiportal.com"; //"localhost"
                .setPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.MAIL_SMTP_PORT, 25))
                .setStarttls(StartTLSOptions.valueOf(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_START_TLS_OPTION, "OPTIONAL").toUpperCase(Locale.ENGLISH)))
                .setLogin(LoginOption.valueOf(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_LOGIN_OPTION, "DISABLED").toUpperCase(Locale.ENGLISH)))
                //.setAllowRcptErrors(true) //TODO: Oku
        ;
        if (mailConfig.getLogin() == LoginOption.REQUIRED) {
            mailConfig
            .setAuthMethods(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_AUTH_METHOD, "PLAIN"))
            .setUsername(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_AUTH_USERNAME, ""))
            .setPassword(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_AUTH_PASSWORD, ""))
            ;
        }

        mailClient = MailClient.createShared(vertx, mailConfig);
        LOGGER.trace("MailClient is configured and created.");

        vertx.eventBus().<JsonObject>consumer(Constants.ABYSS_MAIL_CLIENT).handler(mailSender());
        LOGGER.trace("MailClient is listening on Event Bus @ " + Constants.ABYSS_MAIL_CLIENT);
    }

    private Handler<Message<JsonObject>>mailSender() {
        return msg -> {

            LOGGER.info("MailVerticle - mailSender invoked...");
            LOGGER.debug("Msg:" + msg.body().encodePrettily());

            String token = msg.body().getString(Constants.EB_MSG_TOKEN, "");
            String toEmail = msg.body().getString(Constants.EB_MSG_TO_EMAIL, "");
            String tokenType = msg.body().getString(Constants.EB_MSG_TOKEN_TYPE, Constants.ACTIVATION_TOKEN);
            String htmlString = msg.body().getString(Constants.EB_MSG_HTML_STRING, Constants.DEFAULT_HTML_STRING);

            String from;
            String subject;
            String text;
            //String path;
            if (tokenType.equals(Constants.ACTIVATION_TOKEN)) {
                from = Constants.MAIL_FROM_EMAIL_ACTIVATION;
                subject = Constants.ACTIVATION_SUBJECT;
                text = Constants.ACTIVATION_TEXT;
                //path = Constants.ACTIVATION_PATH;
            } else if (tokenType.equals(Constants.RESET_PASSWORD_TOKEN)) {
                from = Constants.MAIL_FROM_EMAIL_RESET_PASSWORD;
                subject = Constants.RESET_PASSWORD_SUBJECT;
                text = Constants.RESET_PASSWORD_TEXT;
                //path = Constants.RESET_PASSWORD_PATH;
            } else if (tokenType.equals(Constants.WELCOME_TOKEN)) {
                from = Constants.MAIL_FROM_EMAIL_WELCOME;
                subject = Constants.WELCOME_SUBJECT;
                text = Constants.WELCOME_TEXT;
                //path = Constants.RESET_PASSWORD_PATH;
            } else if (tokenType.equals((Constants.PASSWORD_RESET_TOKEN))) {
                from = Constants.MAIL_FROM_EMAIL_NOTIFICATION;
                subject = Constants.PASSWORD_RESET_SUBJECT;
                text = Constants.PASSWORD_RESET_TEXT;
                //path = Constants.RESET_PASSWORD_PATH;
            } else if (tokenType.equals(Constants.INVITE_USER_TOKEN)) {
                from = Constants.MAIL_FROM_EMAIL_INVITATION;
                subject = Constants.INVITATION_SUBJECT;
                text = Constants.INVITE_USER_TEXT;
                //path = Constants.INVITE_USER_PATH;
            } else {//TODO:handle token type is empty
                from = Constants.MAIL_FROM_EMAIL_ACTIVATION;
                subject = Constants.ACTIVATION_SUBJECT;
                text = Constants.ACTIVATION_TEXT;
                //path = Constants.ACTIVATION_PATH;
            }

            if (token.isEmpty()) {
                //TODO:give error
                LOGGER.info("Mail Client received both token and type empty");
                //throw
            }

            if (toEmail.isEmpty()) {
                //TODO:give error
                LOGGER.info("Mail Client received both token and type empty");
                //throw
            }



            MailMessage email = new MailMessage()
                .setFrom(from)
                .setTo(toEmail)
                //.setCc("faik.saglar@verapi.com")
                .setBcc(Arrays.asList("halil.ozkan@verapi.com","faik.saglar@verapi.com"))
                .setBounceAddress("info@verapi.com")
                .setSubject(subject)
                .setText(text)
                    //.setHeaders() //TODO: Oku
                .setHtml(htmlString);

            mailClient.sendMail(email, result -> {
                if (result.succeeded()) {
                    LOGGER.debug(result.result().toString());
                    LOGGER.info("Mail successfully sent");
                } else {
                    LOGGER.error("Mail client got exception:"+result.cause());
                    //result.cause().printStackTrace();
                }
            });

            msg.reply(new JsonObject().put("mailClient","mail processed"));

        };
    }


}
