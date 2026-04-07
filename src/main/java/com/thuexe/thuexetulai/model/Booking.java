package com.thuexe.thuexetulai.model;

import jakarta.persistence.*;
import java.time.LocalDate;



@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long carId;

    private LocalDate startDate;
    private LocalDate endDate;

    private String paymentCode;
    private Boolean depositPaid = false;
    private Boolean fullPaid = false;

    private String status;

    private Double totalPrice;

    private String damageStatus; // OK / DAMAGED
    private Double damageFee;
    private Boolean damagePaid;

    @Column(name = "return_image")
    private String returnImage;

    @Column(name = "return_note")
    private String returnNote;

    /** Tỷ lệ đặt cọc: 20, 50 hoặc 70 (% tổng tiền thuê). */
    @Column(name = "deposit_percent")
    private Integer depositPercent;

    /** CASH hoặc TRANSFER — cách thanh toán khoản cọc. */
    @Column(name = "deposit_method", length = 20)
    private String depositMethod;

    // getter setter
    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDepositPercent() { return depositPercent; }
    public void setDepositPercent(Integer depositPercent) { this.depositPercent = depositPercent; }

    public String getDepositMethod() { return depositMethod; }
    public void setDepositMethod(String depositMethod) { this.depositMethod = depositMethod; }

    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public Boolean getDepositPaid() {
        return depositPaid;
    }

    public void setDepositPaid(Boolean depositPaid) {
        this.depositPaid = depositPaid;
    }

    public Boolean getFullPaid() {
        return fullPaid;
    }

    public void setFullPaid(Boolean fullPaid) {
        this.fullPaid = fullPaid;
    }

    public String getReturnImage() {
        return returnImage;
    }

    public void setReturnImage(String returnImage) {
        this.returnImage = returnImage;
    }

    public String getReturnNote() {
        return returnNote;
    }

    public void setReturnNote(String returnNote) {
        this.returnNote = returnNote;
    }

    public String getDamageStatus() {
        return damageStatus;
    }

    public void setDamageStatus(String damageStatus) {
        this.damageStatus = damageStatus;
    }

    public Double getDamageFee() {
        return damageFee;
    }

    public void setDamageFee(Double damageFee) {
        this.damageFee = damageFee;
    }

    public Boolean getDamagePaid() {
        return damagePaid;
    }

    public void setDamagePaid(Boolean damagePaid) {
        this.damagePaid = damagePaid;
    }
}

