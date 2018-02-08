package com.verapi.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class MainVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

/*
  public static void main(String... args) {

    ClusterManager mgr = new HazelcastClusterManager(new Config());
    logger.info("HazelcastClusterManager created");

    VertxOptions options = new VertxOptions()
      .setHAEnabled(true);
    logger.info("VertxOptions created");

    Vertx.clusteredVertx(options, res -> {
        if (res.succeeded()) {
          res.result().deployVerticle(MainVerticle.class.getName(), new DeploymentOptions().setHa(true));
          logger.info("deployVerticle completed..." + res.succeeded());
        } else {
          logger.error("deployVerticle failed..." + res.cause());
        }
      }
    );
    logger.info("VertxOptions created");
  }
*/

    @Override
    public void start(Future<Void> start) {

        // To simplify the development of the web components we use a Router to route all HTTP requests
        // to organize our code in a reusable way.
        final Router router = Router.router(vertx);

        //log HTTP requests
        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

        // Entry point to the application, this will render a custom Thymeleaf template.
        router.route("/full-width-light/login").handler(this::loginHandler);

        router.route()
                .handler(StaticHandler.create());
        //.failureHandler(this::failureHandler);

        router.route().failureHandler(this::failureHandler);
    /*router.route().failureHandler(failureRoutingContext -> {

      int statusCode = failureRoutingContext.statusCode();

      // Status code will be 500 for the RuntimeException or 403 for the other failure
      HttpServerResponse response = failureRoutingContext.response();
      response.setStatusCode(statusCode).end("Sorry! Not today");

    });*/

        router.get("/full-width-light/404").handler(this::p404Handler);

        router.get("/full-width-light/500").handler(this::p500Handler);

        router.route().handler(ctx -> {
            ctx.fail(404);
        });

        logger.info("starting http server");
        HttpServerOptions httpServerOptions = new HttpServerOptions();

        logger.warn("http server is running in plaintext mode. Enable SSL in config for production deployments.");
        vertx.createHttpServer(httpServerOptions.setCompressionSupported(true))
                .requestHandler(router::accept)
                .listen(8081, result -> {
                    if (result.succeeded()) {
                        logger.info("http server started..." + result.succeeded());
                        start.complete();
                    } else {
                        logger.error("http server starting failed..." + result.cause());
                        start.fail(result.cause());
                    }
                });


/*
    ClusterManager mgr = new HazelcastClusterManager(new Config());

    VertxOptions options = new VertxOptions()
      .setClusterManager(mgr)
      .setClusterHost("192.168.21.99")
      .setClusterPort(8081)
      .setClustered(true)
      .setHAEnabled(true);

    logger.info("Vertx.clusteredVertx is being invoked..");

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        this.vertx = res.result();

        // start a HTTP web server on port 8080
        ///vertx.createHttpServer().requestHandler(router::accept).listen(8081);
        logger.info("http server started..." + res.succeeded());

      } else {
        logger.error("http server starting failed..." + res.cause());
      }
    });
*/
    }

    private void loginHandler(RoutingContext context) {

        logger.info("login handler invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        // we define a hardcoded title for our application
        context.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(context, "src/main/resources/webroot/full-width-light/", "login.html", res -> {
            if (res.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().end(res.result());
            } else {
                context.fail(res.cause());
            }
        });
    }

    private void p404Handler(RoutingContext context) {
        logger.info("p404Handler invoked..");
        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();
        // we define a hardcoded title for our application
        context.put("signin", "404 Error");
        // and now delegate to the engine to render it.
        engine.render(context, "src/main/resources/webroot/full-width-light/", "404.html", res -> {
            if (res.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().setStatusCode(404);
                context.response().end(res.result());
            } else {
                context.fail(res.cause());
            }
        });
    }

    private void p500Handler(RoutingContext context) {
        logger.info("p500Handler invoked..");
        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        // we define a hardcoded title for our application
        context.put("signin", "500 Error");
        // and now delegate to the engine to render it.
        engine.render(context, "src/main/resources/webroot/full-width-light/", "500.html", res -> {
            if (res.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().setStatusCode(500);
                context.response().end(res.result());
            } else {
                context.fail(res.cause());
            }
        });
    }

    private void failureHandler(RoutingContext context) {
        logger.info("failureHandler invoked..");
        logger.debug(context.toString());
        logger.info(context.toString());
        if (context.statusCode() == 404) {
            context.response().putHeader("location", "/full-width-light/404").setStatusCode(302).end();
        } else {
            context.reroute("/full-width-light/500");
        }
    }

}
