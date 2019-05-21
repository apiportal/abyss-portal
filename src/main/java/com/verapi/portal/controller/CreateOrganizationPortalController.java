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
import com.verapi.portal.service.idam.OrganizationService;
import com.verapi.portal.service.idam.SubjectOrganizationService;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@AbyssController(routePathGET = "create-organization", routePathPOST = "create-organization", htmlTemplateFile = "create-organization.html", isPublic = true)
public class CreateOrganizationPortalController extends AbstractPortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrganizationPortalController.class);

//    private String name;
//    private String description;
//    private String url;

    private String organizationUuid;

    public CreateOrganizationPortalController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        LOGGER.trace("CreateOrganizationPortalController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.trace("CreateOrganizationPortalController.handle invoked...");

        String name = routingContext.request().getFormAttribute("name");
        String description = routingContext.request().getFormAttribute("description");
        String url = routingContext.request().getFormAttribute("url");

        LOGGER.trace("Received name: {}", name);
        LOGGER.trace("Received description: {}", description);
        LOGGER.trace("Received url: {}", url);

        String userUuid = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

        OrganizationService organizationService = new OrganizationService(routingContext.vertx());

        SubjectOrganizationService subjectOrganizationService = new SubjectOrganizationService(routingContext.vertx());

        organizationService.initJDBCClient()
                .flatMap((JDBCClient jdbcClient1) ->
                        organizationService.insertAll(new JsonArray().add(new JsonObject()
                                .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                                .put("crudsubjectid", userUuid)
                                .put("name", name)
                                .put("description", description)
                                .put("url", url)
                                .put("isactive", Boolean.TRUE)
                                .put("picture", "")))

                )
                .flatMap((List<JsonObject> jsonObjects) -> {
                    LOGGER.trace("CreateOrganizationPortalController - organizationService.insertAll successfull: {}", jsonObjects.get(0).encodePrettily());

                    organizationUuid = jsonObjects.get(0).getString("uuid");

                    return subjectOrganizationService.initJDBCClient();
                })
                .flatMap((JDBCClient jdbcClient1) ->
                        subjectOrganizationService.insertAll(new JsonArray().add(new JsonObject()
                                .put("organizationid", organizationUuid)
                                .put("crudsubjectid", userUuid)
                                .put("subjectid", userUuid)
                                .put("organizationrefid", organizationUuid)
                                .put("isowner", Boolean.TRUE)
                                .put("isactive", Boolean.TRUE)))
                )
                .flatMap((List<JsonObject> jsonObjects) -> {
                    LOGGER.trace("CreateOrganizationPortalController - subjectOrganizationService.insertAll successfull: {}", jsonObjects.get(0).encodePrettily());

                    return Single.just(jsonObjects);

                })
//                .doOnError(throwable -> {
//                    LOGGER.error("exception occured " + throwable.getLocalizedMessage());
//                    LOGGER.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
//                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());

                .subscribe((List<JsonObject> jsonObjects) -> {
                    try {
                        //Url Encode for cookie compliance
                        String userLoginOrganizationName = URLEncoder.encode(name, "UTF-8");
                        String userLoginOrganizationUUID = URLEncoder.encode(organizationUuid, "UTF-8");

                        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName);
                        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID);

                        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName));
//                                .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60)); //TODO: Remove Cookie at Session Timeout
                        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID));
//                                .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                        redirect(routingContext, Constants.ABYSS_ROOT + "/index");

                    } catch (UnsupportedEncodingException e) {
                        LOGGER.error("SelectOrganizationPortalController - POST handler : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
                        showTrxResult(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, "Organization Selection Failed!", e.getLocalizedMessage(), "");
                    }

                }, (Throwable throwable) -> LOGGER.error("CreateOrganizationPortalController.handle() error {} | {}: "
                        , throwable.getLocalizedMessage(), throwable.getStackTrace()));


    }
}
