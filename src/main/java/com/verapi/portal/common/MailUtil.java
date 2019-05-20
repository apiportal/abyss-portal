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

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class MailUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailUtil.class);

    private MailUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String renderActivationMailBody(RoutingContext routingContext, String activationUrl, String activationText) {
        LOGGER.info("renderActivationMailBody invoked...");

        AtomicReference<String> result = new AtomicReference<>();
        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put(Constants.MAIL_TEMPLATE_URL_ACTIVATION, activationUrl);
        routingContext.put(Constants.MAIL_TEMPLATE_TEXT_ACTIVATION, activationText);
        routingContext.put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put(Constants.MAIL_TEMPLATE_URL_ACTIVATION, activationUrl)
                .put(Constants.MAIL_TEMPLATE_TEXT_ACTIVATION, activationText)
                .put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_ACTIVATE, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                result.set(res.result().toString(StandardCharsets.UTF_8));
            } else {
                LOGGER.error(res.cause().getLocalizedMessage());
                LOGGER.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

    public static String renderWelcomeMailBody(RoutingContext routingContext, String fullName) {
        LOGGER.info("renderWelcomeMailPage invoked...");
        AtomicReference<String> result = new AtomicReference<>();

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put(Constants.MAIL_TEMPLATE_FULL_NAME, fullName);
        routingContext.put(Constants.MAIL_TEMPLATE_URL_LOGIN, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL));
        routingContext.put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put(Constants.MAIL_TEMPLATE_FULL_NAME, fullName)
                .put(Constants.MAIL_TEMPLATE_URL_LOGIN, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL))
                .put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_WELCOME, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                result.set(res.result().toString(StandardCharsets.UTF_8));
            } else {
                LOGGER.error(res.cause().getLocalizedMessage());
                LOGGER.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

    public static String renderForgotPasswordMailBody(RoutingContext routingContext, String resetpasswordUrl, String resetpasswordText) {
        LOGGER.info("renderForgotPasswordMailBody invoked...");
        AtomicReference<String> result = new AtomicReference<>();

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put(Constants.MAIL_TEMPLATE_URL_RESET_PASSWORD, resetpasswordUrl);
        routingContext.put(Constants.MAIL_TEMPLATE_TEXT_RESET_PASSWORD, resetpasswordText);
        routingContext.put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put(Constants.MAIL_TEMPLATE_URL_RESET_PASSWORD, resetpasswordUrl)
                .put(Constants.MAIL_TEMPLATE_TEXT_RESET_PASSWORD, resetpasswordText)
                .put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_FORGOTPASSWORD, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                result.set(res.result().toString(StandardCharsets.UTF_8));
            } else {
                LOGGER.error(res.cause().getLocalizedMessage());
                LOGGER.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

    public static String renderPasswordResetMailBody(RoutingContext routingContext, String fullName) {
        LOGGER.info("renderForgotPasswordMailBody invoked...");
        AtomicReference<String> result = new AtomicReference<>();

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put(Constants.MAIL_TEMPLATE_FULL_NAME, fullName);
        routingContext.put(Constants.MAIL_TEMPLATE_URL_LOGIN, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL));
        routingContext.put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put(Constants.MAIL_TEMPLATE_FULL_NAME, fullName)
                .put(Constants.MAIL_TEMPLATE_URL_LOGIN, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL))
                .put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));


        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_RESETPASSWORD, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                result.set(res.result().toString(StandardCharsets.UTF_8));
            } else {
                LOGGER.error(res.cause().getLocalizedMessage());
                LOGGER.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }


    public static String renderInviteUserMailBody(RoutingContext routingContext, String invitationUrl, String invitationText) {
        LOGGER.info("renderInviteUserMailBody invoked...");

        AtomicReference<String> result = new AtomicReference<>();
        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put(Constants.MAIL_TEMPLATE_URL_INVITATION, invitationUrl);
        routingContext.put(Constants.MAIL_TEMPLATE_TEXT_INVITATION, invitationText);
        routingContext.put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put(Constants.MAIL_TEMPLATE_URL_INVITATION, invitationUrl)
                .put(Constants.MAIL_TEMPLATE_TEXT_INVITATION, invitationText)
                .put(Constants.MAIL_TEMPLATE_IMAGE_URL, Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_INVITE_USER, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                result.set(res.result().toString(StandardCharsets.UTF_8));
            } else {
                LOGGER.error(res.cause().getLocalizedMessage());
                LOGGER.error(new JsonObject(routingContext.getDelegate().data()).encodePrettily());
                routingContext.fail(res.cause());
            }
        });
        return result.get();
    }

}
