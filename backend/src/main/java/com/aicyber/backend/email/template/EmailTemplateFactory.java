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
    private final String supportEmail;

    public EmailTemplateFactory(
            @Value("${jonpc.email.account-url:https://jonpc.com.au/}") String accountUrl,
            @Value("${jonpc.email.support-address:support@jonpc.com.au}") String supportEmail
    ) {
        this.accountUrl = accountUrl;
        this.supportEmail = supportEmail;
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
                                long amountPaidCents, Long cashbackAmountCents) {
        String rewardText = cashbackAmountCents != null && cashbackAmountCents > 0
                ? "Founder reward locked: " + money(cashbackAmountCents)
                        + ". We’ll start it within 30 days after confirmed delivery."
                : "Your paid tax invoice is attached for your records.";
        String text = "Payment confirmed\n\n"
                + "Hi " + safeName(displayName) + ", your payment is complete.\n\n"
                + "Order: " + orderReference + "\n"
                + "Amount paid: " + money(amountPaidCents) + "\n"
                + "Tax invoice: " + invoiceNumber + " (attached)\n\n"
                + rewardText + "\n\n"
                + "View my order: " + accountUrl + "\n"
                + "Questions? Reply to this email or contact " + supportEmail + ".";

        String reward = cashbackAmountCents != null && cashbackAmountCents > 0
                ? "<div style=\"margin-top:18px;padding:18px 20px;background:#effbdc;border-left:4px solid #a8e95f\">"
                    + "<div style=\"font-size:11px;font-weight:700;letter-spacing:.1em;text-transform:uppercase;color:#4f635d\">Founder reward locked</div>"
                    + "<div style=\"margin-top:5px;font-size:22px;font-weight:700;color:#0b1513\">" + escape(money(cashbackAmountCents)) + "</div>"
                    + "<div style=\"margin-top:5px;font-size:13px;line-height:1.5;color:#52615e\">We’ll start it within 30 days after confirmed delivery.</div>"
                    + "</div>"
                : "";

        String body = "<div style=\"display:inline-block;padding:6px 10px;background:#e9f9d3;color:#274116;font-size:11px;font-weight:700;letter-spacing:.08em;text-transform:uppercase\">Paid</div>"
                + "<h1 style=\"margin:18px 0 10px;font-size:32px;line-height:1.12;letter-spacing:-.03em\">Payment confirmed.</h1>"
                + "<p style=\"margin:0 0 24px;color:#52615e;font-size:16px;line-height:1.55\">Thanks, " + escape(safeName(displayName)) + ". Your payment is complete.</p>"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:collapse;background:#f5f7f6\">"
                + detailRow("Order", orderReference, true)
                + detailRow("Amount paid", money(amountPaidCents), true)
                + detailRow("Tax invoice", invoiceNumber + " · PDF attached", false)
                + "</table>"
                + reward
                + "<a href=\"" + escape(accountUrl) + "\" style=\"display:inline-block;margin-top:24px;background:#0b1513;color:#fff;padding:14px 20px;text-decoration:none;font-size:14px;font-weight:700\">View my order&nbsp;&nbsp;→</a>";
        return new EmailContent("Payment confirmed · " + invoiceNumber, text, invoiceWrapper(body));
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
        return "<!doctype html><html><head><meta charset=\"UTF-8\"></head><body style=\"margin:0;background:#eef3f1;font-family:Arial,sans-serif;color:#0b1513\">"
                + "<div style=\"max-width:620px;margin:0 auto;padding:36px 18px\">"
                + "<div style=\"margin-bottom:18px;font-weight:700;letter-spacing:.12em\">JON. PC</div>"
                + "<div style=\"background:#fff;border-top:4px solid #bdf278;padding:32px\">" + body + "</div>"
                + "<div style=\"padding-top:16px;color:#71807d;font-size:12px\">AI CYBER AUSTRALIA PTY LTD</div>"
                + "</div></body></html>";
    }

    private String invoiceWrapper(String body) {
        return "<!doctype html><html><head><meta charset=\"UTF-8\"></head><body style=\"margin:0;background:#eef3f1;font-family:Arial,sans-serif;color:#0b1513\">"
                + "<div style=\"max-width:600px;margin:0 auto;padding:28px 16px\">"
                + "<div style=\"background:#0b1513;padding:24px 28px;color:#fff\">"
                + "<div style=\"font-size:18px;font-weight:700;letter-spacing:.14em\">JON. PC</div>"
                + "<div style=\"margin-top:8px;color:#9eaaa7;font-size:10px;letter-spacing:.12em;text-transform:uppercase\">Order confirmation</div>"
                + "</div>"
                + "<div style=\"background:#fff;border-top:4px solid #bdf278;padding:32px 28px\">" + body + "</div>"
                + "<div style=\"padding:18px 4px 0;color:#71807d;font-size:12px;line-height:1.6\">"
                + "Questions? Reply to this email or contact <a href=\"mailto:" + escape(supportEmail) + "\" style=\"color:#40504d\">" + escape(supportEmail) + "</a>.<br>"
                + "AI CYBER AUSTRALIA PTY LTD · ABN 22 689 546 450"
                + "</div></div></body></html>";
    }

    private String detailRow(String label, String value, boolean border) {
        return "<tr><td style=\"padding:15px 18px;color:#71807d;font-size:12px;" + (border ? "border-bottom:1px solid #dfe5e2;" : "") + "\">" + escape(label) + "</td>"
                + "<td align=\"right\" style=\"padding:15px 18px;color:#0b1513;font-size:13px;font-weight:700;" + (border ? "border-bottom:1px solid #dfe5e2;" : "") + "\">" + escape(value) + "</td></tr>";
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
