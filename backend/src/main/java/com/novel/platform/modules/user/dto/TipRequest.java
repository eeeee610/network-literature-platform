package com.novel.platform.modules.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TipRequest(
        @NotBlank(message = "novelId 不能为空") String novelId,
        String chapterId,
        @NotNull(message = "打赏金额不能为空")
        @DecimalMin(value = "0.01", message = "打赏金额必须大于 0")
        @Digits(integer = 10, fraction = 2, message = "打赏金额最多两位小数")
        BigDecimal amount,
        @Size(max = 200, message = "打赏留言不能超过 200 个字符") String message,
        @Size(max = 64, message = "requestNo 不能超过 64 个字符") String requestNo
) {
}
