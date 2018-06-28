/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 2 2018
 *
 */

package com.verapi.portal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.verapi.portal.common.BuildProperties;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.common.FileUtil;
import com.verapi.portal.common.PlatformAPIList;
import com.verapi.shell.PortalMetricsListCommand;
import com.verapi.shell.PortalVersionCommand;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.shell.command.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PortalLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {

    private Logger logger = LoggerFactory.getLogger(PortalLauncher.class);

    public static void main(String[] args) {

        //enforce SLF4J logging set
        if (null == System.getProperty("vertx.logger-delegate-factory-class-name"))
            System.setProperty("vertx.logger-delegate-factory-class-name", io.vertx.core.logging.SLF4JLogDelegateFactory.class.getCanonicalName());

        System.setProperty("abyss-jar.name", new java.io.File(PortalLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName());

        System.setProperty("es.server.api.bulk.url", getProperty("es.server.api.bulk.url"));

        attachShutDownHook();

        new PortalLauncher()
                .register(PortalVersionCommand.class)
                .dispatch(args);

    }


    public static void executeCommand(String cmd, String... args) {
        new PortalLauncher().execute(cmd, args);
    }

    @Override
    public void afterConfigParsed(JsonObject jsonObject) {

    }

    @Override
    public void beforeStartingVertx(VertxOptions vertxOptions) {
        vertxOptions.setHAEnabled(true);
        vertxOptions.setMetricsOptions(new DropwizardMetricsOptions()
                .setEnabled(Config.getInstance().getConfigJsonObject().getBoolean(Constants.METRICS_ENABLED, true))
                .setJmxEnabled(Config.getInstance().getConfigJsonObject().getBoolean(Constants.METRICS_JMX_ENABLED, true))
                .setRegistryName(Constants.ABYSS_PORTAL)
                .setJmxDomain(Constants.ABYSS_PORTAL)
                .setBaseName(Constants.ABYSS_PORTAL)
        );
        logger.trace(vertxOptions.toString());
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        logger.trace(String.format("%s vertx started", vertx.toString()));
        logger.trace(String.format("vertx is clustered : %s", vertx.isClustered()));

        //MetricsService service = MetricsService.create(vertx);
        logger.trace(String.format("vertx is metric enabled : %s", vertx.isMetricsEnabled()));

        //load abyss-portal-config.properties
        ConfigStoreOptions file = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "abyss-portal-config.properties"));
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(file)
                .setScanPeriod(10000);
        logger.trace("ConfigRetrieverOptions set OK..");
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        logger.trace("ConfigRetriever OK..");
        CompletableFuture future = new CompletableFuture();
        retriever.getConfig(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                logger.error("afterStartingVertx ConfigRetriever getConfig failed " + ar.cause());
            } else {
                Config config = Config.getInstance().setConfig(ar.result());
                future.complete(ar.result());
                logger.info("afterStartingVertx ConfigRetriever getConfig OK..");
                logger.debug("Config loaded... " + Config.getInstance().getConfigJsonObject().encodePrettily());
            }
        });
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error(e.getLocalizedMessage());
            vertx.close();
        }
        retriever.listen(configChange -> {
            Config config = Config.getInstance().setConfig(configChange.getNewConfiguration());
            logger.debug("Config changed and reloaded... " + Config.getInstance().getConfigJsonObject().encodePrettily());

        });
        vertx.exceptionHandler((Throwable event) -> {
            logger.error("vertx global uncaught exceptionHandler >>> " + event + " throws exception: " + Arrays.toString(event.getStackTrace()));
            try {
                throw event;
            } catch (Throwable throwable) {
                logger.error("vertx global uncaught exceptionHandler >>> " + event + " throws exception: " + Arrays.toString(throwable.getStackTrace()));
            }
        });

        //load abyss-version.properties
        ConfigStoreOptions abyssVersionConfigStoreOptions = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "abyss-version.properties"));
        ConfigRetrieverOptions abyssVersionConfigRetrieverOptions = new ConfigRetrieverOptions()
                .addStore(abyssVersionConfigStoreOptions);
        ConfigRetriever abyssVersionConfigRetriever = ConfigRetriever.create(vertx, abyssVersionConfigRetrieverOptions);
        abyssVersionConfigRetriever.getConfig(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                logger.error("afterStartingVertx abyssConfigRetriever getConfig failed " + ar.cause());
            } else {
                BuildProperties buildProperties = BuildProperties.getInstance().setBuildProperties(ar.result());
                logger.info("Build properties loaded");
                logger.debug("Build properties loaded\n{}", BuildProperties.getInstance().getConfigJsonObject().encodePrettily());
            }
        });

        //load platform API list
        PlatformAPIList platformAPIList = PlatformAPIList.getInstance().setPlatformAPIList(new FileUtil().getYamlFileList());
        logger.info("Platform API list loaded");
        logger.debug("PlatformAPIList loaded\n{}", PlatformAPIList.getInstance().getPlatformAPIList().encodePrettily());

        //set all loggers' level
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<ch.qos.logback.classic.Logger> loggerList = loggerContext.getLoggerList();
        loggerList.forEach(tmpLogger -> {
            if (tmpLogger.getName().startsWith("com.verapi") || tmpLogger.getName().startsWith("io.vertx")) {
                logger.trace("setting log level [{}] for the class: {}", Config.getInstance().getConfigJsonObject().getString(Constants.LOG_LEVEL), tmpLogger.getName());
                tmpLogger.setLevel(Level.toLevel(Config.getInstance().getConfigJsonObject().getString(Constants.LOG_LEVEL)));
            }
        });

        //register CLI commands
        CommandRegistry commandRegistry = CommandRegistry.getShared(vertx);
        commandRegistry.registerCommand(PortalMetricsListCommand.class);

    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {

    }

    @Override
    public void beforeStoppingVertx(Vertx vertx) {
        logger.info("shutdown in progres...");
    }

    @Override
    public void afterStoppingVertx() {
        logger.info("shutdown in progres...");
    }

    @Override
    public void handleDeployFailed(Vertx vertx, String s, DeploymentOptions deploymentOptions, Throwable throwable) {

    }

    private static void attachShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Inside Add Shutdown Hook");
            }
        });
        System.out.println("Shut Down Hook Attached");
    }

    private static String getProperty(String propertyName) {
        Properties prop = new Properties();
        InputStream input = null;
        String result = null;
        try {
            input = new FileInputStream("abyss-portal-config.properties");

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            result = prop.getProperty(propertyName);

        } catch (IOException e) {
            System.out.println("PortalLauncher.getProperty error " + e.getLocalizedMessage());
            System.out.println("PortalLauncher.getProperty error " + Arrays.toString(e.getStackTrace()));
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    System.out.println("PortalLauncher.getProperty error during closing config file " + propertyName + e.getLocalizedMessage());
                    System.out.println("PortalLauncher.getProperty error during closing config file " + propertyName + Arrays.toString(e.getStackTrace()));
                }
            }
        }
        return result;
    }
}
