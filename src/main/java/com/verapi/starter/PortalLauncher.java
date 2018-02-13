package com.verapi.starter;

import com.verapi.starter.common.Config;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PortalLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {

    public static void main(String[] args) {

        //enforce SLF4J logging set
        if (null == System.getProperty("vertx.logger-delegate-factory-class-name"))
            System.setProperty("vertx.logger-delegate-factory-class-name", io.vertx.core.logging.SLF4JLogDelegateFactory.class.getCanonicalName());

        System.setProperty("abyss-jar.name", new java.io.File(PortalLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName());

        new PortalLauncher()
                .register(PortalVersionCommand.class)
                .dispatch(args);
    }

    private Logger logger = LoggerFactory.getLogger(PortalLauncher.class);

    public static void executeCommand(String cmd, String... args) {
        new PortalLauncher().execute(cmd, args);
    }

    @Override
    public void afterConfigParsed(JsonObject jsonObject) {

    }

    @Override
    public void beforeStartingVertx(VertxOptions vertxOptions) {
        vertxOptions.setHAEnabled(true);
        logger.info(vertxOptions.toString());
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        logger.info(String.format("%s vertx started", vertx.toString()));
        logger.info(String.format("vertx is clustered : %s", vertx.isClustered()));
        logger.info(String.format("vertx is metric enabled : %s", vertx.isMetricsEnabled()));
        ConfigStoreOptions file = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "abyss-portal-config.properties"));
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(file)
                .setScanPeriod(10000);
        logger.debug("ConfigRetrieverOptions set OK..");
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        logger.debug("ConfigRetriever OK..");
        CompletableFuture future = new CompletableFuture();
        retriever.getConfig(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
                logger.error("afterStartingVertx ConfigRetriever getConfig failed " + ar.cause());
            } else {
                Config config = Config.getInstance().setConfig(ar.result());
                future.complete(ar.result());
                logger.debug("afterStartingVertx ConfigRetriever getConfig OK..");
                logger.debug("Config loaded... " + Config.getInstance().getConfigJsonObject().encodePrettily());
            }
        });
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getLocalizedMessage());
        } catch (ExecutionException e) {
            logger.error(e.getLocalizedMessage());
        } catch (TimeoutException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {

    }

    @Override
    public void beforeStoppingVertx(Vertx vertx) {

    }

    @Override
    public void afterStoppingVertx() {

    }

    @Override
    public void handleDeployFailed(Vertx vertx, String s, DeploymentOptions deploymentOptions, Throwable throwable) {

    }
}
