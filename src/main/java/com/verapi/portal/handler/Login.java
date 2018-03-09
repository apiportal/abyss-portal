package com.verapi.portal.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.verapi.portal.common.Constants;

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
                routingContext.setUser(user); //TODO: Check context. Is this usefull? Should it be vertx context? 
                logger.info("Logged in user: " + user.principal().encodePrettily());
                routingContext.put("username", user.principal().getString("username"));
                routingContext.response().putHeader("location", "/index").setStatusCode(302).end();
                logger.info("redirected../index");
            } else {
                routingContext.fail(401);
            }
        });
    }

    public void pageRender(RoutingContext routingContext) {
        logger.info("Login.pageRender invoked...");

        Boolean isUserActivated = routingContext.session().get("isUserActivated");
        if (isUserActivated == null) {
        	isUserActivated = new Boolean(false);
        }
        
        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();
        //configureThymeleafEngine(engine);

        
        routingContext.put("isUserActivated", isUserActivated);
        // and now delegate to the engine to render it.
        engine.render(routingContext, "webroot/", "login.html", res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    private void configureThymeleafEngine(ThymeleafTemplateEngine engine) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix(Constants.TEMPLATE_PREFIX);
        templateResolver.setSuffix(Constants.TEMPLATE_SUFFIX);
        engine.getThymeleafTemplateEngine().setTemplateResolver(templateResolver);

//        CustomMessageResolver customMessageResolver = new CustomMessageResolver();
//        engine.getThymeleafTemplateEngine().setMessageResolver(customMessageResolver);
    }    
    
}
