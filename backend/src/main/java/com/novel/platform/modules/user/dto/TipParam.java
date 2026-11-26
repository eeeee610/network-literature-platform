package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public class TipParam {
    private Long fromUserId;
    private Long toUserId;
    private Long novelId;
    private Long chapterId;
    private BigDecimal amount;
    private String message;
    private String requestNo;
    private String resultCode;
    private String resultMessage;
    private Long tipId;
    private String tipNo;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
    public Long getNovelId() { return novelId; }
    public void setNovelId(Long novelId) { this.novelId = novelId; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public Long getTipId() { return tipId; }
    public void setTipId(Long tipId) { this.tipId = tipId; }
    public String getTipNo() { return tipNo; }
    public void setTipNo(String tipNo) { this.tipNo = tipNo; }
    public BigDecimal getFromBalanceAfter() { return fromBalanceAfter; }
    public void setFromBalanceAfter(BigDecimal fromBalanceAfter) { this.fromBalanceAfter = fromBalanceAfter; }
    public BigDecimal getToBalanceAfter() { return toBalanceAfter; }
    public void setToBalanceAfter(BigDecimal toBalanceAfter) { this.toBalanceAfter = toBalanceAfter; }
}
