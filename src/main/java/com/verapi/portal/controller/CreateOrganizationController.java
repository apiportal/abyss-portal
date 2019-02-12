/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Faik SAGLAR <faik.saglar@verapi.com>, 7 2018
 *
 */

package com.verapi.portal.controller;

import com.verapi.abyss.common.Config;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@AbyssController(routePathGET = "create-organization", routePathPOST = "create-organization", htmlTemplateFile = "create-organization.html", isPublic = true)
public class CreateOrganizationController extends PortalAbstractController {

    private static Logger logger = LoggerFactory.getLogger(CreateOrganizationController.class);

//    private String name;
//    private String description;
//    private String url;

    private String organizationUuid;

    public CreateOrganizationController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.trace("CreateOrganizationController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.trace("CreateOrganizationController.handle invoked...");

        String name = routingContext.request().getFormAttribute("name");
        String description = routingContext.request().getFormAttribute("description");
        String url = routingContext.request().getFormAttribute("url");

        logger.trace("Received name:" + name);
        logger.trace("Received description:" + description);
        logger.trace("Received url:" + url);

        String userUuid = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

        OrganizationService organizationService = new OrganizationService(routingContext.vertx());

        SubjectOrganizationService subjectOrganizationService = new SubjectOrganizationService(routingContext.vertx());

        organizationService.initJDBCClient()
                .flatMap(jdbcClient1 -> {
                    return organizationService.insertAll(new JsonArray().add(new JsonObject()
                            .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                            .put("crudsubjectid", userUuid)
                            .put("name", name)
                            .put("description", description)
                            .put("url", url)));

                })
                .flatMap(jsonObjects -> {
                    logger.trace("CreateOrganizationController - organizationService.insertAll successfull: {}", jsonObjects.get(0).encodePrettily());

                    organizationUuid = jsonObjects.get(0).getString("uuid");

                    return subjectOrganizationService.initJDBCClient();
                })
                .flatMap(jdbcClient1 -> {
                  return subjectOrganizationService.insertAll(new JsonArray().add(new JsonObject()
                          .put("organizationid", organizationUuid)
                          .put("crudsubjectid",userUuid)
                          .put("subjectid",userUuid)
                          .put("organizationrefid", organizationUuid)));
                })
                .flatMap(jsonObjects -> {
                    logger.trace("CreateOrganizationController - subjectOrganizationService.insertAll successfull: {}", jsonObjects.get(0).encodePrettily());

                    return Single.just(jsonObjects);

                })
//                .doOnError(throwable -> {
//                    logger.error("exception occured " + throwable.getLocalizedMessage());
//                    logger.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
//                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());

                .subscribe(jsonObjects -> {
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
                        logger.error("SelectOrganizationController - POST handler : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
                        showTrxResult(routingContext, logger, 400, "Organization Selection Failed!", e.getLocalizedMessage(), "");
                    }

                }, throwable -> {
                    logger.error("CreateOrganizationController.handle() error {} | {}: ", throwable.getLocalizedMessage(), throwable.getStackTrace());
                });


    }
}
