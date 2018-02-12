package com.verapi.starter.handler;

import com.verapi.starter.MainVerticle;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Login implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(Login.class);
    private final AuthProvider authProvider;

    public Login(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("Login.handle invoked..");

        String username = routingContext.request().getFormAttribute("username");
        String password = routingContext.request().getFormAttribute("password");

        logger.info("Received user:" + username);
        logger.info("Received pass:" + password);

        JsonObject creds = new JsonObject()
                .put("username", username)
                .put("password", password);

        authProvider.authenticate(creds, authResult -> {
            if (authResult.succeeded()) {
                User user = authResult.result();
                logger.info("Logged in user: " + user.principal().encodePrettily());
                routingContext.put("username", routingContext.user().principal().getString("username"));
                //routingContext.put("username", routingContext.request().getFormAttribute("username"));
                routingContext.response().putHeader("location", "/full-width-light/index").setStatusCode(302).end();
                logger.info("redirected../full-width-light/index");
            } else {
                routingContext.fail(401);
            }
        });
    }

    public void pageRender(RoutingContext routingContext) {
        logger.info("Login.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        // we define a hardcoded title for our application
        routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(routingContext, "src/main/resources/webroot/full-width-light/", "login.html", res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

}
