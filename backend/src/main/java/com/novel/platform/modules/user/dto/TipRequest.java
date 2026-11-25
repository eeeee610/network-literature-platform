package com.novel.platform.modules.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TipRequest(
        @NotBlank(message = "作品 ID 不能为空") String novelId,
        String chapterId,
        @NotNull(message = "打赏金额不能为空")
        @DecimalMin(value = "0.01", message = "打赏金额必须大于 0")
        @Digits(integer = 10, fraction = 2, message = "打赏金额最多 10 位整数和 2 位小数")
        BigDecimal amount,
        @Size(max = 200, message = "打赏留言不能超过 200 字") String message,
        @NotBlank(message = "请求号不能为空")
        @Size(max = 64, message = "请求号长度不能超过 64")
        @Pattern(regexp = "[A-Za-z0-9._:-]+", message = "请求号只能包含字母、数字、点、下划线、冒号或短横线")
        String requestNo
) {
}
