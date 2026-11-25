package com.novel.platform.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("chapter_subscription")
public class ChapterSubscription {

    private Long userId;
    private Long chapterId;
    private Long novelId;
    private BigDecimal unitPrice;
    private BigDecimal paidAmount;
    private String txnNo;
    private String requestNo;
    private String status;
    private LocalDateTime subscribedAt;
    private LocalDateTime refundedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public Long getNovelId() { return novelId; }
    public void setNovelId(Long novelId) { this.novelId = novelId; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getTxnNo() { return txnNo; }
    public void setTxnNo(String txnNo) { this.txnNo = txnNo; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(LocalDateTime subscribedAt) { this.subscribedAt = subscribedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
}
