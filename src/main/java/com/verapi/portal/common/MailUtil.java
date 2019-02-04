/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 4 2018
 *
 */

package com.verapi.portal.common;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class MailUtil {
    private static Logger logger = LoggerFactory.getLogger(MailUtil.class);

    public static String renderActivationMailBody(RoutingContext routingContext, String activationUrl, String activationText) {
        logger.info("renderActivationMailBody invoked...");

        AtomicReference<String> result = new AtomicReference<String>();
        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put("url.activation", activationUrl);
        routingContext.put("text.activation", activationText);
        routingContext.put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put("url.activation", activationUrl)
                .put("text.activation", activationText)
                .put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_ACTIVATE, res -> {
            if (res.succeeded()) {
                result.set(res.result().toString("UTF-8"));
            } else {
                logger.error(res.cause().getLocalizedMessage());
                logger.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

    public static String renderWelcomeMailBody(RoutingContext routingContext, String fullName) {
        logger.info("renderWelcomeMailPage invoked...");
        AtomicReference<String> result = new AtomicReference<String>();

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put("full.name", fullName);
        routingContext.put("url.login", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL));
        routingContext.put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put("full.name", fullName)
                .put("url.login", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL))
                .put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_WELCOME, res -> {
            if (res.succeeded()) {
                result.set(res.result().toString("UTF-8"));
            } else {
                logger.error(res.cause().getLocalizedMessage());
                logger.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

    public static String renderForgotPasswordMailBody(RoutingContext routingContext, String resetpasswordUrl, String resetpasswordText) {
        logger.info("renderForgotPasswordMailBody invoked...");
        AtomicReference<String> result = new AtomicReference<String>();

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put("url.resetpassword", resetpasswordUrl);
        routingContext.put("text.resetpassword", resetpasswordText);
        routingContext.put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put("url.resetpassword", resetpasswordUrl)
                .put("text.resetpassword", resetpasswordText)
                .put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_FORGOTPASSWORD, res -> {
            if (res.succeeded()) {
                result.set(res.result().toString("UTF-8"));
            } else {
                logger.error(res.cause().getLocalizedMessage());
                logger.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

    public static String renderPasswordResetMailBody(RoutingContext routingContext, String fullName) {
        logger.info("renderForgotPasswordMailBody invoked...");
        AtomicReference<String> result = new AtomicReference<String>();

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put("full.name", fullName);
        routingContext.put("url.login", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL));
        routingContext.put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put("full.name", fullName)
                .put("url.login", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL))
                .put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));


        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_RESETPASSWORD, res -> {
            if (res.succeeded()) {
                result.set(res.result().toString("UTF-8"));
            } else {
                logger.error(res.cause().getLocalizedMessage());
                logger.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

}
