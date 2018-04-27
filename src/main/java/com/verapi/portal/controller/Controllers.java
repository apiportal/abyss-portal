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

package com.verapi.portal.controller;

public class Controllers {

    public static final ControllerDef LOGIN = new ControllerDef(LoginController.class, "login", "login-auth", "login.html", Boolean.TRUE);
    public static final ControllerDef INDEX = new ControllerDef(IndexController.class, "index", "index", "index.html");
    public static final ControllerDef SIGNUP = new ControllerDef(SignupController.class, "signup", "sign-up", "signup.html", Boolean.TRUE);
    public static final ControllerDef TRX_OK = new ControllerDef(SuccessController.class, "success", "success", "success.html", Boolean.TRUE);
    public static final ControllerDef TRX_NOK = new ControllerDef(FailureController.class, "failure", "failure", "failure.html", Boolean.TRUE);
    public static final ControllerDef ACTIVATE_ACCOUNT = new ControllerDef(ActivateAccountController.class, "activate-account", null, null, Boolean.TRUE);
    public static final ControllerDef FORGOT_PASSWORD = new ControllerDef(ForgotPasswordController.class, "forgot-password", "forgot-password", "forgot-password.html", Boolean.TRUE);
    public static final ControllerDef RESET_PASSWORD = new ControllerDef(ResetPasswordController.class, "reset-password", "reset-password", "reset-password.html", Boolean.TRUE);
    public static final ControllerDef CHANGE_PASSWORD = new ControllerDef(ChangePasswordController.class, "change-password", "change-password", "change-password.html");
    public static final ControllerDef USERS = new ControllerDef(UsersController.class, "users", "users", "users.html");
    public static final ControllerDef SUBJECTGROUP = new ControllerDef(SubjectGroupController.class, "user-groups", "user-groups", "user-groups.html");
    public static final ControllerDef MYAPIS = new ControllerDef(MyApisController.class, "my-apis", "my-apis", "my-apis.html");

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
