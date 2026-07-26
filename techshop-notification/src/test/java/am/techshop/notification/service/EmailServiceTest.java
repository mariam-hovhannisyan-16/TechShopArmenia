package am.techshop.notification.service;

import am.techshop.common.dto.response.InstallmentPlanResponse;
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
                PaymentMethod.IDRAM, null);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderStatusChanged_WhenNotDevMode_SendsViaMailSender() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 1L, OrderStatus.PAID, BigDecimal.TEN,
                PaymentMethod.IDRAM, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("noreply@techshop.am", captor.getValue().getFrom());
    }

    @Test
    void sendOrderStatusChanged_WhenMailSenderThrows_SwallowsException() {
        setDevMode(false);
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 1L, OrderStatus.PAID, BigDecimal.TEN,
                PaymentMethod.IDRAM, null);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderStatusChanged_WhenPending_UsesCreatedSubjectAndBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PENDING, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("Your order has been received — TechShopArmenia", sent.getSubject());
        assertTrue(Objects.requireNonNull(sent.getText()).contains("Your order #7 has been successfully received."));
    }

    @Test
    void sendOrderStatusChanged_WhenNotPending_UsesStatusUpdateSubjectAndBody() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.SHIPPED, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("Order #7 status update — TechShopArmenia", sent.getSubject());
        assertTrue(Objects.requireNonNull(sent.getText()).contains("Your order #7 status is now: SHIPPED."));
    }

    @Test
    void sendOrderStatusChanged_WithRegularPaymentMethod_IncludesPaymentMethodLine() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.IDRAM, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText()).contains("Վճարման եղանակ՝ Idram"));
    }

    @Test
    void sendOrderStatusChanged_WithTelcellPaymentMethod_IncludesPaymentMethodLine() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.TELCELL, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText()).contains("Վճարման եղանակ՝ Telcell"));
    }

    @Test
    void sendOrderStatusChanged_WithRoketLinePaymentMethod_IncludesPaymentMethodLine() {
        setDevMode(false);

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.ROKET_LINE, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(Objects.requireNonNull(captor.getValue().getText()).contains("Վճարման եղանակ՝ Roket Line"));
    }

    @Test
    void sendOrderStatusChanged_WithInstallmentPaymentMethod_IncludesPlanSummaryInsteadOfPaymentMethodLine() {
        setDevMode(false);
        InstallmentPlanResponse plan = new InstallmentPlanResponse(
                "Ameriabank", BigDecimal.valueOf(0.12), 12, BigDecimal.valueOf(20), BigDecimal.valueOf(112.00));

        emailService.sendOrderStatusChanged("mariam@test.com", "Mariam", 7L, OrderStatus.PAID, BigDecimal.valueOf(150),
                PaymentMethod.INSTALLMENT, plan);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String text = Objects.requireNonNull(captor.getValue().getText());
        assertTrue(text.contains("Դուք ընտրել եք ապառիկ վճարում Ameriabank-ի միջոցով, 12 ամսով, ամսական 112.0 դրամ"));
        assertFalse(text.contains("Վճարման եղանակ՝"));
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
