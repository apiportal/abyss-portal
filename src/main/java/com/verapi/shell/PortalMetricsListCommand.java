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

import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;


@Name("metrics-ls")
@Summary("List metrics.")
@Description("List all metrics")
public class PortalMetricsListCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess commandProcess) {
/*
        MetricsService metrics = MetricsService.create(commandProcess.vertx());
        metrics.metricsNames().forEach(name -> {
            commandProcess.write(name + "\n");
        });
*/
        commandProcess.end();
    }

}
