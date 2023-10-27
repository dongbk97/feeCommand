package vn.vnpay.demo2.bean;

import vn.vnpay.demo2.common.FeeStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "FEE_TRANSACTION")
public class FeeTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private int id;

    @Column(name = "TRANSACTION_CODE")
    private String transactionCode;

    @Column(name = "COMMAND_CODE")
    private String commandCode;

    @Column(name = "FEE_AMOUNT")
    private double feeAmount;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private FeeStatus status;

    @Column(name = "ACCOUNT_NUMBER")
    private String accountNumber;

    @Column(name = "TOTAL_SCAN")
    private int totalScan;

    @Column(name = "REMARK")
    private String remark;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "MODIFIED_DATE")
    private LocalDateTime modifiedDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(String commandCode) {
        this.commandCode = commandCode;
    }

    public double getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(double feeAmount) {
        this.feeAmount = feeAmount;
    }

    public FeeStatus getStatus() {
        return status;
    }

    public void setStatus(FeeStatus status) {
        this.status = status;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getTotalScan() {
        return totalScan;
    }

    public void setTotalScan(int totalScan) {
        this.totalScan = totalScan;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
