package com.verapi.starter;

import com.verapi.starter.handler.Index;
import com.verapi.starter.handler.Login;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class MainVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    
    private JDBCClient jdbcClient;
    
    private JDBCAuth authProvider;

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

    	/*
    	You can set the default search_path at the database level:

    		ALTER DATABASE <database_name> SET search_path TO schema1,schema2;
    	
    	Or at the user or role level:

    		ALTER ROLE <role_name> SET search_path TO schema1,schema2;
    		
    	Or add parameter to connection URL:
    	
    		https://jdbc.postgresql.org/documentation/head/connect.html
    		
    	*/
    	
    	JsonObject jdbcClientConfig = new JsonObject()
    			  .put("url", "jdbc:postgresql://192.168.10.40:5432/abyssportal?currentSchema=portalschema")
    			  .put("driver_class", "org.postgresql.Driver")
    			  .put("user", "postgres")//"abyssuser")
    			  .put("password", "postgres")//"User007")
    			  .put("max_pool_size", 30);    
    	
    	jdbcClient = JDBCClient.createShared(vertx, jdbcClientConfig);
    	logger.info("JDBCClient created... " + jdbcClient.toString() );

    	authProvider = JDBCAuth.create(vertx, jdbcClient);
    	
    	logger.info("JDBCAuthProvider created... " + authProvider.toString());
    	
    	//authProvider.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM portalschema.USER WHERE USERNAME = ?");
    	//authProvider.setPermissionsQuery("SELECT PERM FROM portalschema.ROLES_PERMS RP, portalschema.USER_ROLES UR WHERE UR.USERNAME = ? AND UR.ROLE = RP.ROLE");
    	//authProvider.setRolesQuery("SELECT ROLE FROM portalschema.USER_ROLES WHERE USERNAME = ?");
    	authProvider.setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx));
    	//authProvider.setNonces();
    	logger.info("JDBCAuthProvider configuration done... ");
    	
    	
    	
    	
        AuthProvider auth = ShiroAuth.create(vertx, new ShiroAuthOptions()
                .setType(ShiroAuthRealmType.PROPERTIES)
                .setConfig(new JsonObject()
                        .put("properties_path", "classpath:users.properties")));
        logger.info("AuthProvider created.. " + auth.toString());

        // To simplify the development of the web components we use a Router to route all HTTP requests
        // to organize our code in a reusable way.
        Router router = Router.router(vertx);

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
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx, "abyss.session")).setSessionCookieName("abyss.session"));

        //This handler should be used if you want to store the User object in the Session so it's available between different requests, without you having re-authenticate each time
        //It requires that the session handler is already present on previous matching routes
        //It requires an Auth provider so, if the user is deserialized from a clustered session it knows which Auth provider to associate the session with.
        router.route().handler(UserSessionHandler.create(auth));

        //An auth handler that's used to handle auth (provided by Shiro Auth prodiver) by redirecting user to a custom login page
        AuthHandler authHandler = RedirectAuthHandler.create(auth, "/full-width-light/login");

        router.get("/create_user").handler(this::createUser).failureHandler(this::failureHandler);
        
        //install authHandler for all routes where authentication is required
        //router.route("/full-width-light/").handler(authHandler);
        router.route("/full-width-light/index").handler(authHandler.addAuthority("okumaz")).failureHandler(this::failureHandler);

        // Entry point to the application, this will render a custom Thymeleaf template
        //router.get("/full-width-light/login").handler(this::loginHandler);
        Login login = new Login(auth);
        router.get("/full-width-light/login").handler(login::pageRender).failureHandler(this::failureHandler);
        router.post("/login-auth").handler(login).failureHandler(this::failureHandler);

        Index index = new Index(auth);
        router.get("/full-width-light/index").handler(index::pageRender).failureHandler(this::failureHandler);
        //router.post("/login-auth").handler(new SpecialLoginHandler(auth));

        //router.post("/login-auth2").handler(FormLoginHandler.create(auth));
        
        

        router.get("/img/*").handler(StaticHandler.create("/img").setWebRoot("webroot/img"));
        router.get("/vendors/*").handler(StaticHandler.create("/vendors").setWebRoot("webroot/vendors"));
        router.get("/full-width-light/dist/*").handler(StaticHandler.create("/full-width-light/dist").setWebRoot("webroot/full-width-light/dist"));
        //.failureHandler(this::failureHandler);

        //router.route().failureHandler(this::failureHandler);
    /*router.route().failureHandler(failureRoutingContext -> {

      int statusCode = failureRoutingContext.statusCode();

      // Status code will be 500 for the RuntimeException or 403 for the other failure
      HttpServerResponse response = failureRoutingContext.response();
      response.setStatusCode(statusCode).end("Sorry! Not today");

    });*/
        
        

        router.get("/full-width-light/404").handler(this::p404Handler).failureHandler(this::failureHandler);

        router.get("/full-width-light/500").handler(this::p500Handler).failureHandler(this::failureHandler);

        //only rendering page routings' failures shall be handled by using regex
        //The regex below will match any string, or line without a line break, not containing the (sub)string '.'
        router.routeWithRegex("^((?!\\.).)*$").failureHandler(this::failureHandler);

        router.route().handler(ctx -> {
            logger.info("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
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

	/**
	 * @param authProvider
	 */
	private void createUser(RoutingContext routingContext) {
		
        String username = routingContext.request().getParam("username");
        String password = routingContext.request().getParam("password");

        logger.info("Received user:" + username);
        logger.info("Received pass:" + password);

		
		jdbcClient.getConnection(resConn -> {
			if (resConn.succeeded()) {

				SQLConnection connection = resConn.result();
				
				connection.queryWithParams("SELECT * FROM USER WHERE USERNAME = ?", new JsonArray().add(username), resQuery -> {
					if (resQuery.succeeded()) {
						ResultSet rs = resQuery.result();
						// Do something with results
						if (rs.getNumRows() > 0) {
							logger.info("user found: " + rs.toJson().encodePrettily());
						} else {
							logger.info("user NOT found, creating ...");
							String salt = authProvider.generateSalt();
							String hash = authProvider.computeHash(password, salt);
							// save to the database
							connection.updateWithParams("INSERT INTO user VALUES (?, ?, ?)", new JsonArray().add(username).add(hash).add(salt), resUpdate -> {
								if (resUpdate.succeeded()) {
									logger.info("user created successfully");
								} else {
									logger.error("user create error: " + resUpdate.cause().getLocalizedMessage());
									resUpdate.failed();
								}
							});
						}
					} else {
						logger.error("SELECT user failed: " + resQuery.cause().getLocalizedMessage());
						connection.close();
						//jdbcClient.close();
					}
				});
			} else {
				// Failed to get connection - deal with it
				logger.error("JDBC getConnection failed: " + resConn.cause().getLocalizedMessage());
				resConn.failed();
				//jdbcClient.close();
			}
		});
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
            context.response().putHeader("location", "/full-width-light/500").setStatusCode(302).end();
        }
    }

	/* (non-Javadoc)
	 * @see io.vertx.core.AbstractVerticle#stop()
	 */
	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		super.stop();
		jdbcClient.close();
	}
}
