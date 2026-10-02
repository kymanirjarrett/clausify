package com.clausify.contract;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContractServiceTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            "Consulting_Agreement_2026.pdf     | Consulting_Agreement_2026.pdf",
            "C:\\Users\\maria\\Desktop\\NDA.pdf | NDA.pdf",
            "/home/maria/contracts/NDA.pdf     | NDA.pdf",
            "'   '                             | contract.pdf",
            "NULL                              | contract.pdf",
    })
    void cleanFilenameKeepsOnlyTheName(String original, String expected) {
        assertThat(ContractService.cleanFilename(original)).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    void cleanFilenameDropsControlCharactersAndCapsLength() {
        assertThat(ContractService.cleanFilename("evil\r\nname.pdf")).isEqualTo("evilname.pdf");
        assertThat(ContractService.cleanFilename("a".repeat(300) + ".pdf")).hasSize(255).endsWith(".pdf");
    }
}
