package vn.vnpay.demo2.bean;

import vn.vnpay.demo2.common.CreateUser;

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
@Table(name = "FEE_COMMAND")
public class FeeCommand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private int id;

    @Column(name = "COMMAND_CODE")
    private String commandCode;

    @Column(name = "TOTAL_RECORD")
    private int totalRecord;

    @Column(name = "TOTAL_FEE")
    private double totalFee;

    @Column(name = "CREATED_USER")
    @Enumerated(EnumType.STRING)
    private CreateUser createdUser;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(String commandCode) {
        this.commandCode = commandCode;
    }

    public int getTotalRecord() {
        return totalRecord;
    }

    public void setTotalRecord(int totalRecord) {
        this.totalRecord = totalRecord;
    }

    public double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }

    public CreateUser getCreatedUser() {
        return createdUser;
    }

    public void setCreatedUser(CreateUser createdUser) {
        this.createdUser = createdUser;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
