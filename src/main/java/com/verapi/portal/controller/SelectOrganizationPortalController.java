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
public class SelectOrganizationPortalController extends AbstractPortalController {
    private static final String USER_ORGANIZATION_ARRAY = "userOrganizationArray";
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectOrganizationPortalController.class);

    public SelectOrganizationPortalController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        LOGGER.trace("SelectOrganizationPortalController.defaultGetHandler invoked...");

        LOGGER.trace("userOrganizationList: {}", (JsonArray) routingContext.session().get(USER_ORGANIZATION_ARRAY));

        class OrganizationTuple {
            public String uuid;
            public String name;

            private OrganizationTuple(String uuid, String name) {
                this.uuid = uuid;
                this.name = name;
            }
        }

        ArrayList<OrganizationTuple> orgs = new ArrayList<>();

        JsonArray jsonArray = routingContext.session().get(USER_ORGANIZATION_ARRAY);

        jsonArray.forEach((Object o) -> {
            JsonObject j = (JsonObject) o;
            orgs.add(new OrganizationTuple(j.getString("uuid"), j.getString("name")));
        });

        LOGGER.trace("{}: {}", USER_ORGANIZATION_ARRAY, orgs);

        routingContext.put(USER_ORGANIZATION_ARRAY, orgs);

        JsonObject context = new JsonObject().put(USER_ORGANIZATION_ARRAY, orgs);

        renderTemplate(routingContext, context, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.trace("SelectOrganizationPortalController.handle invoked..");

        String compositeValue = routingContext.request().getFormAttribute("orgid");
        LOGGER.trace("Received orgid: {}", compositeValue);

        String[] values = compositeValue.split("\\|");
        LOGGER.trace("values: {}, {}", values[0], values[1]);
        if (values.length == 2) {

            try {
                //Url Encode for cookie compliance
                String userLoginOrganizationName = URLEncoder.encode(values[1], "UTF-8");
                String userLoginOrganizationUUID = URLEncoder.encode(values[0], "UTF-8");

                routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName);
                routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID);

                routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName));
//                        .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));
                routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID));
//                        .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                redirect(routingContext, Constants.ABYSS_ROOT + "/index");

            } catch (UnsupportedEncodingException e) {
                LOGGER.error("SelectOrganizationPortalController - POST handler : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
                showTrxResult(routingContext, LOGGER, 400, "Organization Selection Failed!", e.getLocalizedMessage(), "");
            }

        } else {
            routingContext.fail(400);
        }
    }
}
