package com.novel.platform.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("reward_tip")
public class RewardTip {

    @TableId(type = IdType.AUTO)
    private Long tipId;
    private String tipNo;
    private String requestNo;
    private Long fromUserId;
    private Long toUserId;
    private Long novelId;
    private Long chapterId;
    private BigDecimal amount;
    private String message;
    private String status;
    private String outTxnNo;
    private String inTxnNo;
    private LocalDateTime createdAt;
    private LocalDateTime refundedAt;

    public Long getTipId() {
        return tipId;
    }

    public void setTipId(Long tipId) {
        this.tipId = tipId;
    }

    public String getTipNo() {
        return tipNo;
    }

    public void setTipNo(String tipNo) {
        this.tipNo = tipNo;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutTxnNo() {
        return outTxnNo;
    }

    public void setOutTxnNo(String outTxnNo) {
        this.outTxnNo = outTxnNo;
    }

    public String getInTxnNo() {
        return inTxnNo;
    }

    public void setInTxnNo(String inTxnNo) {
        this.inTxnNo = inTxnNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
