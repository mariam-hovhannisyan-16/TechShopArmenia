package am.techshop.notification.email;

import am.techshop.common.enums.Language;
import am.techshop.notification.i18n.AuthMessages;

import java.time.Year;

public final class EmailTemplates {

    private static final String ACCENT = "#0066ff";
    private static final String BG_PAGE = "#f4f6f8";
    private static final String TEXT_PRIMARY = "#1b1f24";
    private static final String TEXT_SECONDARY = "#57606a";
    private static final String BORDER = "#e5e8eb";
    private static final String FONT = "Arial, Helvetica, sans-serif";

    private EmailTemplates() {}

    public static String verificationEmail(String name, String verificationLink, Language language) {
        String body =
                "<p style=\"" + textStyle() + " margin:0 0 16px;\">" + escape(AuthMessages.greeting(name, language)) + "</p>" +
                "<p style=\"" + textStyle() + " margin:0 0 28px;\">" +
                escape(AuthMessages.verificationIntro(language)) +
                "</p>" +
                button(verificationLink, AuthMessages.verificationButton(language)) +
                "<p style=\"" + smallTextStyle() + " margin:28px 0 0;\">" + escape(AuthMessages.verificationExpiryNote(language)) + "</p>" +
                "<p style=\"" + smallTextStyle() + " margin:8px 0 0;\">" + escape(AuthMessages.verificationIgnoreNote(language)) + "</p>";

        return wrap(body, language);
    }

    public static String passwordResetEmail(String name, String resetLink, Language language) {
        String body =
                "<p style=\"" + textStyle() + " margin:0 0 16px;\">" + escape(AuthMessages.greeting(name, language)) + "</p>" +
                "<p style=\"" + textStyle() + " margin:0 0 28px;\">" +
                escape(AuthMessages.passwordResetIntro(language)) +
                "</p>" +
                button(resetLink, AuthMessages.passwordResetButton(language)) +
                "<p style=\"" + smallTextStyle() + " margin:28px 0 0;\">" + escape(AuthMessages.passwordResetExpiryNote(language)) + "</p>" +
                "<p style=\"" + smallTextStyle() + " margin:8px 0 0;\">" + escape(AuthMessages.passwordResetIgnoreNote(language)) + "</p>";

        return wrap(body, language);
    }

    public static String welcomeEmail(String name, String homeUrl, Language language) {
        String body =
                "<p style=\"" + textStyle() + " margin:0 0 16px;\">" + escape(AuthMessages.greeting(name, language)) + "</p>" +
                "<p style=\"" + textStyle() + " margin:0 0 28px;\">" +
                escape(AuthMessages.welcomeIntro(language)) +
                "</p>" +
                button(homeUrl, AuthMessages.welcomeButton(language));

        return wrap(body, language);
    }

    private static String wrap(String bodyHtml, Language language) {
        return "<!DOCTYPE html>" +
                "<html lang=\"" + language.name().toLowerCase() + "\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>TechShop AM</title>" +
                "</head>" +
                "<body style=\"margin:0; padding:0; background-color:" + BG_PAGE + ";\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:" + BG_PAGE + "; padding:24px 12px;\">" +
                "<tr><td align=\"center\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px; width:100%; background-color:#ffffff; border-radius:12px; overflow:hidden; border:1px solid " + BORDER + ";\">" +
                header() +
                "<tr><td style=\"padding:32px 32px 28px;\">" + bodyHtml + "</td></tr>" +
                footer() +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body></html>";
    }

    private static String header() {
        return "<tr><td style=\"background-color:" + ACCENT + "; padding:24px 32px;\" align=\"center\">" +
                "<span style=\"font-family:" + FONT + "; font-size:22px; font-weight:bold; color:#ffffff; letter-spacing:0.3px;\">" +
                "&#9889; TechShop AM" +
                "</span>" +
                "</td></tr>";
    }

    private static String footer() {
        int year = Year.now().getValue();
        return "<tr><td style=\"padding:20px 32px; border-top:1px solid " + BORDER + ";\" align=\"center\">" +
                "<p style=\"" + smallTextStyle() + " margin:0 0 4px;\">TechShop AM &middot; Երևան, Հայաստան</p>" +
                "<p style=\"font-family:" + FONT + "; font-size:12px; color:#8b949e; margin:0;\">" +
                "&copy; " + year + " TechShop AM. Բոլոր իրավունքները պաշտպանված են:" +
                "</p>" +
                "</td></tr>";
    }

    private static String button(String href, String label) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto;\">" +
                "<tr><td align=\"center\" style=\"border-radius:8px; background-color:" + ACCENT + ";\">" +
                "<a href=\"" + href + "\" style=\"display:inline-block; padding:14px 32px; font-family:" + FONT + "; font-size:15px; font-weight:bold; color:#ffffff; text-decoration:none; border-radius:8px;\">" +
                escape(label) +
                "</a>" +
                "</td></tr>" +
                "</table>";
    }

    private static String textStyle() {
        return "font-family:" + FONT + "; font-size:15px; line-height:1.5; color:" + TEXT_PRIMARY + ";";
    }

    private static String smallTextStyle() {
        return "font-family:" + FONT + "; font-size:13px; line-height:1.4; color:" + TEXT_SECONDARY + ";";
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
