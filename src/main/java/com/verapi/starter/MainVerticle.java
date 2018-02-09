package com.verapi.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.impl.FormLoginHandlerImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class MainVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    //private AuthProvider auth;

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

    	AuthProvider auth = ShiroAuth.create(vertx, new ShiroAuthOptions()
                .setType(ShiroAuthRealmType.PROPERTIES)
                .setConfig(new JsonObject()
                        .put("properties_path", "classpath:users.properties")));
        

        
        

        // To simplify the development of the web components we use a Router to route all HTTP requests
        // to organize our code in a reusable way.
        final Router router = Router.router(vertx);

        //log HTTP requests
        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

        //firstly install cookie handler
        //A handler which decodes cookies from the request, makes them available in the RoutingContext and writes them back in the response
        router.route().handler(CookieHandler.create());

        //secondly install body handler
        //A handler which gathers the entire request body and sets it on the RoutingContext
        //It also handles HTTP file uploads and can be used to limit body sizes
        router.route().handler(BodyHandler.create());

        //thirdly install session handler
        //A handler that maintains a Session for each browser session
        //The session is available on the routing context with RoutingContext.session()
        //The session handler requires a CookieHandler to be on the routing chain before it
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        //This handler should be used if you want to store the User object in the Session so it's available between different requests, without you having re-authenticate each time
        //It requires that the session handler is already present on previous matching routes
        //It requires an Auth provider so, if the user is deserialized from a clustered session it knows which Auth provider to associate the session with.
        router.route().handler(UserSessionHandler.create(auth));

        //An auth handler that's used to handle auth (provided by Shiro Auth prodiver) by redirecting user to a custom login page
        AuthHandler authHandler = RedirectAuthHandler.create(auth, "/full-width-light/login");

        //install authHandler for all routes where authentication is required
        //router.route("/full-width-light/").handler(authHandler);
        router.route("/full-width-light/index").handler(authHandler);

        // Entry point to the application, this will render a custom Thymeleaf template
        router.get("/full-width-light/login").handler(this::loginHandler);

        router.post("/login-auth").handler(new SpecialLoginHandler(auth));
        
        router.post("/login-auth2").handler(FormLoginHandler.create(auth));

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

    
    class SpecialLoginHandler implements FormLoginHandler {

    	//private final Logger log = LoggerFactory.getLogger(FormLoginHandlerImpl.class);

    	private final AuthProvider authProvider;
    	
		public SpecialLoginHandler(AuthProvider authProvider) {
			this.authProvider = authProvider;
		}


		/* (non-Javadoc)
		 * @see io.vertx.core.Handler#handle(java.lang.Object)
		 */
		@Override
		public void handle(RoutingContext context) {
			logger.info("loginAuthHandler invoked..");
			
			String username = context.request().getFormAttribute("username");
			String password = context.request().getFormAttribute("password");
			
			logger.info("Received user:"+username+":hdr:"+context.request().getHeader("username"));
			logger.info("Received pass:"+password+":hdr:"+context.request().getHeader("password"));
			
            JsonObject creds = new JsonObject()
                    .put("username", username)
                    .put("password", password);
            
            //Direct Method
/*            Subject currentUser = SecurityUtils.getSubject();
            
            UsernamePasswordToken userToken = new UsernamePasswordToken(username, password, "127.0.0.1");
            
            currentUser.login(userToken);
            logger.info("User [" + username + "] logged in successfully.");

            logger.info("Logged in user: " + ((User)currentUser.getPrincipal()).principal().encodePrettily());
*/            
            //Previous Method
            authProvider.authenticate(creds, authResult -> {
                if (authResult.succeeded()) {
                    User user = authResult.result();
                    logger.info("Logged in user: " + user.principal().encodePrettily());
                } else {
                    context.fail(401);
                }
            });
		}


		/* (non-Javadoc)
		 * @see io.vertx.ext.web.handler.FormLoginHandler#setDirectLoggedInOKURL(java.lang.String)
		 */
		@Override
		public FormLoginHandler setDirectLoggedInOKURL(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}


		/* (non-Javadoc)
		 * @see io.vertx.ext.web.handler.FormLoginHandler#setPasswordParam(java.lang.String)
		 */
		@Override
		public FormLoginHandler setPasswordParam(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}


		/* (non-Javadoc)
		 * @see io.vertx.ext.web.handler.FormLoginHandler#setReturnURLParam(java.lang.String)
		 */
		@Override
		public FormLoginHandler setReturnURLParam(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}


		/* (non-Javadoc)
		 * @see io.vertx.ext.web.handler.FormLoginHandler#setUsernameParam(java.lang.String)
		 */
		@Override
		public FormLoginHandler setUsernameParam(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

    	
    }
    
}
