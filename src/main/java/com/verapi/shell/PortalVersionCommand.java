/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 4 2018
 *
 */

package com.verapi.shell;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.core.spi.launcher.DefaultCommand;


@Name("version")
@Summary("Displays the version.")
@Description("Prints the version of the portal.")
public class PortalVersionCommand extends DefaultCommand {

    @Override
    public void run() throws CLIException {
        out.println("Portal : " + new java.io.File(PortalVersionCommand.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName());
        out.println("Vert.x : " + VersionCommand.getVersion());
    }

}
