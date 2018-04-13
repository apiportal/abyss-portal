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

import com.verapi.portal.controller.IndexController;
import com.verapi.portal.controller.LoginController;

public class Controllers {

    public static final ControllerDef LOGIN = new ControllerDef(LoginController.class, "login", "login-auth", "login.html");
    public static final ControllerDef INDEX = new ControllerDef(IndexController.class, "index", "index", "index.html");
    public static final ControllerDef SIGNUP = new ControllerDef(IndexController.class, "signup", "sign-up", "signup.html");

    public static final class ControllerDef {
        public Object className;
        public String routePathGET;
        public String routePathPOST;
        public String templateFileName;

        public ControllerDef(Object className, String routePathGET, String routePathPOST, String templateFileName) {
            this.className = className;
            this.routePathGET = routePathGET;
            this.routePathPOST = routePathPOST;
            this.templateFileName = templateFileName;
        }
    }

}
