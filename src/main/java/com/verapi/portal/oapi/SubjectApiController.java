/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 5 2018
 *
 */

package com.verapi.portal.oapi;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@AbyssApiController(apiSpec = "/openapi/Subject.yaml")
public class SubjectApiController extends AbstractApiController {
    private static Logger logger = LoggerFactory.getLogger(SubjectApiController.class);

    public SubjectApiController(Vertx vertx, Router router) {
        super(vertx, router);
    }

    @Override
    public void init() {
        logger.info("initializing");

        OpenAPI3RouterFactory.createRouterFactoryFromFile(vertx, apiSpec, ar -> {
                    // The router factory instantiation could fail
                    if (ar.succeeded()) {
                        logger.info("OpenAPI3RouterFactory created");
                        OpenAPI3RouterFactory factory = ar.result();

                        Reflections reflections = new Reflections(
                                new ConfigurationBuilder()
                                        .setUrls(ClasspathHelper.forClass(this.getClass()))
                                        .setScanners(new MethodAnnotationsScanner()));

                        Set<Method> resources =
                                reflections.getMethodsAnnotatedWith(AbyssApiOperationHandler.class);
                        logger.info("AbyssApiOperationHandler annotated methods; " + resources.toString());
                        resources.forEach(method -> {
                            logger.info("method name: " + method.getName());

                            // Now you can use the factory to mount map endpoints to Vert.x handlers
                            factory.addHandlerByOperationId(method.getName(), routingContext -> {
                                try {
                                    SubjectApiController instance = this;
                                    //instance.getClass().getDeclaredMethod(method.getName(), RoutingContext.class).invoke(routingContext);
                                    getClass().getDeclaredMethod(method.getName(), RoutingContext.class).invoke(this, routingContext);
                                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                    logger.error(e.getLocalizedMessage());
                                    logger.error(Arrays.toString(e.getStackTrace()));
                                }
                            });

                            // Add a failure handler
                            factory.addFailureHandlerByOperationId(method.getName(), this::failureHandler);
                        });

                        // Add a security handler
                        factory.addSecurityHandler("cookieAuth", this::cookiesecurityHandler);

                        // Before router creation you can enable/disable various router factory behaviours
                        RouterFactoryOptions factoryOptions = new RouterFactoryOptions()
                                .setMountValidationFailureHandler(true) // Disable mounting of dedicated validation failure handler
                                .setMountResponseContentTypeHandler(true) // Mount ResponseContentTypeHandler automatically
                                .setMountNotImplementedHandler(true);

                        // Now you have to generate the router
                        Router router = factory.setOptions(factoryOptions).getRouter();

                        //Mount router into main router
                        abyssRouter.mountSubRouter(mountPoint, router);

                        logger.trace("generated router : " + router.getRoutes().toString());
                        logger.trace("Abyss router : " + abyssRouter.getRoutes().toString());

                    } else {
                        logger.error("OpenAPI3RouterFactory creation failed, cause: " + ar.cause());
                        throw new RuntimeException("OpenAPI3RouterFactory creation failed, cause: " + ar.cause());
                    }
                }
        );
    }

    @AbyssApiOperationHandler
    public void getSubjects(RoutingContext routingContext) {
        logger.info("getSubjects() invoked");

        // Get the parsed parameters
        //RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        //JsonObject subjects = params.body().getJsonObject();

        JsonObject subjects = new JsonObject().put("path", "getSubjects invoked");
        if (routingContext.user() != null) {
            subjects.put("user", routingContext.user().toString());
            subjects.put("user.principal", routingContext.user().principal().toString());
        }
/*
        if (1 == 1) {
            throwApiException(routingContext, BadRequest400Exception.class, "test exception", "very detailed message");
            return;
        }
*/

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

}
