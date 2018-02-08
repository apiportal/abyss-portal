package com.verapi.starter;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.core.spi.launcher.DefaultCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Name("version")
@Summary("Displays the version.")
@Description("Prints the version of the portal.")
public class PortalVersionCommand extends DefaultCommand {
    private static Logger logger = LoggerFactory.getLogger(PortalVersionCommand.class);

    @Override
    public void run() throws CLIException {
        logger.info("Portal : " + new java.io.File(PortalVersionCommand.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName());
        logger.info("Vert.x : " + VersionCommand.getVersion());
    }

}
