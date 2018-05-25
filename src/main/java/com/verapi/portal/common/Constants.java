/*
 *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *
 *  Written by Ismet Faik SAGLAR <faik.saglar@verapi.com>, December 2017
 */
package com.verapi.portal.common;

/**
 * @author faik.saglar
 * @author halil.ozkan
 */
public class Constants {

    private static final String ABYSS = "abyss";
    public static final String ABYSS_ROOT = "/"+ABYSS;
    public static final String ABYSS_PORTAL = "abyss-portal";
    public static final String HOST_PROTOCOL = "hostProtocol";
    public static final String HOST = "host";
    public static final String ISSANDBOX = "issandbox";

    public static final String BROWSER_SESSION_TIMEOUT = "browser.session.timeout";

    public static final String HTTP_SERVER_HOST = "http.server.host";
    public static final String HTTP_SERVER_PORT = "http.server.port";
    public static final String HTTP_PROXY_SERVER_PORT = "http.proxy.server.port";
    public static final String HTTP_SERVER_TIMEOUT = "http.server.timeout";

    public static final String HTTP_API_SERVER_HOST = "http.api.server.host";
    public static final String HTTP_API_SERVER_PORT = "http.api.server.port";
    public static final String HTTP_PROXY_API_SERVER_PORT = "http.proxy.api.server.port";
    public static final String HTTP_API_SERVER_TIMEOUT = "http.api.server.timeout";
    public static final String EB_API_SERVER_ADDRESS = "eb.api.server.address";

    public static final String HTTP_OPENAPI_SERVER_HOST = "http.openapi.server.host";
    public static final String HTTP_OPENAPI_SERVER_PORT = "http.openapi.server.port";
    public static final String HTTP_PROXY_OPENAPI_SERVER_PORT = "http.proxy.openapi.server.port";
    public static final String HTTP_OPENAPI_SERVER_TIMEOUT = "http.openapi.server.timeout";

    public static final String CONTEXT_FAILURE_MESSAGE = "context.failureMessage";
    public static final String HTTP_ERRORMESSAGE = "http.errorMessage";
    public static final String HTTP_URL = "http.url";
    public static final String HTTP_STATUSCODE = "http.statusCode";
    public static final String HTML_FAILURE = "failure.html";
    public static final String HTML_SUCCESS = "success.html";

    public static final String PORTAL_JDBC_URL = "portal.jdbc.url";
    public static final String PORTAL_JDBC_DRIVER_CLASS = "portal.jdbc.driver.class";
    public static final String PORTAL_DBUSER_NAME = "portal.dbuser.name";
    public static final String PORTAL_DBUSER_PASSWORD = "portal.dbuser.password";
    public static final String PORTAL_DBCONN_MAX_POOL_SIZE = "portal.dbconn.max.pool.size";
    public static final String PORTAL_DATA_SOURCE_SERVICE = "portal-data-source-service";
    public static final String PORTAL_DBQUERY_TIMEOUT = "portal.dbconn.query.timeout";

    public static final String API_DATA_SOURCE_SERVICE = "api-data-source-service";
    public static final String API_DBQUERY_TIMEOUT = "api.dbconn.query.timeout";

    public static final String TEMPLATE_DIR_ROOT = "webroot/";
    static final String TEMPLATE_DIR_EMAIL = TEMPLATE_DIR_ROOT +"email/";
    public static final String TEMPLATE_SUFFIX = ".html";


    public static final String LOG_LEVEL = "log.level";
    public static final String LOG_HTTPSERVER_ACTIVITY = "log.httpserver.activity";

    public static final String METRICS_ENABLED = "metrics.enabled";
    public static final String METRICS_JMX_ENABLED = "metrics.jmx.enabled";

    public static final String EB_MSG_TOKEN = "token";
    public static final String EB_MSG_TOKEN_TYPE = "token.type";
    public static final String EB_MSG_TO_EMAIL = "to.email";
    public static final String EB_MSG_HTML_STRING = "html.string";
    public static final String ABYSS_MAIL_CLIENT = "ABYSS_MAIL_CLIENT";
    public static final String DEFAULT_HTML_STRING = "<a href=\"http://apiportal.com/abyss/activation/?v=asSFbhIKae34654yhe3XEEUmjt56n5yrn45yh\">Activate My API Portal Account</a>";

    public static final String MAIL_SMTP_HOST = "mail.smtp.host";
    public static final String MAIL_SMTP_PORT = "mail.smtp.port";
    public static final String MAIL_SMTP_LOGIN_OPTION = "mail.smtp.login.option";
    public static final String MAIL_SMTP_START_TLS_OPTION = "mail.smtp.start.tls.option";
    public static final String MAIL_SMTP_AUTH_METHOD = "mail.smtp.auth.method";
    public static final String MAIL_SMTP_AUTH_USERNAME = "mail.smtp.auth.username";
    public static final String MAIL_SMTP_AUTH_PASSWORD = "mail.smtp.auth.password";

    public static final String MAIL_HREF_HOST = "mail.href.host";
    public static final String MAIL_HREF_PORT = "mail.href.port";
    public static final String MAIL_BASE_URL = "mail.base.url";
    public static final String MAIL_IMAGE_URL = "mail.image.url";
    public static final String MAIL_LOGIN_URL = "mail.login.url";

    public static final String ACTIVATION_SUBJECT = "Activate Your API Portal Account";
    public static final String COGITO_ACTIVATION_SUBJECT = "Activate Your Cogito Account";
    public static final String RESET_PASSWORD_SUBJECT = "Reset Your API Portal Password";
    public static final String WELCOME_SUBJECT = "Welcome to Abyss API Portal";
    public static final String PASSWORD_RESET_SUBJECT = "Your Abyss API Portal Password Has Been Reset";

    public static final String ACTIVATION_TEXT = "Activate My API Portal Account";
    public static final String COGITO_ACTIVATION_TEXT = "Activate My Cogito Account";
    public static final String RESET_PASSWORD_TEXT = "Reset My API Portal Password";
    public static final String WELCOME_TEXT = "Login to Abyss API Portal";
    public static final String PASSWORD_RESET_TEXT = "Your Abyss API Portal Password Has Been Reset. You Can Now Login to Abyss API Portal";

    public static final String ACTIVATION_PATH = "/activate-account";
    public static final String COGITO_ACTIVATION_PATH = "/activate-cogito";
    public static final String RESET_PASSWORD_PATH = "/reset-password";

    public static final String ACTIVATION_TOKEN = "token.type.activation";
    public static final String RESET_PASSWORD_TOKEN = "token.type.reset.password";
    public static final String INVITE_USER_TOKEN = "token.type.invite.user";
    public static final String WELCOME_TOKEN = "token.type.welcome"; //No token
    public static final String PASSWORD_RESET_TOKEN = "token.type.password.reset"; //No token

    public static final String MAIL_FROM_EMAIL_RESET_PASSWORD = "reset-password@apiportal.com (ABYSS API PORTAL)";
    public static final String MAIL_FROM_EMAIL_ACTIVATION = "activation@apiportal.com (ABYSS API PORTAL)";
    public static final String MAIL_FROM_EMAIL_WELCOME = "welcome@apiportal.com (ABYSS API PORTAL)";
    public static final String MAIL_FROM_EMAIL_NOTIFICATION = "notification@apiportal.com (ABYSS API PORTAL)";

    public static final String  ES_SERVER_URL = "es.server.url";
    public static final String  ES_SERVER_API_BULK_URL = "es.server.api.bulk.url";

    public static final String VERTICLE_TYPE_PORTAL = "verticle.type.portal";
    public static final String VERTICLE_TYPE_API = "verticle.type.api";


    public static final int PASSWORD_EXPIRATION_DAYS = 90;

    public static final int INTERNAL_SUBJECT_DIRECTORY_ID = 1;

    public static final int SYSTEM_USER_ID = 1;

    public static final int DEFAULT_ORGANIZATION_ID = 0;

    public static final int SUBJECT_TYPE_USER = 1;
    public static final int SUBJECT_TYPE_APP = 2;
    public static final int SUBJECT_TYPE_SYSTEM = 3;

    public static final String RESOURCE_DEFAULT_AVATAR = "webroot/dist/img/avatar.jpg";

    public static final String RESTAPI_FILTERING_NAME = "name";
    public static final String RESTAPI_SORTING_SORT = "sort";
    public static final String RESTAPI_FILTERING_FIELDS = "fields";
    public static final String RESTAPI_PAGINATION_OFFSET = "offset";
    public static final String RESTAPI_PAGINATION_LIMIT = "limit";
    public static final String RESTAPI_PAGINATION_TOTAL = "total";
}
