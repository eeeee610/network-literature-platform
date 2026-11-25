package com.novel.platform.common.exception;

/**
 * 公共与 A 模块业务码。A 模块使用 408xx 编号段，HTTP 状态单独按语义设置。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    /** 参数校验失败 */
    public static final int PARAM_INVALID = 40000;
    /** 未登录或凭证失效 */
    public static final int UNAUTHORIZED = 40100;
    /** 用户名或密码错误 */
    public static final int LOGIN_FAILED = 40101;
    /** 已登录但无操作权限 */
    public static final int FORBIDDEN = 40300;
    /** 请求地址不存在 */
    public static final int NOT_FOUND = 40400;
    /** 唯一键冲突 */
    public static final int DUPLICATE = 40900;
    /** 服务器内部错误 */
    public static final int SERVER_ERROR = 50000;

    /** 作品不存在或不可见 */
    public static final int NOVEL_NOT_FOUND = 40801;
    /** 章节不存在或不可购买 */
    public static final int CHAPTER_NOT_AVAILABLE = 40802;
    /** 余额不足 */
    public static final int BALANCE_INSUFFICIENT = 40803;
    /** 免费章节无需订阅 */
    public static final int CHAPTER_FREE = 40804;
    /** 不能打赏自己的作品 */
    public static final int REWARD_SELF = 40806;
    /** 账号状态不允许交易 */
    public static final int ACCOUNT_RESTRICTED = 40807;
    /** 请求号已被其他业务使用 */
    public static final int REQUEST_NO_CONFLICT = 40808;
    /** 书架中不存在该作品 */
    public static final int SHELF_NOT_FOUND = 40809;
}
