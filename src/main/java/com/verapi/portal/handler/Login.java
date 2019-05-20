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

package com.verapi.portal.handler;

import com.verapi.abyss.common.Constants;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Login implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Login.class);
    private final AuthProvider authProvider;

    public Login(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.info("Login.handle invoked..");

        String username = routingContext.request().getFormAttribute("username");
        String password = routingContext.request().getFormAttribute("password");

        LOGGER.info("Received user:" + username);
        LOGGER.info("Received pass:" + password);

        JsonObject creds = new JsonObject()
                .put("username", username)
                .put("password", password);

        authProvider.authenticate(creds, authResult -> {
            if (authResult.succeeded()) {
                User user = authResult.result();
                String userName = user.principal().getString("username");
                routingContext.setUser(user); //TODO: Check context. Is this usefull? Should it be vertx context? 
                LOGGER.info("Logged in user: " + user.principal().encodePrettily());
                routingContext.put("username", userName);
                routingContext.session().regenerateId();
                routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME, userName);
                routingContext.response().putHeader("location", "/abyss/index").setStatusCode(302).end();
                LOGGER.info("redirected../index");
            } else {
                routingContext.fail(401);
            }
        });
    }

    public void pageRender(RoutingContext routingContext) {
        LOGGER.info("Login.pageRender invoked...");

        Boolean isUserActivated = routingContext.session().get("isUserActivated");
        if (isUserActivated == null) {
            isUserActivated = Boolean.FALSE;
        }
        routingContext.session().put("isUserActivated", false);

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());
        //configureThymeleafEngine(engine);


        routingContext.put("isUserActivated", isUserActivated);
        // and now delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_LOGIN, res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }
/*
    private void configureThymeleafEngine(ThymeleafTemplateEngine engine) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix(Constants.TEMPLATE_PREFIX);
        templateResolver.setSuffix(Constants.TEMPLATE_SUFFIX);
        engine.getThymeleafTemplateEngine().setTemplateResolver(templateResolver);

//        CustomMessageResolver customMessageResolver = new CustomMessageResolver();
//        engine.getThymeleafTemplateEngine().setMessageResolver(customMessageResolver);
    }    
*/
}
