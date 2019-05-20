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

package com.verapi.portal.controller;

import com.verapi.abyss.common.Constants;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.OrganizationService;
import com.verapi.portal.service.idam.SubjectOrganizationService;
import com.verapi.portal.service.idam.SubjectService;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@AbyssController(routePathGET = "login", routePathPOST = "login-auth", htmlTemplateFile = "login.html", isPublic = true)
public class LoginPortalController extends AbstractPortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginPortalController.class);

    public LoginPortalController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        LOGGER.trace("LoginPortalController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.trace("LoginPortalController.handle invoked..");

        String username = routingContext.request().getFormAttribute("username");
        String password = routingContext.request().getFormAttribute("password");

        LOGGER.debug("Received user: {}", username);

        JsonObject creds = new JsonObject()
                .put("username", username)
                .put("password", password);

        authProvider.authenticate(creds, (AsyncResult<User> authResult) -> {
            if (authResult.succeeded()) {
                try {

                    //TODO: Check if password has expired and force change

                    SubjectService subjectService = new SubjectService(routingContext.vertx());

                    Single<JsonObject> apiResponse = subjectService.initJDBCClient()
                            .flatMap(jdbcClient -> subjectService.findByName(username))
                            .flatMap((ResultSet result) -> {
                                //result.toJson().getValue("rows")
                                LOGGER.trace(result.toJson().encodePrettily());
                                return Single.just(result.getRows().get(0));
                            });

                    apiResponse.subscribe((JsonObject resp) -> {
                                LOGGER.trace("LoginPortalController.handle() subjectService.findBySubjectName replied successfully {}", resp.encodePrettily());
                                User user = authResult.result();
                                String userUUID = resp.getString("uuid");
                                String displayName = resp.getString("displayname");
                                user.principal().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID);
                                routingContext.setUser(user); //TODO: Check context. Is this usefull? Should it be vertx context?
                                routingContext.session().regenerateId();
                                routingContext.session().destroy();
                                routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME
                                        , user.principal().getString("username"));
                                routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID);
                                routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_DISPLAY_NAME_SESSION_VARIABLE_NAME, displayName);
                                routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME, userUUID)); //TODO: Remove for OWASP Compliance
//                                        .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                                LOGGER.debug("Logged in user: {}", user.principal().encodePrettily());
                                routingContext.put("username", user.principal().getString("username"));
                                //redirect(routingContext, Constants.ABYSS_ROOT + "/index");

                                // --------- getOrganizationListOfSubject(userUUID);--------------
                                SubjectOrganizationService subjectOrganizationService = new SubjectOrganizationService(routingContext.vertx());

                                subjectOrganizationService.initJDBCClient()
                                        .flatMap(jdbcClient -> subjectOrganizationService.findAll(new ApiFilterQuery()
                                                .setFilterQuery(SubjectOrganizationService.FILTER_BY_SUBJECT)
                                                .setFilterQueryParams(new JsonArray().add(userUUID)))
                                        )
                                        .flatMap((ResultSet resultSet) -> {
                                            LOGGER.trace(resultSet.toJson().encodePrettily());
                                            return Single.just(resultSet);
                                        })
                                        .flatMap((ResultSet userOrganizations) -> {
                                            if (userOrganizations.getNumRows() == 0) {
                                                return Single.just(new ArrayList<JsonObject>());
                                            } else {
                                                OrganizationService organizationService = new OrganizationService(routingContext.vertx());

                                                return organizationService.initJDBCClient()
                                                        .flatMap((JDBCClient jdbcClient) -> {

                                                            Observable<JsonObject> observable = Observable.fromIterable(userOrganizations.getRows());

                                                            return observable
                                                                    .flatMap((JsonObject entries) -> organizationService
                                                                            .findById(UUID.fromString(entries.getString("organizationrefid"))).toObservable())
                                                                    .flatMap((ResultSet resultSet) -> {

                                                                        //userOrganizationArray.add(
                                                                        return Observable.just(new JsonObject()
                                                                                .put("uuid", resultSet.getRows().get(0).getString("uuid"))
                                                                                .put("name", resultSet.getRows().get(0).getString("name"))
                                                                        );
                                                                    }).toList();
                                                        });
                                            }
                                        })
                                        .subscribe((List<JsonObject> jsonObjects) -> {
                                                    if (jsonObjects.isEmpty()) {
                                                        redirect(routingContext, Constants.ABYSS_ROOT + "/create-organization"); //Render src\main\resources\webroot\create-organization.html
                                                    } else {
                                                        JsonArray jsonArray = new JsonArray(jsonObjects);
                                                        LOGGER.trace("LoginPortalController.handle() findByIdResult.subscribe result: {}", jsonArray);
                                                        routingContext.session().put("userOrganizationArray", jsonArray);
                                                        redirect(routingContext, Constants.ABYSS_ROOT + "/select-organization"); //Render src\main\resources\webroot\select-organization.html

                                                    }
                                                }
                                                , (Throwable throwable) -> {
                                                    LOGGER.error("LoginPortalController.handle() findByIdResult.subscribe replied error {} | {}: ", throwable.getLocalizedMessage(), throwable.getStackTrace());
                                                });

                            },
                            throwable -> LOGGER.error("LoginPortalController.handle() subjectService.findBySubjectName replied error {} | {}: "
                                    , throwable.getLocalizedMessage(), throwable.getStackTrace()));
                } catch (Exception e) {
                    LOGGER.error("LoginPortalController.handle() subjectService error : {}", Arrays.toString(e.getStackTrace()));
                    routingContext.fail(500);
                }
            } else {
                routingContext.fail(401);
            }
        });

        super.handle(routingContext);
    }
}
