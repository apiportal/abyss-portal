package com.verapi.starter;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {
    private static Logger logger = LoggerFactory.getLogger(PortalLauncher.class);

    public static void main(String[] args) {

        //enforce SLF4J logging set
        if (null == System.getProperty("vertx.logger-delegate-factory-class-name"))
            System.setProperty("vertx.logger-delegate-factory-class-name", io.vertx.core.logging.SLF4JLogDelegateFactory.class.getCanonicalName());

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
        logger.info(vertxOptions.toString());
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        logger.info(String.format("%s vertx started", vertx.toString()));
        logger.info(String.format("vertx is clustered : %s", vertx.isClustered()));
        logger.info(String.format("vertx is metric enabled : %s", vertx.isMetricsEnabled()));
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
