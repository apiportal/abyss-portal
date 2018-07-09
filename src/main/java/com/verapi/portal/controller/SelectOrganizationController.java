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

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
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
import java.util.ArrayList;

@AbyssController(routePathGET = "select-organization", routePathPOST = "select-organization", htmlTemplateFile = "select-organization.html", isPublic = true)
public class SelectOrganizationController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(SelectOrganizationController.class);

    public SelectOrganizationController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.trace("SelectOrganizationController.defaultGetHandler invoked...");

        logger.trace("userOrganizationList: {}", (JsonArray)routingContext.session().get("userOrganizationArray"));

        class OrganizationTuple {
            public String uuid;
            public String name;

            public OrganizationTuple (String uuid, String name) {
                this.uuid = uuid;
                this.name=name;
            }
        }

        ArrayList<OrganizationTuple> orgs = new ArrayList<OrganizationTuple>();

        JsonArray jsonArray = (JsonArray)routingContext.session().get("userOrganizationArray");

        jsonArray.forEach(o -> {
            JsonObject j = (JsonObject) o;
            orgs.add(new OrganizationTuple(j.getString("uuid"), j.getString("name")));
        });

        logger.trace("userOrganizationArray: {}", orgs);

        routingContext.put("userOrganizationArray", orgs);

        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.trace("SelectOrganizationController.handle invoked..");

        String compositeValue = routingContext.request().getFormAttribute("orgid");
        logger.trace("Received orgid:" + compositeValue);

        String[] values = compositeValue.split("\\|");
        logger.trace("values:" + values[0] + "," + values[1]);
        if ((values!= null) && (values.length==2)) {

            try {
                //Url Encode for cookie compliance
                String userLoginOrganizationName = URLEncoder.encode(values[1], "UTF-8");
                String userLoginOrganizationUUID = URLEncoder.encode(values[0], "UTF-8");

                routingContext.session().put("user.login.organization.name", userLoginOrganizationName);
                routingContext.session().put("user.login.organization.uuid", userLoginOrganizationUUID);

                routingContext.addCookie(Cookie.cookie("abyss.login.organization.name", userLoginOrganizationName));
//                        .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));
                routingContext.addCookie(Cookie.cookie("abyss.login.organization.uuid", userLoginOrganizationUUID));
//                        .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                redirect(routingContext, Constants.ABYSS_ROOT + "/index");

            } catch (UnsupportedEncodingException e) {
                logger.error("SelectOrganizationController - POST handler : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
                showTrxResult(routingContext, logger, 400, "Organization Selection Failed!", e.getLocalizedMessage(), "");
            }

        } else {
            routingContext.fail(400);
        }
    }
}
