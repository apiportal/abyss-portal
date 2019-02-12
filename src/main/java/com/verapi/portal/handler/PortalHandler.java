package com.verapi.portal.handler;

import com.verapi.abyss.common.Constants;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;

public abstract class PortalHandler {

    protected void generateResponse(RoutingContext context, Logger logger, int statusCode, String message1, String message2, String message3, String message4) {
        logger.info("generateResponse invoked...");

        //Use user's session for storage
        context.session().put(Constants.HTTP_STATUSCODE, statusCode);
        context.session().put(Constants.HTTP_URL, message2);
        context.session().put(Constants.HTTP_ERRORMESSAGE, message1);
        context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, message3);

        if (statusCode==200) {
            context.response().putHeader("location", "/abyss/success").setStatusCode(302).end();
        } else {
            context.response().putHeader("location", "/abyss/httperror").setStatusCode(302).end();
        }
    }

}
