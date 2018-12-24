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

    public static final String ABYSS_BUILD_TIMESTAMP = "abyss.build.timestamp";
    private static final String ABYSS = "abyss";
    public static final String ABYSS_GW = "abyss-gw";
    public static final String ABYSS_ROOT = "/" + ABYSS;
    public static final String ABYSS_GATEWAY_ROOT = "/" + ABYSS_GW;
    public static final String ABYSS_PORTAL = "abyss-portal";
    public static final String HOST_PROTOCOL = "hostProtocol";
    public static final String HOST = "host";
    public static final String ISSANDBOX = "issandbox";

    public static final String SESSION_IDLE_TIMEOUT = "session.idle.timeout";

    public static final String HTTP_ECHO_SERVER_HOST = "http.echo.server.host";
    public static final String HTTP_ECHO_SERVER_PORT = "http.echo.server.port";
    public static final String HTTP_ECHO_PROXY_SERVER_PORT = "http.echo.proxy.server.port";
    public static final String HTTP_ECHO_SERVER_TIMEOUT = "http.echo.server.timeout";

    public static final String HTTP_SERVER_HOST = "http.server.host";
    public static final String HTTP_SERVER_PORT = "http.server.port";
    public static final String HTTP_PROXY_SERVER_PORT = "http.proxy.server.port";
    public static final String HTTP_SERVER_TIMEOUT = "http.server.timeout";
    public static final String HTTP_SERVER_ENABLE_COMPRESSION_SUPPORT = "http.server.enable.compression.support";

    public static final String HTTP_API_SERVER_HOST = "http.api.server.host";
    public static final String HTTP_API_SERVER_PORT = "http.api.server.port";
    public static final String HTTP_PROXY_API_SERVER_PORT = "http.proxy.api.server.port";
    public static final String HTTP_API_SERVER_TIMEOUT = "http.api.server.timeout";
    public static final String EB_API_SERVER_ADDRESS = "eb.api.server.address";
    public static final String HTTP_API_SERVER_ENABLE_COMPRESSION_SUPPORT = "http.api.server.enable.compression.support";

    public static final String HTTP_OPENAPI_SERVER_HOST = "http.openapi.server.host";
    public static final String HTTP_OPENAPI_SERVER_PORT = "http.openapi.server.port";
    public static final String HTTP_PROXY_OPENAPI_SERVER_PORT = "http.proxy.openapi.server.port";
    public static final String HTTP_OPENAPI_SERVER_TIMEOUT = "http.openapi.server.timeout";
    public static final String HTTP_OPENAPI_SERVER_ENABLE_COMPRESSION_SUPPORT = "http.openapi.server.enable.compression.support";

    public static final String HTTP_GATEWAY_SERVER_HOST = "http.gateway.server.host";
    public static final String HTTP_GATEWAY_SERVER_PORT = "http.gateway.server.port";
    public static final String HTTP_PROXY_GATEWAY_SERVER_PORT = "http.proxy.gateway.server.port";
    public static final String HTTP_GATEWAY_SERVER_TIMEOUT = "http.gateway.server.timeout";
    public static final String HTTP_GATEWAY_SERVER_LOG_HTTP_ACTIVITY = "http.gateway.server.log.http.activity";
    public static final String HTTP_GATEWAY_SERVER_ACCEPT_BACKLOG = "http.gateway.server.accept.backlog";
    public static final String HTTP_GATEWAY_SERVER_ENABLE_COMPRESSION_SUPPORT = "http.gateway.server.enable.compression.support";
    public static final String HTTP_GATEWAY_SERVER_IDLE_TIMEOUT = "http.gateway.server.idle.timeout";

    public static final String HTTPS_GATEWAY_SERVER_HOST = "https.gateway.server.host";
    public static final String HTTPS_GATEWAY_SERVER_PORT = "https.gateway.server.port";
    public static final String HTTPS_GATEWAY_OPENAPI_SERVER_PORT = "https.proxy.gateway.server.port";
    public static final String HTTPS_GATEWAY_SERVER_TIMEOUT = "https.gateway.server.timeout";
    public static final String HTTPS_GATEWAY_SERVER_LOG_HTTP_ACTIVITY = "https.gateway.server.log.http.activity";
    public static final String HTTPS_GATEWAY_SERVER_ACCEPT_BACKLOG = "https.gateway.server.accept.backlog";
    public static final String HTTPS_GATEWAY_SERVER_ENABLE_COMPRESSION_SUPPORT = "https.gateway.server.enable.compression.support";
    public static final String HTTPS_GATEWAY_SERVER_IDLE_TIMEOUT = "https.gateway.server.idle.timeout";

    public static final String HTTPS_GATEWAY_SSL_KEYSTORE_PATH = "";
    public static final String HTTPS_GATEWAY_SSL_KEYSTORE_PASSWORD = "";
    public static final String HTTPS_GATEWAY_SSL_TRUSTSTORE_PATH = "";
    public static final String HTTPS_GATEWAY_SSL_TRUSTSTORE_PASSWORD = "";

    public static final String CONTEXT_FAILURE_MESSAGE = "context.failureMessage";
    public static final String HTTP_ERRORMESSAGE = "http.errorMessage";
    public static final String HTTP_URL = "http.url";
    public static final String HTTP_STATUSCODE = "http.statusCode";
    public static final String HTML_FAILURE = "failure.html";
    public static final String HTML_SUCCESS = "success.html";

    public static final String PORTAL_JDBC_URL = "portal.jdbc.url";
    public static final String PORTAL_JDBC_DRIVER_CLASS = "portal.jdbc.driver.class";
    public static final String PORTAL_DBUSER_NAME = "portal.dbuser.name";
    public static final String PORTAL_DBSCHEMA_NAME = "portal.dbschema.name";
    public static final String PORTAL_DBUSER_PASSWORD = "portal.dbuser.password";
    public static final String PORTAL_DBCONN_MAX_POOL_SIZE = "portal.dbconn.max.pool.size";
    public static final String PORTAL_DBCONN_MAX_IDLE_TIME = "portal.dbconn.max.idle.time";

    public static final String PORTAL_DATA_SOURCE_SERVICE = "portal-data-source-service";
    public static final String PORTAL_DBQUERY_TIMEOUT = "portal.dbconn.query.timeout";
    public static final String API_DATA_SOURCE_SERVICE = "api-data-source-service";
    public static final String API_DBQUERY_TIMEOUT = "api.dbconn.query.timeout";
    public static final String GATEWAY_DATA_SOURCE_SERVICE = "gateway-data-source-service";
    public static final String GATEWAY_DBQUERY_TIMEOUT = "gateway.dbconn.query.timeout";
    public static final String ECHO_HTTP_SERVICE = "echo-http-service";

    public static final String TEMPLATE_DIR_ROOT = "webroot/";
    static final String TEMPLATE_DIR_EMAIL = TEMPLATE_DIR_ROOT + "email/";
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

    public static final String ES_SERVER_SCHEME = "es.server.scheme";
    public static final String ES_SERVER_HOST = "es.server.host";
    public static final String ES_SERVER_PORT = "es.server.port";
    public static final String ES_SERVER_URL = "es.server.url";
    public static final String ES_SERVER_API_BULK_URL = "es.server.api.bulk.url";

    public static final String HTTP_BIN_URL = "http.bin.url";

    public static final String VERTICLE_TYPE_PORTAL = "verticle.type.portal";
    public static final String VERTICLE_TYPE_API = "verticle.type.api";


    public static final String PASSWORD_EXPIRATION_DAYS = "subject.password.renewal.period";

    public static final int INTERNAL_SUBJECT_DIRECTORY_ID = 1;
    public static final String INTERNAL_SUBJECT_DIRECTORY_UUID = "ac504ae6-2bc9-40fa-8dfb-0ce501089573";

    public static final int SYSTEM_USER_ID = 1;
    public static final String SYSTEM_USER_UUID = "e20ca770-3c44-4a2d-b55d-2ebcaa0536bc";

    public static final int DEFAULT_ORGANIZATION_ID = 0;
    public static final String DEFAULT_ORGANIZATION_UUID = "3c65fafc-8f3a-4243-9c4e-2821aa32d293";

    public static final String SUBJECT_TYPE_USER = "21371a15-04f8-445e-a899-006ee11c0e09";
    public static final String SUBJECT_TYPE_APP = "ca80dd37-7484-46d3-b4a1-a8af93b2d3c6";
    public static final String SUBJECT_TYPE_SYSTEM = "80fc37d5-0594-456c-851b-a7e68fe55e9e";
    public static final String SUBJECT_TYPE_GROUP = "c5ef2da7-b55e-4dec-8be3-96bf30255781";

    public static final String RESOURCE_TYPE_API = "505099b4-19da-401c-bd17-8c3a85d89743";
    public static final String RESOURCE_TYPE_APP = "9f4be4c4-fbbe-4f13-a5e1-5b8f3d8e30ec";
    public static final String RESOURCE_TYPE_POLICY = "4ddbc735-8905-488a-81a4-f21a45ebc4ef";
    public static final String RESOURCE_TYPE_CONTRACT = "0e600a0a-8edc-41f2-8749-2560278d33f1";
    public static final String RESOURCE_TYPE_LICENSE = "4a3d51ce-cbd6-405b-bf58-328332efa499";

    public static final String RESOURCE_ACTION_INVOKE_API = "c5639f00-94c9-4cc9-8ad9-df76f9d162a8";
    public static final String RESOURCE_ACTION_VIEW_API = "bf0b6ac2-7d07-49c6-b3f8-0fd7c927126e";
    public static final String RESOURCE_ACTION_EDIT_API = "7e55b086-75e0-4209-9cc5-51baa38393ed";

    public static final String CONTRACT_STATE_IS_ACTIVATED = "846282ec-1329-4a3c-908b-672b4de3ade2";

    public static final String CONTRACT_STATUS_IS_INFORCE = "inforce";

    public static final String RESOURCE_DEFAULT_SUBJECT_AVATAR = "/webroot/dist/img/avatar.jpg";
    public static final String RESOURCE_DEFAULT_API_AVATAR = "/webroot/dist/img/thumb.png";

    public static final String RESTAPI_FILTERING_BY_NAME = "byname";
    public static final String RESTAPI_FILTERING_LIKE_NAME = "likename";
    public static final String RESTAPI_SORTING_SORT = "sort";
    public static final String RESTAPI_FILTERING_FIELDS = "fields";
    public static final String RESTAPI_PAGINATION_OFFSET = "offset";
    public static final String RESTAPI_PAGINATION_LIMIT = "limit";
    public static final String RESTAPI_PAGINATION_TOTAL = "total";
    public static final String RESTAPI_AGGREGATE = "aggregate";

    public static final String JSONB_COLUMN_API_OPENAPIDOCUMENT = "openapidocument";
    public static final String JSONB_COLUMN_API_EXTENDEDDOCUMENT = "extendeddocument";
    public static final String JSONB_COLUMN_SUBJECT_DIRECTORY_DIRECTORYATTRIBUTES = "directoryattributes";
    public static final String JSONB_COLUMN_SUBJECT_DIRECTORY_TYPE_ATTRIBUTE_TEMPLATE = "attributetemplate";
    public static final String JSONB_COLUMN_POLICY_POLICYINSTANCE = "policyinstance";
    public static final String JSONB_COLUMN_LICENSE_LICENSEDOCUMENT = "licensedocument";
    public static final String JSONB_COLUMN_POLICY_TYPE_TEMPLATE = "template";
    public static final String JSONB_COLUMN_ACCESS_MANAGER_TYPE_TEMPLATE = "attributetemplate";
    public static final String JSONB_COLUMN_ACCESS_MANAGER_ATTRIBUTES = "accessmanagerattributes";
    public static final String JSONB_COLUMN_MESSAGE_RECEIVER = "receiver";
    public static final String JSONB_COLUMN_MESSAGE_SENDER = "sender";

    public static final String NESTED_COLUMN_USER_GROUPS = "groups";
    public static final String NESTED_COLUMN_USER_PERMISSIONS = "permissions";

    public static final int ONE_MINUTE_IN_SECONDS = 60;
    public static final int ONE_HOUR_IN_SECONDS = 3600;
    public static final int ONE_DAY_IN_SECONDS = 86400;

    public static final String AUTH_ABYSS_GATEWAY_COOKIE_NAME = "abyss-gateway-session";
    public static final String AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME = "abyss-gateway-api-access-token"; //spinal case recommended

    public static final String AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME = "abyss.login.organization.name";
    public static final String AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME = "abyss.login.organization.uuid";
    public static final String AUTH_ABYSS_PORTAL_SESSION_COOKIE_NAME = "abyss.session";
    public static final String AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME = "abyss.principal.uuid";
    public static final String AUTH_ABYSS_PORTAL_PRINCIPAL_COOKIE_NAME = "abyss_principal";
    public static final String AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME = "user.uuid";
    public static final String AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME = "username";

    /*
    # enums
    */

    public enum ConfigState {
        INITIALIZED("initialized"),
        CHANGED("changed");

        private final String desc;

        ConfigState(String desc) {
            this.desc = desc;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    /*public enum PgDatabaseMetaData {
        TABLE_NAME("TABLE_NAME"),
        COLUMN_NAME("COLUMN_NAME"),
        DATA_TYPE("DATA_TYPE"),
        TYPE_NAME("TYPE_NAME"),
        COLUMN_SIZE("COLUMN_SIZE"),
        DECIMAL_DIGITS("DECIMAL_DIGITS"),
        NULLABLE("NULLABLE"),
        REMARKS("REMARKS"),
        COLUMN_DEF("COLUMN_DEF"),
        IS_NULLABLE("IS_NULLABLE"),
        IS_AUTOINCREMENT("IS_AUTOINCREMENT");

        private final String desc;

        PgDatabaseMetaData(String desc) {
            this.desc = desc;
        }

        @Override
        public String toString() {
            return desc;
        }
    }*/
}
