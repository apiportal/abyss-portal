package com.verapi.portal.handler;

import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivateAccount extends PortalHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(ActivateAccount.class);

    private final JDBCClient jdbcClient;

    private Integer tokenId;

    public ActivateAccount(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("ActivateAccount.handle invoked..");

        String token = routingContext.request().getParam("v");
        logger.info("Received token:" + token);

        String path = routingContext.normalisedPath();
        logger.info("Received path:" + path);

        //TODO: Get Stored Token Info
        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT * FROM portalschema.SUBJECT_ACTIVATION WHERE TOKEN = ?", new JsonArray().add(token)))
                        .flatMap(resultSet -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                logger.info("token NOT found...");
                                return Single.error(new Exception("Token not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                logger.info("Token found:" + row.encodePrettily());

                                if (row.getInteger("is_deleted") == 1) {
                                    logger.error("Received Token is deleted");
                                    return Single.error(new Exception("Token does not exist in our records. Please request a new token."));
                                }

                                if (!(row.getString("token_type","").equals(Constants.ACTIVATION_TOKEN))) {
                                    logger.error("Received Token Type does not match: " +  row.getString("token_type","NULL"));
                                    return Single.error(new Exception("Right token does not exist in our records. Please request a new token."));
                                }

                                tokenId = row.getInteger("id");

                                AuthenticationInfo authInfo = new AuthenticationInfo(
                                        row.getString("token"),
                                        row.getString("nonce"),
                                        row.getInstant("expire_date"),
                                        row.getString("user_data"));

                                Token tokenValidator = new Token();

                                AuthenticationInfo authResult = tokenValidator.validateToken(token, authInfo);

                                if (authResult.isValid()) {
                                    logger.info("Received Token is valid.");
                                    return Single.just(row);
                                } else {
                                    logger.error("Received Token is NOT valid: " + authResult.getResultText());
                                    return Single.error(new Exception("Token is not valid. Please request a new token."));
                                }

                            } else {
                                logger.info("Multiple tokens found...");
                                return Single.error(new Exception("Valid token is not found in our records"));
                            }
                        })
                        .flatMap(row -> {
                            logger.info("Activate Account - Updating Subject with id:[" + row.getInteger("subject_id") + "] -> " + row.encodePrettily());
                            return resConn.rxUpdateWithParams("UPDATE portalschema.subject SET " +
                                                    "updated = now()," +
                                                    "crud_subject_id = ?," +
                                                    "is_activated = 1" +
                                                    " WHERE " +
                                                    "id = ?;",
                                            new JsonArray()
                                                    .add(1)
                                                    .add(row.getInteger("subject_id")));
                                }
                        )
                        .flatMap(updateResult -> {
                            logger.info("Activate Account - Updating Subject... Number of rows updated:" + updateResult.getUpdated());
                            //logger.info("Activate Account - Subject Update Result information:" + updateResult.getKeys().encodePrettily());
                            logger.info("Activate Account - Updating Subject Activation...");
                            if (updateResult.getUpdated() == 1) {
                                return resConn.rxUpdateWithParams("UPDATE portalschema.subject_activation SET " +
                                                "deleted = now()," +
                                                "crud_subject_id = ?," +
                                                "is_deleted = 1" +
                                                " WHERE " +
                                                "id = ?;",
                                        new JsonArray()
                                                .add(1)
                                                .add(tokenId));
                            } else {
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> {
                            if (updateResult.getUpdated() == 1) {
                                logger.info("Activate Account - Subject Activation Update Result information:" + updateResult.getKeys().encodePrettily());
                                return resConn.rxCommit().toSingleDefault(true);
                            } else {
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }

                        })

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess(succ -> {
                            logger.info("Activate Account: User record is activated and Token is deleted. Both persisted successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.info("Subscription to ActivateAccount successful:" + result);
                    generateResponse(routingContext, logger, 200, "Activation Successful!", "Welcome to API Portal.", "", "" );
                    //TODO: Send email to user
                }, t -> {
                    logger.error("ActivateAccount Error", t);
                    generateResponse(routingContext, logger, 401, "Activation Failed!", t.getLocalizedMessage(), "", "" );

                }
        );


        //TODO: Different action for different tokens and paths
/*        if (path.contains(Constants.ACTIVATION_PATH)) {

            //TODO: Validate Token

            //TODO: Update User Activated

        } else if (path.contains(Constants.RESET_PASSWORD_PATH)) {

            //TODO: Validate Token

            //TODO: Route to Reset Password Page as authenticated

        } else {

        }
*/
        //TODO: mark token as deleted

    }
}
