package com.aicyber.backend.invoice.service;

import com.aicyber.backend.invoice.model.InvoiceLine;
import com.aicyber.backend.invoice.model.SalesInvoice;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class InvoicePdfRenderer {
    private static final Color INK = new Color(21, 31, 29);
    private static final Color MUTED = new Color(91, 108, 103);
    private static final Color GREEN = new Color(105, 166, 43);
    private static final Color PALE = new Color(240, 246, 241);
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public byte[] render(SalesInvoice invoice) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDDocumentInformation information = new PDDocumentInformation();
            information.setTitle(invoice.documentType().replace('_', ' ') + " " + invoice.invoiceNumber());
            information.setAuthor(invoice.sellerLegalName());
            information.setSubject("JON. PC order " + invoice.orderReference());
            document.setDocumentInformation(information);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream canvas = new PDPageContentStream(document, page)) {
                draw(canvas, invoice);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Tax invoice PDF could not be generated", exception);
        }
    }

    private void draw(PDPageContentStream canvas, SalesInvoice invoice) throws IOException {
        float left = 48;
        float right = 547;
        float y = 790;

        text(canvas, BOLD, 22, GREEN, left, y, "JON. PC");
        text(canvas, BOLD, 24, INK, 382, y, invoice.documentType().replace('_', ' '));
        y -= 28;
        text(canvas, BOLD, 10, INK, left, y, invoice.sellerLegalName());
        text(canvas, REGULAR, 9, MUTED, 382, y, invoice.invoiceNumber());
        y -= 15;
        text(canvas, REGULAR, 9, MUTED, left, y, "ABN " + invoice.sellerAbn());
        text(canvas, REGULAR, 9, MUTED, 382, y, "Issued " + DATE.format(invoice.issuedAt()));
        y -= 14;
        text(canvas, REGULAR, 9, MUTED, left, y, invoice.sellerAddress());
        y -= 14;
        text(canvas, REGULAR, 9, MUTED, left, y, invoice.sellerEmail() + "  |  " + invoice.sellerPhone());

        y -= 34;
        line(canvas, left, right, y, GREEN, 1.5f);
        y -= 28;
        label(canvas, left, y, "BILL TO");
        label(canvas, 330, y, "REFERENCES");
        y -= 19;
        text(canvas, BOLD, 11, INK, left, y, invoice.buyerName());
        text(canvas, REGULAR, 9, INK, 330, y, "Order  " + invoice.orderReference());
        y -= 16;
        text(canvas, REGULAR, 9, MUTED, left, y, invoice.buyerEmail());
        text(canvas, REGULAR, 9, INK, 330, y, "Payment  " + invoice.paymentReference());
        y -= 16;
        text(canvas, REGULAR, 9, MUTED, left, y, "Customer ID  " + invoice.buyerCustomerReference());
        if (invoice.buyerLocation() != null && !invoice.buyerLocation().isBlank()) {
            y -= 16;
            text(canvas, REGULAR, 9, MUTED, left, y, invoice.buyerLocation());
        }

        y -= 34;
        fill(canvas, left, y - 27, right - left, 32, PALE);
        text(canvas, BOLD, 8, MUTED, left + 10, y - 8, "DESCRIPTION");
        text(canvas, BOLD, 8, MUTED, 355, y - 8, "QTY");
        text(canvas, BOLD, 8, MUTED, 405, y - 8, "GST");
        text(canvas, BOLD, 8, MUTED, 485, y - 8, "TOTAL");
        y -= 46;
        for (InvoiceLine item : invoice.lines()) {
            text(canvas, BOLD, 10, INK, left + 10, y, item.description());
            text(canvas, REGULAR, 9, INK, 360, y, Integer.toString(item.quantity()));
            text(canvas, REGULAR, 9, INK, 405, y, money(item.gstCents()));
            rightText(canvas, BOLD, 9, INK, right - 6, y, money(item.lineTotalCents()));
            y -= 17;
            if (item.sku() != null && !item.sku().isBlank()) {
                text(canvas, REGULAR, 8, MUTED, left + 10, y, "SKU " + item.sku());
                y -= 12;
            }
            line(canvas, left, right, y, new Color(213, 222, 218), .7f);
            y -= 22;
        }

        float totalsX = 355;
        label(canvas, totalsX, y, "PAYMENT SUMMARY");
        y -= 22;
        amountRow(canvas, totalsX, right, y, "Subtotal excluding GST", invoice.subtotalExGstCents(), false);
        y -= 20;
        amountRow(canvas, totalsX, right, y, "GST", invoice.gstCents(), false);
        y -= 10;
        line(canvas, totalsX, right, y, GREEN, 1.2f);
        y -= 24;
        amountRow(canvas, totalsX, right, y, "Total including GST", invoice.totalCents(), true);
        y -= 22;
        amountRow(canvas, totalsX, right, y, "Amount paid", invoice.amountPaidCents(), false);
        y -= 22;
        amountRow(canvas, totalsX, right, y, "Balance due", 0, false);

        if (invoice.founderNumber() != null && invoice.founderCashbackAmountCents() != null) {
            y -= 46;
            fill(canvas, left, y - 50, right - left, 68, PALE);
            text(canvas, BOLD, 8, GREEN, left + 12, y, "FOUNDER CASHBACK COMMITMENT");
            text(canvas, BOLD, 10, INK, left + 12, y - 18,
                    "Founder #%02d  |  %s".formatted(invoice.founderNumber(), invoice.founderTierName()));
            rightText(canvas, BOLD, 11, GREEN, right - 12, y - 18, money(invoice.founderCashbackAmountCents()));
            text(canvas, REGULAR, 7.5f, MUTED, left + 12, y - 36,
                    "Separate from this invoice total. Initiated within 30 days after confirmed delivery.");
        }

        y = 92;
        fill(canvas, left, y - 17, right - left, 44, PALE);
        text(canvas, BOLD, 9, GREEN, left + 12, y + 8, "PAID");
        text(canvas, REGULAR, 8, MUTED, left + 12, y - 7, "Total price includes GST. Currency: AUD.");
        y = 49;
        line(canvas, left, right, y + 14, new Color(213, 222, 218), .7f);
        text(canvas, REGULAR, 7.5f, MUTED, left, y, "This tax invoice is your proof of purchase. Keep it for your records.");
        rightText(canvas, REGULAR, 7.5f, MUTED, right, y, "Page 1 of 1");
    }

    private void amountRow(PDPageContentStream canvas, float left, float right, float y, String label, long cents, boolean total) throws IOException {
        text(canvas, total ? BOLD : REGULAR, total ? 10 : 9, total ? INK : MUTED, left, y, label);
        rightText(canvas, total ? BOLD : REGULAR, total ? 11 : 9, total ? GREEN : INK, right, y, money(cents));
    }

    private void label(PDPageContentStream canvas, float x, float y, String value) throws IOException {
        text(canvas, BOLD, 8, GREEN, x, y, value);
    }

    private void text(PDPageContentStream canvas, PDFont font, float size, Color color, float x, float y, String value) throws IOException {
        canvas.beginText();
        canvas.setFont(font, size);
        canvas.setNonStrokingColor(color);
        canvas.newLineAtOffset(x, y);
        canvas.showText(value == null ? "" : value);
        canvas.endText();
    }

    private void rightText(PDPageContentStream canvas, PDFont font, float size, Color color, float right, float y, String value) throws IOException {
        float width = font.getStringWidth(value) / 1000 * size;
        text(canvas, font, size, color, right - width, y, value);
    }

    private void line(PDPageContentStream canvas, float left, float right, float y, Color color, float width) throws IOException {
        canvas.setStrokingColor(color);
        canvas.setLineWidth(width);
        canvas.moveTo(left, y);
        canvas.lineTo(right, y);
        canvas.stroke();
    }

    private void fill(PDPageContentStream canvas, float x, float y, float width, float height, Color color) throws IOException {
        canvas.setNonStrokingColor(color);
        canvas.addRect(x, y, width, height);
        canvas.fill();
    }

    private String money(long cents) {
        return String.format(Locale.US, "$%,.2f", cents / 100.0);
    }
}
