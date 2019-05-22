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

package com.verapi.portal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.portal.common.BuildProperties;
import com.verapi.portal.service.es.ElasticSearchService;
import com.verapi.shell.PortalMetricsListCommand;
import com.verapi.shell.PortalVersionCommand;
import io.vertx.config.ConfigChange;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.command.CommandRegistry;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxInfluxDbOptions;
import io.vertx.micrometer.VertxJmxMetricsOptions;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PortalLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {

    private static final int SCAN_PERIOD = 10000;
    private Logger logger = LoggerFactory.getLogger(PortalLauncher.class);

    public static void main(String[] args) {

        //enforce SLF4J logging set
        if (null == System.getProperty("vertx.logger-delegate-factory-class-name")) {
            System.setProperty("vertx.logger-delegate-factory-class-name", io.vertx.core.logging.SLF4JLogDelegateFactory.class.getCanonicalName());
        }
        try {
            System.setProperty("abyss-jar.name",
                    new java.io.File(FilenameUtils.getName(PortalLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath()))
                            .getName());
        } catch (Exception e) {
            System.setProperty("abyss-jar.name", "abyss-portal.jar");
        }

        try {
            System.setProperty(Constants.ES_SERVER_API_BULK_URL, getProperty(Constants.ES_SERVER_API_BULK_URL));
        } catch (Exception e) {
            System.setProperty(Constants.ES_SERVER_API_BULK_URL, "http://localhost:9200/_bulk");
        }


        attachShutDownHook();

        new PortalLauncher()
                .register(PortalVersionCommand.class)
                .dispatch(args);

    }


    public static void executeCommand(String cmd, String... args) {
        new PortalLauncher().execute(cmd, args);
    }

    private static void attachShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook routine started now");
            System.out.println("active thread count: " + Thread.activeCount());
        }));
        System.out.println("Shutdown hook attached");
    }

    private static String getProperty(String propertyName) throws IOException {

        final Path path = Paths.get("abyss-portal-config.properties");
        try (InputStream is = java.nio.file.Files.newInputStream(path)) {

            // load a properties file
            Properties prop = new Properties();
            prop.load(is);

            // get the property value and print it out
            return prop.getProperty(propertyName);

        } catch (IOException e) {
            System.out.println("PortalLauncher.getProperty error " + e.getLocalizedMessage());
            System.out.println("PortalLauncher.getProperty error " + Arrays.toString(e.getStackTrace()));
            throw e;
        }


    }

    @Override
    public void afterConfigParsed(JsonObject jsonObject) {
        //no any implementation need right now
    }

    @Override
    public void beforeStartingVertx(VertxOptions vertxOptions) {
        vertxOptions.setHAEnabled(true);
        try {
            vertxOptions.setMetricsOptions(new MicrometerMetricsOptions()
                    .setJmxMetricsOptions(new VertxJmxMetricsOptions()
                            .setStep(Integer.parseInt(getProperty(Constants.VERTX_JMX_METRICS_PERIOD_IN_SECONDS)))
                            .setDomain(Constants.ABYSS)
                            .setEnabled(true))
                    .setInfluxDbOptions(new VertxInfluxDbOptions()
                            .setUri(getProperty(Constants.INFLUXDB_URI))
                            .setDb(getProperty(Constants.INFLUXDB_DBNAME))
                            .setUserName(getProperty(Constants.INFLUXDB_DBUSER_NAME))
                            .setPassword(getProperty(Constants.INFLUXDB_DBUSER_PASSWORD)
                            )
                            .setEnabled(("true".equals(getProperty(Constants.INFLUXDB_LOGGER_ENABLED))))
                    )
                    .setEnabled(true));
            logger.trace("Vertx Metrics options set successfully, settings are {}", vertxOptions);
        } catch (IOException e) {
            logger.error("Error occured while setting Vertx Metrics options, error is {}\n{}", e.getLocalizedMessage(), e.getStackTrace());
            System.out.println("Error occured while setting Vertx Metrics options, error: " + e.getLocalizedMessage());
            System.out.println("Error occured while setting Vertx Metrics options, error stack: " + Arrays.toString(e.getStackTrace()));

            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        if (vertx.isClustered()) {
            logger.trace("running cluster mode");
        }

        if (vertx.isMetricsEnabled()) {
            logger.trace("Vertx metrics enabled");
        }

        //load abyss-portal-config.properties
        ConfigStoreOptions file = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "abyss-portal-config.properties"));
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(file)
                .setScanPeriod(SCAN_PERIOD);
        logger.trace("ConfigRetrieverOptions set OK..");
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        logger.trace("ConfigRetriever OK..");
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        retriever.getConfig((AsyncResult<JsonObject> ar) -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                logger.error("afterStartingVertx ConfigRetriever getConfig failed: {} ", ar.cause().getLocalizedMessage());
            } else {
                Config.getInstance().setConfig(ar.result());
                future.complete(ar.result());
                logger.info("afterStartingVertx ConfigRetriever getConfig OK..");
                logger.debug("Config loaded... \n {} ", Config.getInstance().getConfigJsonObject().encodePrettily());
                ElasticSearchService elasticSearchService = new ElasticSearchService();
                elasticSearchService.indexDocument("configuration-audit", "configuration",
                        new JsonObject()
                                .put("op", Constants.ConfigState.INITIALIZED.toString())
                                .put("old", ar.result())
                                .put("new", new JsonObject()));
            }
        });
        final int CONFIGFILEREADTIMEOUT = 60;
        try {
            future.get(CONFIGFILEREADTIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("{}", e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        }
        retriever.listen((ConfigChange configChange) -> {
            Config.getInstance().setConfig(configChange.getNewConfiguration());
            logger.info("Config changed and reloaded... ");
            logger.debug("Config changed and reloaded...\n {} ", Config.getInstance().getConfigJsonObject().encodePrettily());
            ElasticSearchService elasticSearchService = new ElasticSearchService();
            elasticSearchService.indexDocument("configuration-audit", "configuration",
                    new JsonObject()
                            .put("op", Constants.ConfigState.CHANGED.toString())
                            .put("old", configChange.getPreviousConfiguration())
                            .put("new", configChange.getNewConfiguration()));
        });
        vertx.exceptionHandler((Throwable event) -> {
            logger.error("vertx global uncaught exceptionHandler >>> {}\n{}", event.getLocalizedMessage(), Arrays.toString(event.getStackTrace()));
            try {
                throw new RuntimeException(event);
            } catch (RuntimeException e) {
                logger.error("vertx global uncaught exceptionHandler >>> {}\n{}", e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
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
        abyssVersionConfigRetriever.getConfig((AsyncResult<JsonObject> ar) -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                logger.error("afterStartingVertx abyssVersionConfigRetriever getConfig failed {}", ar.cause().getLocalizedMessage());
            } else {
                BuildProperties buildProperties = BuildProperties.getInstance().setBuildProperties(ar.result());
                logger.info("Build properties loaded");
                logger.debug("Build properties loaded\n{}", BuildProperties.getInstance().getConfigJsonObject().encodePrettily());
            }
        });

        /* TODO: Complete Vault Configuration
        //load vault store
        JsonObject vaultConfig = new JsonObject()
                .put("host", "127.0.0.1") // The host name
                .put("port", 8200) // The port
                .put("ssl", true); // Whether or not SSL is used (disabled by default)

        ConfigStoreOptions abyssVaultConfigStoreOptions = new ConfigStoreOptions()
                .setType("vault")
                .setConfig(vaultConfig);
        ConfigRetrieverOptions abyssVaultConfigRetrieverOptions = new ConfigRetrieverOptions()
                .addStore(abyssVaultConfigStoreOptions);
        ConfigRetriever abyssVaultConfigRetriever = ConfigRetriever.create(vertx, abyssVaultConfigRetrieverOptions);
        abyssVaultConfigRetriever.getConfig(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                logger.error("afterStartingVertx abyssVaultConfigRetriever getConfig failed " + ar.cause());
            } else {
                BuildProperties buildProperties = BuildProperties.getInstance().setBuildProperties(ar.result());
                logger.info("afterStartingVertx abyssVaultConfigRetriever getConfig OK..");
                logger.debug("Config loaded... " + Config.getInstance().getConfigJsonObject().encodePrettily());
            }
        });
        */

        // TODO: Complete Consul Configuration
/*
        ConfigStoreOptions consulConfigStore = new ConfigStoreOptions()
                .setType("consul")
                .setFormat("json")
                .setConfig(new JsonObject()
                        .put("host", "localhost")
                        .put("port", 8500)
                  //      .put("prefix", "abyss")
                );
        ConfigRetriever consulConfigRetriever = ConfigRetriever.create(vertx,
                new ConfigRetrieverOptions().addStore(consulConfigStore));

        consulConfigRetriever.getConfig(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                logger.error("afterStartingVertx consulConfigRetriever getConfig failed " + ar.cause());
            } else {
                Config.getInstance().setConfig(ar.result());
                logger.warn("consulConfigRetriever loaded\n{}", ar.result().encodePrettily());
                logger.warn("consulConfigRetriever loaded\n{}", ar.result());
                logger.warn("consulConfigRetriever loaded\n{}", ar.result().getString("hostProtocol"));
            }
        });
*/


        //set all loggers' level
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<ch.qos.logback.classic.Logger> loggerList = loggerContext.getLoggerList();
        loggerList.forEach((ch.qos.logback.classic.Logger tmpLogger) -> {
            if (tmpLogger.getName().startsWith("com.verapi") || tmpLogger.getName().startsWith("io.vertx")) {
                logger.trace("setting log level [{}] for the class: {}"
                        , Config.getInstance().getConfigJsonObject().getString(Constants.LOG_LEVEL)
                        , tmpLogger.getName());
                tmpLogger.setLevel(Level.toLevel(Config.getInstance().getConfigJsonObject().getString(Constants.LOG_LEVEL)));
            }
        });

        //register CLI commands
        CommandRegistry commandRegistry = CommandRegistry.getShared(vertx);
        commandRegistry.registerCommand(PortalMetricsListCommand.class);

    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
        logger.info("deploying verticles...");
    }

    @Override
    public void beforeStoppingVertx(Vertx vertx) {
        logger.info("shutdown in progres...");
    }

    @Override
    public void afterStoppingVertx() {
        logger.info("shutdown");
    }

    @Override
    public void handleDeployFailed(Vertx vertx, String s, DeploymentOptions deploymentOptions, Throwable throwable) {
        throw new UnsupportedOperationException();
    }
}
