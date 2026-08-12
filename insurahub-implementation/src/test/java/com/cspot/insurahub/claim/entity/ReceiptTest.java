package com.cspot.insurahub.claim.entity;

import org.junit.jupiter.api.Test;

import static com.cspot.insurahub.claim.testdata.ClaimTestData.createValidClaim;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceiptTest {

    @Test
    void shouldCreateReceipt() {
        Claim claim = createValidClaim();
        byte[] content = "receipt".getBytes();

        Receipt receipt = new Receipt(
                claim,
                "receipt.pdf",
                "application/pdf",
                (long) content.length,
                content
        );

        assertEquals(claim, receipt.getClaim());
        assertEquals(receipt, claim.getReceipt());
        assertEquals("receipt.pdf", receipt.getOriginalFileName());
        assertEquals("application/pdf", receipt.getContentType());
        assertEquals(content.length, receipt.getSizeBytes());
        assertArrayEquals(content, receipt.getContent());
    }
}
