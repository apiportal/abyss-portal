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

package com.verapi.portal.common;

import com.verapi.portal.controller.FailureController;
import com.verapi.portal.controller.IndexController;
import com.verapi.portal.controller.LoginController;
import com.verapi.portal.controller.SignupController;
import com.verapi.portal.controller.SuccessController;

public class Controllers {

    public static final ControllerDef LOGIN = new ControllerDef(LoginController.class, "login", "login-auth", "login.html", Boolean.TRUE);
    public static final ControllerDef INDEX = new ControllerDef(IndexController.class, "index", "index", "index.html");
    public static final ControllerDef SIGNUP = new ControllerDef(SignupController.class, "signup", "sign-up", "signup.html", Boolean.TRUE);
    public static final ControllerDef TRX_OK = new ControllerDef(SuccessController.class, "success", "success", "success.html", Boolean.TRUE);
    public static final ControllerDef TRX_NOK = new ControllerDef(FailureController.class, "failure", "failure", "failure.html", Boolean.TRUE);

    public Controllers() {
    }

    public static final class ControllerDef {
        public Class className;
        public String routePathGET;
        public String routePathPOST;
        public String templateFileName;
        public Boolean isPublic = Boolean.FALSE;

        ControllerDef(Class className, String routePathGET, String routePathPOST, String templateFileName, Boolean isPublic) {
            this.className = className;
            this.routePathGET = routePathGET;
            this.routePathPOST = routePathPOST;
            this.templateFileName = templateFileName;
            this.isPublic = isPublic;
        }

        ControllerDef(Class className, String routePathGET, String routePathPOST, String templateFileName) {
            this.className = className;
            this.routePathGET = routePathGET;
            this.routePathPOST = routePathPOST;
            this.templateFileName = templateFileName;
        }

    }

}
