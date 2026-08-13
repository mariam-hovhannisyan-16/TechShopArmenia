package am.techshop.notification.i18n;

import java.math.BigDecimal;

public final class AccountMessages {

    private AccountMessages() {}

    private static final String SEPARATOR = " · ";
    private static final String BLOCK_DIVIDER = "\n\n———\n\n";

    public static String emailVerificationInApp(String name) {
        return "Բարի գալուստ TechShop AM, %1$s! Խնդրում ենք հաստատել ձեր էլ. հասցեն".formatted(name)
                + SEPARATOR + "Welcome to TechShop AM, %1$s! Please verify your email address".formatted(name)
                + SEPARATOR + "Добро пожаловать в TechShop AM, %1$s! Пожалуйста, подтвердите свой email".formatted(name);
    }

    public static String emailVerificationSubject() {
        return "Հաստատեք ձեր հաշիվը TechShop AM-ում" + SEPARATOR
                + "Verify your TechShop AM account" + SEPARATOR
                + "Подтвердите аккаунт TechShop AM";
    }

    public static String emailVerificationText(String name, String link) {
        String hy = """
                Բարև, %1$s.

                Շնորհակալություն TechShop AM-ում գրանցվելու համար: Ձեր հաշիվը ակտիվացնելու համար խնդրում ենք հետևել այս հղմանը.

                %2$s

                Հղումն ուժի մեջ է 24 ժամ:

                Եթե դուք չեք գրանցվել TechShop AM-ում, պարզապես անտեսեք այս նամակը:""".formatted(name, link);
        String en = """
                Hello, %1$s,

                Thank you for signing up at TechShop AM. To activate your account, please follow this link:

                %2$s

                This link is valid for 24 hours.

                If you did not sign up at TechShop AM, simply ignore this email.""".formatted(name, link);
        String ru = """
                Здравствуйте, %1$s,

                Спасибо за регистрацию в TechShop AM. Чтобы активировать аккаунт, перейдите по ссылке:

                %2$s

                Ссылка действительна в течение 24 часов.

                Если вы не регистрировались в TechShop AM, просто проигнорируйте это письмо.""".formatted(name, link);
        return hy + BLOCK_DIVIDER + en + BLOCK_DIVIDER + ru;
    }

    public static String passwordResetSubject() {
        return "Գաղտնաբառի վերականգնում TechShop AM-ում" + SEPARATOR
                + "Password reset for TechShop AM" + SEPARATOR
                + "Восстановление пароля TechShop AM";
    }

    public static String passwordResetText(String name, String link) {
        String hy = """
                Բարև, %1$s.

                Մենք ստացել ենք ձեր հաշվի գաղտնաբառը վերականգնելու հայտը: Նոր գաղտնաբառ սահմանելու համար հետևեք այս հղմանը.

                %2$s

                Հղումն ուժի մեջ է 1 ժամ:

                Եթե դուք չեք հայցել գաղտնաբառի վերականգնում, պարզապես անտեսեք այս նամակը:""".formatted(name, link);
        String en = """
                Hello, %1$s,

                We received a request to reset your account password. To set a new password, follow this link:

                %2$s

                This link is valid for 1 hour.

                If you did not request a password reset, simply ignore this email.""".formatted(name, link);
        String ru = """
                Здравствуйте, %1$s,

                Мы получили запрос на восстановление пароля вашего аккаунта. Чтобы задать новый пароль, перейдите по ссылке:

                %2$s

                Ссылка действительна в течение 1 часа.

                Если вы не запрашивали восстановление пароля, просто проигнорируйте это письмо.""".formatted(name, link);
        return hy + BLOCK_DIVIDER + en + BLOCK_DIVIDER + ru;
    }

    public static String welcomeSubject() {
        return "Բարի գալուստ TechShop AM" + SEPARATOR
                + "Welcome to TechShop AM" + SEPARATOR
                + "Добро пожаловать в TechShop AM";
    }

    public static String welcomeText(String name) {
        String hy = "Բարև, %1$s.\n\nՁեր հաշիվը հաստատված է, և դուք այժմ կարող եք օգտվել TechShop AM-ի բոլոր հնարավորություններից: Բարի գնումներ:".formatted(name);
        String en = "Hello, %1$s,\n\nYour account is verified and you can now enjoy all TechShop AM features. Happy shopping!".formatted(name);
        String ru = "Здравствуйте, %1$s,\n\nВаш аккаунт подтверждён, и теперь вам доступны все возможности TechShop AM. Приятных покупок!".formatted(name);
        return hy + BLOCK_DIVIDER + en + BLOCK_DIVIDER + ru;
    }

    public static String chatReplyMessage(String messagePreview) {
        return "Ձեր աջակցության չաթում նոր պատասխան կա՝ %1$s".formatted(messagePreview)
                + SEPARATOR + "New reply in your support conversation: %1$s".formatted(messagePreview)
                + SEPARATOR + "Новый ответ в вашем чате поддержки: %1$s".formatted(messagePreview);
    }

    public static String priceDropMessage(String productName, BigDecimal oldPrice, BigDecimal newPrice) {
        String hy = "%1$s-ի գինը իջել է. %2$s → %3$s"
                .formatted(productName, oldPrice.toPlainString(), newPrice.toPlainString());
        String en = "Price drop for %1$s: %2$s → %3$s"
                .formatted(productName, oldPrice.toPlainString(), newPrice.toPlainString());
        String ru = "Цена на %1$s снизилась: %2$s → %3$s"
                .formatted(productName, oldPrice.toPlainString(), newPrice.toPlainString());
        return hy + SEPARATOR + en + SEPARATOR + ru;
    }
}
