package com.novel.platform.modules.user.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 请求体：校验失败由全局异常处理统一转为 40000。
 */
public final class Requests {

    private Requests() {
    }

    public record Register(
            @NotBlank(message = "不能为空")
            @Size(min = 3, max = 50, message = "长度需在 3～50 之间")
            @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "只能包含字母、数字和下划线")
            String username,

            @NotBlank(message = "不能为空")
            @Size(min = 6, max = 64, message = "长度需在 6～64 之间")
            String password,

            @Size(max = 50, message = "长度不能超过 50")
            String nickname,

            @Email(message = "格式不正确")
            @Size(max = 254, message = "长度不能超过 254")
            String email,

            @Pattern(regexp = "^$|^1\\d{10}$", message = "格式不正确")
            String phone) {
    }

    public record Login(
            @NotBlank(message = "不能为空") String username,
            @NotBlank(message = "不能为空") String password) {
    }

    public record ShelfAdd(@NotBlank(message = "不能为空") String novelId) {
    }

    public record ShelfUpdate(Boolean isTop, Boolean isNotify) {
    }

    public record ReadingProgress(@NotBlank(message = "不能为空") String chapterId) {
    }

    public record Subscribe(
            @NotBlank(message = "不能为空") String novelId,
            @NotBlank(message = "不能为空") String chapterId,
            @NotBlank(message = "不能为空")
            @Size(max = 64, message = "长度不能超过 64")
            String requestNo) {
    }

    public record Recharge(
            @NotNull(message = "不能为空")
            @DecimalMin(value = "0.01", message = "必须大于等于 0.01")
            @Digits(integer = 10, fraction = 2, message = "最多两位小数")
            BigDecimal amount,

            @NotBlank(message = "不能为空")
            @Size(max = 64, message = "长度不能超过 64")
            String requestNo) {
    }

    public record Reward(
            @NotBlank(message = "不能为空") String novelId,
            String chapterId,
            @NotNull(message = "不能为空")
            @DecimalMin(value = "0.01", message = "必须大于等于 0.01")
            @Digits(integer = 10, fraction = 2, message = "最多两位小数")
            BigDecimal amount,
            @Size(max = 200, message = "长度不能超过 200")
            String message,
            @NotBlank(message = "不能为空")
            @Size(max = 64, message = "长度不能超过 64")
            String requestNo) {
    }
}
