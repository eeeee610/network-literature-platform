package com.novel.content.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class SaveChapterRequest {

    @NotBlank(message = "章节标题不能为空")
    @Size(max = 120, message = "章节标题长度不能超过 120")
    private String title;

    @NotNull(message = "正文不能为空")
    private String content;

    @NotNull(message = "VIP 标记不能为空")
    @Min(value = 0, message = "VIP 标记只能为 0 或 1")
    @Max(value = 1, message = "VIP 标记只能为 0 或 1")
    private Integer isVip;

    @NotNull(message = "VIP 价格不能为空")
    @DecimalMin(value = "0.00", message = "VIP 价格不能为负数")
    @Digits(integer = 8, fraction = 2, message = "VIP 价格最多两位小数")
    private BigDecimal vipPrice;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getIsVip() {
        return isVip;
    }

    public void setIsVip(Integer isVip) {
        this.isVip = isVip;
    }

    public BigDecimal getVipPrice() {
        return vipPrice;
    }

    public void setVipPrice(BigDecimal vipPrice) {
        this.vipPrice = vipPrice;
    }
}
