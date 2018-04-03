package com.verapi.portal.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.postgresql.core.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;

public class Signup implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(Signup.class);

    private final JDBCClient jdbcClient;

    private final JDBCAuth authProvider;

    public Signup(JDBCAuth authProvider, JDBCClient jdbcClient) {
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("Signup.handle invoked..");

        /*
        firstname:Ökkeş
        lastname:Tutan
        username:okkes.tutan
        email:okkes.tutan69@example.com
        password:pwd
        password2:pwd
        isAgreedToTerms:on
         */

        String firstname = routingContext.request().getFormAttribute("firstname");
        String lastname = routingContext.request().getFormAttribute("lastname");
        String username = routingContext.request().getFormAttribute("username");
        String email = routingContext.request().getFormAttribute("email");
        String password = routingContext.request().getFormAttribute("password");
        //TODO: should password consistency check be performed @FE or @BE or BOTH?
        String password2 = routingContext.request().getFormAttribute("password2");
        String isAgreedToTerms = routingContext.request().getFormAttribute("isAgreedToTerms");

        //TODO: OWASP Validate

        logger.info("Received firstname:" + firstname);
        logger.info("Received lastname:" + lastname);
        logger.info("Received user:" + username);
        logger.info("Received email:" + email);
        logger.info("Received pass:" + password);
        logger.info("Received pass2:" + password2);
        logger.info("Received isAgreedToTerms:" + isAgreedToTerms);

        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn

                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT * FROM portalschema.USER WHERE USERNAME = ?", new JsonArray().add(email))
                                .map(resultSet -> {
                                    if (resultSet.getNumRows() > 0) {
                                        logger.info("user found: " + resultSet.toJson().encodePrettily());
                                    } else {
                                        logger.info("user NOT found, creating user and activation records...");
                                        String salt = authProvider.generateSalt();
                                        String hash = authProvider.computeHash(password, salt);
                                        // save user to the database
                                        resConn.rxUpdateWithParams("INSERT INTO portalschema.user VALUES (?, ?, ?)", new JsonArray().add(email).add(hash).add(salt))
                                                .map(updateResult -> {
                                                            logger.info("user created successfully: " + updateResult.getKeys().encodePrettily());

                                                            //Generate and Persist Activation Token
                                                            Token tokenGenerator = new Token();
                                                            AuthenticationInfo authInfo = null;
                                                            try {
                                                                authInfo = tokenGenerator.encodeToken(Config.getInstance().getConfigJsonObject().getInteger("one.hour.in.seconds"), email, routingContext.vertx().getDelegate());
                                                                logger.info("activation token is created successfully: " + authInfo.getToken());
                                                            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                                                                logger.error("tokenGenerator.encodeToken :" + e.getLocalizedMessage());
                                                            }
                                                            resConn.rxUpdateWithParams("INSERT INTO portalschema.user_activation (username, expire_date, token) VALUES (?, ?, ?)", new JsonArray().add(email).add(authInfo.getExpireDate()).add(authInfo.getToken()))
                                                                    .map(updateResult1 -> {
                                                                        resConn.rxCommit().toSingleDefault(true).map(commit -> updateResult);
                                                                        return resultSet;
                                                                    });
                                                            return resultSet;
                                                        }
                                                );
                                    }
                                    return resultSet;
                                })
                                .onErrorResumeNext(ex -> resConn.rxRollback()
                                        .toSingleDefault(true)
                                        .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                        .flatMap(ignore -> Single.error(ex))
                                )
                        )
                        //.flatMap(logit -> logger.info("signup - connection autocommit is set to false").map(dolog -> resQuery))
                        //.flatMap().map(checkNumOfRows -> updateResult)
                        .doAfterSuccess(succ -> {
                            logger.info("activation token is created and persisted successfully");
                            //generateResponse(routingContext, 200, "Activation Code is sent to your email address", "Please check spam folder also...", "", "" );
                            //TODO: Send email to user
                        })
                        .doAfterTerminate(resConn::close)
        ).subscribe();

/*
		jdbcClient.getConnection(resConn -> {
			if (resConn.succeeded()) {

				SQLConnection connection = resConn.result();
				connection.setAutoCommit(false, resultHandler -> {
					if (resultHandler.succeeded()) {
						logger.info("signup - connection autocommit is set to false");
					} else {
						
					}
						
				});
				
				
				connection.queryWithParams("SELECT * FROM portalschema.USER WHERE USERNAME = ?", new JsonArray().add(email), resQuery -> {
					if (resQuery.succeeded()) {
						ResultSet rs = resQuery.result();
						// Do something with results
						if (rs.getNumRows() > 0) {
							connection.close();
							logger.info("user found: " + rs.toJson().encodePrettily());
							//TODO: Send response: this user exists 
							//TODO: Design a generic business error page with user error message
						} else {
							logger.info("user NOT found, creating user and activation records...");
							String salt = authProvider.generateSalt();
							String hash = authProvider.computeHash(password, salt);
							// save user to the database
							connection.updateWithParams("INSERT INTO portalschema.user VALUES (?, ?, ?)", new JsonArray().add(email).add(hash).add(salt), resUpdate -> {
								if (resUpdate.succeeded()) {
									logger.info("user created successfully: " + resUpdate.result().getKeys().encodePrettily());
									
									//Generate and Persist Activation Token
									Token tokenGenerator = new Token();
									AuthenticationInfo authInfo = null;
									try {
										authInfo = tokenGenerator.encodeToken(Config.getInstance().getConfigJsonObject().getInteger("one.hour.in.seconds"), email, routingContext.vertx());
										logger.info("activation token is created successfully: " + authInfo.getToken());
									} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
										logger.error("tokenGenerator.encodeToken :" + e.getLocalizedMessage());
									}
									connection.updateWithParams("INSERT INTO portalschema.user_activation (username, expire_date, token) VALUES (?, ?, ?)", new JsonArray().add(email).add(authInfo.getExpireDate()).add(authInfo.getToken()), resUpdateActivation -> {
										if (resUpdate.succeeded()) {
											connection.commit(handler -> {
												if (handler.failed()) {
													connection.close();
													throw new RuntimeException(handler.cause());
												}
											});
											logger.info("activation token is created and persisted successfully");
											//generateResponse(routingContext, 200, "Activation Code is sent to your email address", "Please check spam folder also...", "", "" );
											//TODO: Send email to user
											
											routingContext.session().put("isUserActivated", true);
											routingContext.response().putHeader("location", "/abyss/login").setStatusCode(302).end();
											
										} else {
											logger.error("user_activation create error: " + resUpdateActivation.cause().getLocalizedMessage());
											connection.rollback(handler -> {
												if (handler.failed()) {
													connection.close();
													throw new RuntimeException(handler.cause());
												}
											});
											connection.close();
											resUpdateActivation.failed();
										}
									});
									
								} else {
									logger.error("user create error: " + resUpdate.cause().getLocalizedMessage());
									connection.rollback(handler -> {
										if (handler.failed()) {
											connection.close();
											throw new RuntimeException(handler.cause());
										}
									});
									connection.close();
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
*/


    }

    public void pageRender(RoutingContext routingContext) {
        logger.info("Signup.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(routingContext, "webroot/", "signup.html", res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    private void generateResponse(RoutingContext context, int statusCode, String message1, String message2, String message3, String message4) {

        logger.info("generateResponse invoked...");

        //Use user's session for storage 
        context.session().put(Constants.HTTP_STATUSCODE, statusCode);
        context.session().put(Constants.HTTP_URL, message2);
        context.session().put(Constants.HTTP_ERRORMESSAGE, message1);
        context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, message3);

        context.response().putHeader("location", "/httperror").setStatusCode(302).end();
    }

}
