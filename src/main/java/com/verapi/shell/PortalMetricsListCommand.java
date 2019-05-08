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
