package com.aicyber.backend.invoice.service;

import com.aicyber.backend.invoice.model.GstBreakdown;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GstCalculatorTest {
    private final GstCalculator calculator = new GstCalculator();

    @Test
    void extractsOneEleventhFromGstInclusiveComputerPrice() {
        GstBreakdown result = calculator.fromGstInclusiveTotal(234_700);

        assertEquals(213_364, result.subtotalExGstCents());
        assertEquals(21_336, result.gstCents());
        assertEquals(234_700, result.totalCents());
        assertEquals(result.totalCents(), result.subtotalExGstCents() + result.gstCents());
    }

    @Test
    void roundsToTheNearestCentWithoutFloatingPointArithmetic() {
        assertEquals(1, calculator.fromGstInclusiveTotal(6).gstCents());
        assertEquals(0, calculator.fromGstInclusiveTotal(5).gstCents());
    }

    @Test
    void rejectsNonPositiveInvoiceTotals() {
        assertThrows(IllegalArgumentException.class, () -> calculator.fromGstInclusiveTotal(0));
        assertThrows(IllegalArgumentException.class, () -> calculator.fromGstInclusiveTotal(-1));
    }
}
