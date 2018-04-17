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
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import static com.verapi.portal.common.Constants.TEMPLATE_DIR_EMAIL;

public class MailUtil {
    private static Logger logger = LoggerFactory.getLogger(MailUtil.class);

    public static String renderActivationMailBody(RoutingContext routingContext, String activationUrl, String activationText) {
        AtomicReference<String> result = new AtomicReference<String>();
        logger.info("renderMailPage invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        routingContext.put("url.activation", activationUrl);
        routingContext.put("text.activation", activationText);
        routingContext.put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));
        // and now delegate to the engine to render it.
        engine.render(routingContext, TEMPLATE_DIR_EMAIL, "activate.html", res -> {
            if (res.succeeded()) {
                //routingContext.response().putHeader("Content-Type", "text/html");
                //routingContext.response().end(res.result());

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
