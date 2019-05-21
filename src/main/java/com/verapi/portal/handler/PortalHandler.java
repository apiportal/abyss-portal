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

package com.verapi.portal.handler;

import com.verapi.abyss.common.Constants;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

abstract class PortalHandler {

    void generateResponse(RoutingContext context, Logger logger, int statusCode, String message1, String message2, String message3) {
        logger.info("generateResponse invoked...");

        //Use user's session for storage
        context.session().put(Constants.HTTP_STATUSCODE, statusCode);
        context.session().put(Constants.HTTP_URL, message2);
        context.session().put(Constants.HTTP_ERRORMESSAGE, message1);
        context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, message3);

        if (statusCode== HttpStatus.SC_OK) {
            context.response().putHeader("location", "/abyss/success").setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY).end();
        } else {
            context.response().putHeader("location", "/abyss/httperror").setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY).end();
        }
    }

}
