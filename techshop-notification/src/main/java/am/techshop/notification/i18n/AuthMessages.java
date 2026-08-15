package am.techshop.notification.i18n;

import am.techshop.common.enums.Language;

public final class AuthMessages {

    private AuthMessages() {}

    public static String greeting(String name, Language language) {
        return switch (language) {
            case HY -> "Բարև, %s,".formatted(name);
            case RU -> "Здравствуйте, %s,".formatted(name);
            case EN -> "Hello %s,".formatted(name);
        };
    }

    public static String verificationSubject(Language language) {
        return switch (language) {
            case HY -> "Հաստատեք ձեր հաշիվը TechShop AM-ում";
            case RU -> "Подтвердите ваш аккаунт TechShopArmenia";
            case EN -> "Confirm your TechShopArmenia account";
        };
    }

    public static String verificationIntro(Language language) {
        return switch (language) {
            case HY -> "Շնորհակալություն TechShop AM-ում գրանցվելու համար: Ձեր հաշիվը ակտիվացնելու համար հաստատեք ձեր էլ. հասցեն՝ սեղմելով ստորև գտնվող կոճակին:";
            case RU -> "Спасибо за регистрацию в TechShopArmenia! Чтобы активировать аккаунт, подтвердите свой email, нажав на кнопку ниже.";
            case EN -> "Thank you for signing up at TechShopArmenia! To activate your account, please confirm your email by clicking the button below.";
        };
    }

    public static String verificationButton(Language language) {
        return switch (language) {
            case HY -> "Հաստատել հաշիվը";
            case RU -> "Подтвердить аккаунт";
            case EN -> "Confirm account";
        };
    }

    public static String verificationExpiryNote(Language language) {
        return switch (language) {
            case HY -> "Այս հղումն ուժի մեջ է 24 ժամ:";
            case RU -> "Ссылка действительна в течение 24 часов.";
            case EN -> "This link is valid for 24 hours.";
        };
    }

    public static String verificationIgnoreNote(Language language) {
        return switch (language) {
            case HY -> "Եթե դուք չեք գրանցվել TechShop AM-ում, պարզապես անտեսեք այս նամակը:";
            case RU -> "Если вы не регистрировались в TechShopArmenia, просто проигнорируйте это письмо.";
            case EN -> "If you didn't sign up at TechShopArmenia, just ignore this email.";
        };
    }

    public static String passwordResetSubject(Language language) {
        return switch (language) {
            case HY -> "Գաղտնաբառի վերականգնում TechShop AM-ում";
            case RU -> "Восстановление пароля TechShopArmenia";
            case EN -> "Reset your TechShopArmenia password";
        };
    }

    public static String passwordResetIntro(Language language) {
        return switch (language) {
            case HY -> "Մենք ստացել ենք ձեր հաշվի գաղտնաբառը վերականգնելու հայտը: Նոր գաղտնաբառ սահմանելու համար սեղմեք ստորև գտնվող կոճակին:";
            case RU -> "Мы получили запрос на восстановление пароля вашего аккаунта. Чтобы задать новый пароль, нажмите на кнопку ниже.";
            case EN -> "We received a request to reset your account password. To set a new password, click the button below.";
        };
    }

    public static String passwordResetButton(Language language) {
        return switch (language) {
            case HY -> "Վերականգնել գաղտնաբառը";
            case RU -> "Восстановить пароль";
            case EN -> "Reset password";
        };
    }

    public static String passwordResetExpiryNote(Language language) {
        return switch (language) {
            case HY -> "Այս հղումն ուժի մեջ է 1 ժամ:";
            case RU -> "Ссылка действительна в течение 1 часа.";
            case EN -> "This link is valid for 1 hour.";
        };
    }

    public static String passwordResetIgnoreNote(Language language) {
        return switch (language) {
            case HY -> "Եթե դուք չեք հայցել գաղտնաբառի վերականգնում, պարզապես անտեսեք այս նամակը:";
            case RU -> "Если вы не запрашивали восстановление пароля, просто проигнорируйте это письмо.";
            case EN -> "If you didn't request a password reset, just ignore this email.";
        };
    }

    public static String welcomeSubject(Language language) {
        return switch (language) {
            case HY -> "Բարի գալուստ TechShop AM";
            case RU -> "Добро пожаловать в TechShopArmenia";
            case EN -> "Welcome to TechShopArmenia";
        };
    }

    public static String welcomeIntro(Language language) {
        return switch (language) {
            case HY -> "Ձեր հաշիվը հաստատված է, և դուք այժմ կարող եք օգտվել TechShop AM-ի բոլոր հնարավորություններից: Բարի գնումներ:";
            case RU -> "Ваш аккаунт подтверждён, и теперь вам доступны все возможности TechShopArmenia. Приятных покупок!";
            case EN -> "Your account is confirmed, and you can now enjoy everything TechShopArmenia has to offer. Happy shopping!";
        };
    }

    public static String welcomeButton(Language language) {
        return switch (language) {
            case HY -> "Սկսել գնումներ կատարել";
            case RU -> "Начать покупки";
            case EN -> "Start shopping";
        };
    }

    public static String inAppEmailVerification(String name, Language language) {
        return switch (language) {
            case HY -> "Բարի գալուստ TechShop AM, %s! Խնդրում ենք հաստատել ձեր էլ. հասցեն".formatted(name);
            case RU -> "Добро пожаловать в TechShopArmenia, %s! Пожалуйста, подтвердите свой email".formatted(name);
            case EN -> "Welcome to TechShopArmenia, %s! Please confirm your email".formatted(name);
        };
    }
}
