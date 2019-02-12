package com.verapi.portal.handler;

import com.verapi.abyss.common.Constants;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Index implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(Index.class);
    private final AuthProvider authProvider;

    public Index(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("Index.handle invoked..");

    }

    public void pageRender(RoutingContext routingContext) {
        logger.info("Index.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        routingContext.put("user.name", routingContext.user().principal().getValue("username"));
        // and now delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_INDEX, res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

}
