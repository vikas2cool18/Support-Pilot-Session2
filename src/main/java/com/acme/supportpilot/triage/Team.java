package com.acme.supportpilot.triage;

/** Support teams a ticket can be routed to. */
public enum Team {

    BILLING_OPS("billing and payment questions"),
    L1_SUPPORT("how-to questions, general questions and simple account changes"),
    L2_ENGINEERING("bugs, errors, outages, data problems and performance"),
    ACCOUNT_SECURITY("log-in problems, suspicious activity, 2FA and account deletion"),
    PRODUCT("feature requests and product feedback");

    private final String description;

    Team(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }

}
