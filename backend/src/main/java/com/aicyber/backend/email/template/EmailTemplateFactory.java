package com.aicyber.backend.email.template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class EmailTemplateFactory {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);
    private final String accountUrl;

    public EmailTemplateFactory(@Value("${jonpc.email.account-url:https://jonpc.com.au/}") String accountUrl) {
        this.accountUrl = accountUrl;
    }

    public EmailContent verification(String displayName, String verificationUrl) {
        return action(
                "Verify your JON. PC email",
                "Verify your email",
                "Hi " + safeName(displayName) + ", verify this email address to secure your JON. PC account.",
                "Verify email",
                verificationUrl,
                "This link expires automatically. If you did not create this account, you can ignore this email."
        );
    }

    public EmailContent passwordReset(String displayName, String resetUrl) {
        return action(
                "Reset your JON. PC password",
                "Reset your password",
                "Hi " + safeName(displayName) + ", use the secure link below to choose a new password.",
                "Reset password",
                resetUrl,
                "If you did not request this, you can ignore this email."
        );
    }

    public EmailContent buildReceived(String displayName, String reference) {
        String intro = "Hi " + safeName(displayName) + ", we have received custom build " + reference
                + ". A JON. PC specialist will review compatibility, availability and final pricing.";
        return action("We received your JON. PC build " + reference, "Build received", intro,
                "View my orders", accountUrl, "We will email you again when your quote is ready.");
    }

    public EmailContent quoteReady(String displayName, String reference, long totalCents, OffsetDateTime validUntil) {
        String expiry = validUntil == null ? "" : " It is valid until " + DATE.format(validUntil) + ".";
        String intro = "Hi " + safeName(displayName) + ", your reviewed quote for " + reference + " is ready. "
                + "The confirmed total is " + money(totalCents) + "." + expiry;
        return action("Your JON. PC quote is ready", "Quote ready", intro,
                "Review quote", accountUrl, "Sign in to review and accept the quote before payment.");
    }

    public EmailContent invoice(String displayName, String invoiceNumber, String orderReference,
                                Long cashbackAmountCents, String cashbackTerms) {
        String intro = "Hi " + safeName(displayName) + ", payment for " + orderReference
                + " is confirmed. Your paid tax invoice " + invoiceNumber + " is attached.";
        String footer = cashbackAmountCents != null && cashbackAmountCents > 0
                ? "Founder cashback locked: " + money(cashbackAmountCents) + ". "
                    + (cashbackTerms == null ? "It remains separate from the amount paid." : cashbackTerms)
                : "Keep this invoice for your records.";
        return simple("Your JON. PC tax invoice " + invoiceNumber, "Payment confirmed", intro,
                footer);
    }

    public EmailContent operationsBuildReceived(String reference, String customerName) {
        return simple("Custom build awaiting review: " + reference, "New custom build",
                reference + " was submitted by " + safeName(customerName) + ".",
                "Open JON. PC Operations to review the configuration and prepare a final quote.");
    }

    private EmailContent action(String subject, String heading, String intro, String action, String url, String footer) {
        String text = heading + "\n\n" + intro + "\n\n" + action + ": " + url + "\n\n" + footer;
        String body = "<h1 style=\"margin:0 0 20px;font-size:30px;line-height:1.15\">" + escape(heading) + "</h1>"
                + "<p style=\"margin:0 0 24px;color:#40504d;line-height:1.6\">" + escape(intro) + "</p>"
                + "<a href=\"" + escape(url) + "\" style=\"display:inline-block;background:#bdf278;color:#07100e;"
                + "padding:14px 20px;text-decoration:none;font-weight:700\">" + escape(action) + "</a>"
                + "<p style=\"margin:24px 0 0;color:#71807d;font-size:13px;line-height:1.5\">" + escape(footer) + "</p>";
        return new EmailContent(subject, text, wrapper(body));
    }

    private EmailContent simple(String subject, String heading, String intro, String footer) {
        String text = heading + "\n\n" + intro + "\n\n" + footer;
        String body = "<h1 style=\"margin:0 0 20px;font-size:30px;line-height:1.15\">" + escape(heading) + "</h1>"
                + "<p style=\"margin:0 0 18px;color:#40504d;line-height:1.6\">" + escape(intro) + "</p>"
                + "<p style=\"margin:0;color:#71807d;font-size:13px;line-height:1.5\">" + escape(footer) + "</p>";
        return new EmailContent(subject, text, wrapper(body));
    }

    private String wrapper(String body) {
        return "<!doctype html><html><body style=\"margin:0;background:#eef3f1;font-family:Arial,sans-serif;color:#0b1513\">"
                + "<div style=\"max-width:620px;margin:0 auto;padding:36px 18px\">"
                + "<div style=\"margin-bottom:18px;font-weight:700;letter-spacing:.12em\">JON. PC</div>"
                + "<div style=\"background:#fff;border-top:4px solid #bdf278;padding:32px\">" + body + "</div>"
                + "<div style=\"padding-top:16px;color:#71807d;font-size:12px\">AI CYBER AUSTRALIA PTY LTD</div>"
                + "</div></body></html>";
    }

    private String money(long cents) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "AU"));
        return format.format(cents / 100.0);
    }

    private String safeName(String value) {
        return value == null || value.isBlank() ? "there" : value.trim();
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
