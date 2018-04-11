package com.verapi.portal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(MailVerticle.class);

    public static final String TOKEN = "token";
    public static final String TO = "to";
    public static final String ABYSS_MAIL_CLIENT = "ABYSS_MAIL_CLIENT";

    private final String SMTP_HOST = "verapi-com.mail.protection.outlook.com";//"dev.apiportal.com"; //"localhost";
    private final int SMTP_PORT = 25;

    private final LoginOption SMTP_LOGIN_OPTION = LoginOption.DISABLED; //LoginOption.REQUIRED;
    private final String SMTP_AUTH_METHOD = "PLAIN"; //"PLAIN";

    private final String SMTP_USER = "user"; //"username";
    private final String SMTP_PASS = "pass"; //"password";

    private MailClient mailClient;

    private String token;

    public void start() {
        logger.info("MailClient is starting");

        MailConfig mailConfig = new MailConfig()
                .setHostname(SMTP_HOST)
                .setPort(SMTP_PORT)
                //.setStarttls(StartTLSOptions.REQUIRED)
                .setLogin(SMTP_LOGIN_OPTION)
                //.setAuthMethods(SMTP_AUTH_METHOD)
                //.setUsername(SMTP_USER)
                //.setPassword(SMTP_PASS)
                ;

        mailClient = MailClient.createShared(vertx, mailConfig);

        vertx.eventBus().<JsonObject>consumer(ABYSS_MAIL_CLIENT).handler(mailSender());
        logger.info("MailClient is listening on Event Bus @ ABYSS_MAIL_CLIENT");

    }

    private Handler<Message<JsonObject>>mailSender() {
        return msg -> {

            logger.info("MailVerticle - mailSender invoked...");
            logger.info("Msg:" + msg.body().encodePrettily());

            String token = msg.body().getString(TOKEN);
            String toEmail = msg.body().getString(TO);

            MailMessage email = new MailMessage()
                .setFrom("activation@apiportal.com")
                .setTo(toEmail)
                .setCc("faik.saglar@verapi.com")
                .setBcc("halil.ozkan@verapi.com")
                .setBounceAddress("info@verapi.com")
                .setSubject("Activate Your Verapi API Portal Account")
                .setText("Please click -> Activate My API Portal Account")
                .setHtml("<a href=\"http://localhost:38081/abyss/activate/v=" + token + "\">Activate My API Portal Account</a>");

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
