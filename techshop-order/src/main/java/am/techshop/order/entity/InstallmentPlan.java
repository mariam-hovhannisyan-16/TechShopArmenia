package am.techshop.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlan {

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "down_payment")
    private BigDecimal downPayment;

    @Column(name = "monthly_payment")
    private BigDecimal monthlyPayment;
}
