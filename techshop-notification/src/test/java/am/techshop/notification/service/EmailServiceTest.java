package am.techshop.notification.service;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setFrontendUrl() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
    }

    private void setDevMode(boolean devMode) {
        ReflectionTestUtils.setField(emailService, "mailUsername", devMode ? "" : "noreply@techshop.am");
    }

    @Test
    void sendOrderStatusChanged_WhenDevMode_NeverCallsMailSender() {
        setDevMode(true);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 1L, OrderStatus.PENDING, BigDecimal.TEN,
                PaymentMethod.IDRAM, null, Language.HY, null);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderStatusChanged_WhenNotDevMode_SendsViaMailSender() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 1L, OrderStatus.PAID, BigDecimal.TEN,
                PaymentMethod.IDRAM, null, Language.HY, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("noreply@techshop.am", captor.getValue().getFrom());
    }

    @Test
    void sendOrderStatusChanged_WhenMailSenderThrows_SwallowsException() {
        setDevMode(false);
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 1L, OrderStatus.PAID, BigDecimal.TEN,
                PaymentMethod.IDRAM, null, Language.HY, null);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderStatusChanged_WhenPendingAndEnglish_UsesEnglishCreatedSubjectAndBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PENDING, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null, Language.EN, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("Your order has been received — TechShopArmenia", sent.getSubject());
        assertTrue(Objects.requireNonNull(sent.getText()).contains("Your order #7 has been successfully received."));
    }

    @Test
    void sendOrderStatusChanged_WhenNotPendingAndEnglish_UsesEnglishStatusUpdateSubjectAndBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.SHIPPED, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null, Language.EN, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("Order #7 status update — TechShopArmenia", sent.getSubject());
        assertTrue(Objects.requireNonNull(sent.getText()).contains("Your order #7 status is now: Shipped."));
    }

    @Test
    void sendOrderStatusChanged_WhenArmenian_UsesArmenianSubjectAndBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PENDING, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null, Language.HY, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("Ձեր պատվերն ընդունված է — TechShop AM", sent.getSubject());
        String text = Objects.requireNonNull(sent.getText());
        assertTrue(text.contains("Բարև, Mariam,"));
        assertTrue(text.contains("Ձեր՝ #7 պատվերը հաջողությամբ ընդունվել է:"));
        assertFalse(text.contains("Your order"), "Armenian email should not contain leftover English copy");
    }

    @Test
    void sendOrderStatusChanged_WhenRussian_UsesRussianSubjectAndBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.SHIPPED, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null, Language.RU, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("Обновление статуса заказа №7 — TechShopArmenia", sent.getSubject());
        String text = Objects.requireNonNull(sent.getText());
        assertTrue(text.contains("Здравствуйте, Mariam,"));
        assertTrue(text.contains("Статус вашего заказа №7 сейчас: Отправлен."));
        assertFalse(text.contains("Your order"), "Russian email should not contain leftover English copy");
    }

    @Test
    void sendOrderStatusChanged_WithRegularPaymentMethod_IncludesPaymentMethodLineInRequestedLanguage() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null, Language.HY, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText()).contains("Վճարման եղանակ՝ Idram"));
    }

    @Test
    void sendOrderStatusChanged_WithTelcellPaymentMethodInEnglish_IncludesEnglishPaymentMethodLine() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.TELCELL, null, Language.EN, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText()).contains("Payment method: Telcell"));
    }

    @Test
    void sendOrderStatusChanged_WithRoketLinePaymentMethodInRussian_IncludesRussianPaymentMethodLine() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.ROKET_LINE, null, Language.RU, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText()).contains("Способ оплаты: Roket Line"));
    }

    @Test
    void sendOrderStatusChanged_WithInstallmentPaymentMethod_IncludesPlanSummaryInsteadOfPaymentMethodLine() {
        setDevMode(false);
        InstallmentPlanResponse plan = new InstallmentPlanResponse(
                "Ameriabank", BigDecimal.valueOf(0.12), 12, BigDecimal.valueOf(20), BigDecimal.valueOf(112.00));

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.INSTALLMENT, plan, Language.HY, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String text = Objects.requireNonNull(captor.getValue().getText());
        assertTrue(text.contains("Դուք ընտրել եք ապառիկ վճարում Ameriabank-ի միջոցով, 12 ամսով, ամսական 112.0 դրամ"));
        assertFalse(text.contains("Վճարման եղանակ՝"));
    }

    @Test
    void sendOrderStatusChanged_WithSingleProduct_IncludesProductNameInEnglishBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.SHIPPED, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null, Language.EN, List.of("iPhone 15, 128GB"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText())
                .contains("Your order #7 (iPhone 15, 128GB) status is now: Shipped."));
    }

    @Test
    void sendOrderStatusChanged_WithManyProducts_TruncatesToFirstPlusCountInRussianBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.SHIPPED, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null, Language.RU,
                List.of("iPhone 15, 128GB", "AirPods Pro", "USB-C Cable", "Чехол"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText())
                .contains("Статус вашего заказа №7 (iPhone 15, 128GB и ещё 3 товара) сейчас: Отправлен."));
    }

    @Test
    void sendVerificationEmail_WhenDevMode_NeverCallsMailSender() {
        setDevMode(true);

        emailService.sendVerificationEmail("mariam@test.com", "Mariam", "token123");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendVerificationEmail_WhenNotDevMode_SendsViaMimeMessage() {
        setDevMode(false);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail("mariam@test.com", "Mariam", "token123");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendVerificationEmail_WhenMimeMessageCreationFails_SwallowsException() {
        setDevMode(false);
        when(mailSender.createMimeMessage()).thenReturn(null);

        emailService.sendVerificationEmail("mariam@test.com", "Mariam", "token123");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_WhenNotDevMode_SendsViaMimeMessage() {
        setDevMode(false);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("mariam@test.com", "Mariam", "reset-token");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendWelcome_WhenNotDevMode_SendsViaMimeMessage() {
        setDevMode(false);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendWelcome("mariam@test.com", "Mariam");

        verify(mailSender).send(mimeMessage);
    }
}
