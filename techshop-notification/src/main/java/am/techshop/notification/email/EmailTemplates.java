package am.techshop.notification.email;

import java.time.Year;

public final class EmailTemplates {

    private static final String ACCENT = "#0066ff";
    private static final String BG_PAGE = "#f4f6f8";
    private static final String TEXT_PRIMARY = "#1b1f24";
    private static final String TEXT_SECONDARY = "#57606a";
    private static final String BORDER = "#e5e8eb";
    private static final String FONT = "Arial, Helvetica, sans-serif";

    private EmailTemplates() {}

    public static String verificationEmail(String name, String verificationLink) {
        String body =
                "<p style=\"" + textStyle() + " margin:0 0 16px;\">Բարև, " + escape(name) + ":</p>" +
                "<p style=\"" + textStyle() + " margin:0 0 28px;\">" +
                "Շնորհակալություն TechShop AM-ում գրանցվելու համար: Ձեր հաշիվը ակտիվացնելու համար հաստատեք ձեր էլ. հասցեն՝ սեղմելով ստորև գտնվող կոճակին:" +
                "</p>" +
                button(verificationLink, "Հաստատել հաշիվը") +
                "<p style=\"" + smallTextStyle() + " margin:28px 0 0;\">Այս հղումն ուժի մեջ է 24 ժամ:</p>" +
                "<p style=\"" + smallTextStyle() + " margin:8px 0 0;\">Եթե դուք չեք գրանցվել TechShop AM-ում, պարզապես անտեսեք այս նամակը:</p>";

        return wrap(body);
    }

    public static String passwordResetEmail(String name, String resetLink) {
        String body =
                "<p style=\"" + textStyle() + " margin:0 0 16px;\">Բարև, " + escape(name) + ":</p>" +
                "<p style=\"" + textStyle() + " margin:0 0 28px;\">" +
                "Մենք ստացել ենք ձեր հաշվի գաղտնաբառը վերականգնելու հայտը: Նոր գաղտնաբառ սահմանելու համար սեղմեք ստորև գտնվող կոճակին:" +
                "</p>" +
                button(resetLink, "Վերականգնել գաղտնաբառը") +
                "<p style=\"" + smallTextStyle() + " margin:28px 0 0;\">Այս հղումն ուժի մեջ է 1 ժամ:</p>" +
                "<p style=\"" + smallTextStyle() + " margin:8px 0 0;\">Եթե դուք չեք հայցել գաղտնաբառի վերականգնում, պարզապես անտեսեք այս նամակը:</p>";

        return wrap(body);
    }

    public static String welcomeEmail(String name, String homeUrl) {
        String body =
                "<p style=\"" + textStyle() + " margin:0 0 16px;\">Բարև, " + escape(name) + ":</p>" +
                "<p style=\"" + textStyle() + " margin:0 0 28px;\">" +
                "Ձեր հաշիվը հաստատված է, և դուք այժմ կարող եք օգտվել TechShop AM-ի բոլոր հնարավորություններից: Բարի գնումներ:" +
                "</p>" +
                button(homeUrl, "Սկսել գնումներ կատարել");

        return wrap(body);
    }

    private static String wrap(String bodyHtml) {
        return "<!DOCTYPE html>" +
                "<html lang=\"hy\">" +
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
