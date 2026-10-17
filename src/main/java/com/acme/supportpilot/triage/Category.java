package com.acme.supportpilot.triage;

/** Ticket categories. The descriptions are rendered into the prompt, so the code and the prompt never drift apart. */
public enum Category {

    BILLING("invoices, payments, refunds, plans, pricing, GST and tax details"),
    TECHNICAL("bugs, errors, outages, performance problems and how-to questions about the product"),
    ACCOUNT("log-in, passwords, two-factor authentication, users, permissions, closing an account"),
    FEATURE_REQUEST("suggestions and requests for new features or integrations"),
    OTHER("anything else, such as praise or general questions");

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }

}
