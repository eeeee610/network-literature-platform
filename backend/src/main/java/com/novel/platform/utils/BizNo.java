package com.novel.platform.utils;

import java.util.UUID;

/**
 * 业务单号生成：32 位 UUID（去横线），与 CHAR(32) 字段一致。
 */
public final class BizNo {

    private BizNo() {
    }

    public static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
