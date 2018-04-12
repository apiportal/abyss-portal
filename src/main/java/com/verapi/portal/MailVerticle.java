package com.verapi.portal;

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
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

import java.util.Locale;

public class MailVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(MailVerticle.class);

    private MailClient mailClient;

    private final String hrefHost = Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_HREF_HOST, "apiportal.com");//TODO:
    private final String hrefPort = String.valueOf(Config.getInstance().getConfigJsonObject().getInteger(Constants.MAIL_HREF_PORT, 80));


    public void start() {
        logger.info("MailClient is starting");

        MailConfig mailConfig = new MailConfig()
                .setHostname(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_HOST, "verapi-com.mail.protection.outlook.com")) //"dev.apiportal.com"; //"localhost"
                .setPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.MAIL_SMTP_PORT, 25))
                .setStarttls(StartTLSOptions.valueOf(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_START_TLS_OPTION, "OPTIONAL").toUpperCase(Locale.ENGLISH)))
                .setLogin(LoginOption.valueOf(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_LOGIN_OPTION, "DISABLED").toUpperCase(Locale.ENGLISH)))
        ;
        if (mailConfig.getLogin() == LoginOption.REQUIRED) {
            mailConfig
            .setAuthMethods(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_AUTH_METHOD, "PLAIN"))
            .setUsername(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_AUTH_USERNAME, ""))
            .setPassword(Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SMTP_AUTH_PASSWORD, ""))
            ;
        }

        mailClient = MailClient.createShared(vertx, mailConfig);
        logger.info("MailClient is configured and created.");

        vertx.eventBus().<JsonObject>consumer(Constants.ABYSS_MAIL_CLIENT).handler(mailSender());
        logger.info("MailClient is listening on Event Bus @ " + Constants.ABYSS_MAIL_CLIENT);
    }

    private Handler<Message<JsonObject>>mailSender() {
        return msg -> {

            logger.info("MailVerticle - mailSender invoked...");
            logger.info("Msg:" + msg.body().encodePrettily());

            String token = msg.body().getString(Constants.EB_MSG_TOKEN, "");
            String toEmail = msg.body().getString(Constants.EB_MSG_TO_EMAIL, "");
            String tokenType = msg.body().getString(Constants.EB_MSG_TOKEN_TYPE, Constants.ACTIVATION_TOKEN);

            String subject;
            String text;
            String path;
            if (tokenType.equals(Constants.ACTIVATION_TOKEN)) {
                subject = Constants.ACTIVATION_SUBJECT;
                text = Constants.ACTIVATION_TEXT;
                path = Constants.ACTIVATION_PATH;
            } else if (tokenType.equals(Constants.RESET_PASSWORD_TOKEN)) {
                subject = Constants.RESET_PASSWORD_SUBJECT;
                text = Constants.RESET_PASSWORD_TEXT;
                path = Constants.RESET_PASSWORD_PATH;
            } else {//TODO:handle token type is empty
                subject = Constants.ACTIVATION_SUBJECT;
                text = Constants.ACTIVATION_TEXT;
                path = Constants.ACTIVATION_PATH;
            }

            if (token.isEmpty()) {
                //TODO:give error
                logger.error("Mail Client received both token and type empty");
                //throw
            }

            if (toEmail.isEmpty()) {
                //TODO:give error
                logger.error("Mail Client received both token and type empty");
                //throw
            }


            MailMessage email = new MailMessage()
                .setFrom("info@apiportal.com")
                .setTo(toEmail)
                .setCc("faik.saglar@verapi.com")
                .setBcc("halil.ozkan@verapi.com")
                .setBounceAddress("info@verapi.com")
                .setSubject(subject)
                .setText("Please click -> Activate My API Portal Account")
                .setHtml("<a href=\"http://"+hrefHost+":"+hrefPort+"/abyss"+path+"/?v=" + token + "\">"+text+"</a>");

            mailClient.sendMail(email, result -> {
                if (result.succeeded()) {
                    logger.info(result.result().toString());
                    logger.info("Mail successfully sent");
                } else {
                    logger.error("Mail client got exception:"+result.cause());
                    //result.cause().printStackTrace();
                }
            });

        };
    }
}
