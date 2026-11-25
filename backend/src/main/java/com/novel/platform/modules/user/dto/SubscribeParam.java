package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public class SubscribeParam {
    private Long userId;
    private Long chapterId;
    private String requestNo;
    private String resultCode;
    private String message;
    private String txnNo;
    private BigDecimal balanceAfter;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTxnNo() { return txnNo; }
    public void setTxnNo(String txnNo) { this.txnNo = txnNo; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
}
