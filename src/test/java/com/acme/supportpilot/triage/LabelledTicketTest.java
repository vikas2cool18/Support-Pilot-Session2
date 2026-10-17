package com.acme.supportpilot.triage;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelledTicketTest {

    @Test
    void parsesQuotedFieldsWithCommasAndEscapedQuotes() {
        List<String> fields = LabelledTicket.parseCsvLine("T99,\"Hi, I said \"\"help\"\"\",BILLING");
        assertThat(fields).containsExactly("T99", "Hi, I said \"help\"", "BILLING");
    }

    @Test
    void readsTheTwentyLabelledTickets() throws Exception {
        List<LabelledTicket> tickets = LabelledTicket.readCsv(Path.of("data/tickets-labelled.csv"));

        assertThat(tickets).hasSize(20);
        assertThat(tickets.get(0).id()).isEqualTo("T01");
        assertThat(tickets.get(0).category()).isEqualTo(Category.BILLING);
        assertThat(tickets).extracting(LabelledTicket::category).contains(Category.values());
    }

}
