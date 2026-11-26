package com.novel.platform.modules.user.service;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.SubscribeParam;
import com.novel.platform.modules.user.dto.TipParam;
import com.novel.platform.modules.user.mapper.UserTradeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class TradeProcRepository {

    private final UserTradeMapper tradeMapper;

    public TradeProcRepository(UserTradeMapper tradeMapper) {
        this.tradeMapper = tradeMapper;
    }

    public void subscribe(SubscribeParam param) {
        try {
            tradeMapper.subscribe(param);
        } catch (RuntimeException exception) {
            throw translate(exception);
        }
    }

    public void tip(TipParam param) {
        try {
            tradeMapper.tip(param);
        } catch (RuntimeException exception) {
            throw translate(exception);
        }
    }

    private RuntimeException translate(RuntimeException exception) {
        SQLException sql = findSql(exception);
        if (sql == null) {
            return exception;
        }
        if (sql.getErrorCode() == 1062) {
            return new DuplicateKeyException("交易请求号或业务键冲突", sql);
        }
        if (!"45000".equals(sql.getSQLState()) && sql.getErrorCode() != 1644) {
            return new IllegalStateException("数据库交易操作失败", exception);
        }
        String message = sql.getMessage() == null ? "" : sql.getMessage();
        if (message.contains("余额不足")) {
            return new BusinessException(41003, "账户余额不足", HttpStatus.CONFLICT);
        }
        if (message.contains("状态不可用") || message.contains("禁止订阅")) {
            return new BusinessException(40301, "账号状态不可用，禁止交易", HttpStatus.FORBIDDEN);
        }
        if (message.contains("同一 request_no")) {
            return new BusinessException(41006, "同一请求号的参数与已有交易不一致", HttpStatus.CONFLICT);
        }
        if (message.contains("章节不存在")) {
            return new BusinessException(41004, "章节不存在", HttpStatus.NOT_FOUND);
        }
        if (message.contains("作品或章节不存在")) {
            return new BusinessException(41004, "作品或章节不存在，或章节不属于该作品", HttpStatus.NOT_FOUND);
        }
        if (message.contains("不能给自己打赏") || message.contains("收款人必须是")) {
            return new BusinessException(41005, message.contains("不能给自己打赏")
                    ? "不能给自己打赏" : "收款人必须是该作品作者", HttpStatus.BAD_REQUEST);
        }
        if (message.contains("VIP") || message.contains("未发布") || message.contains("未通过审核")) {
            return new BusinessException(41007, "章节当前不可订阅", HttpStatus.CONFLICT);
        }
        return new BusinessException(41000, "交易未完成，请检查输入后重试", HttpStatus.BAD_REQUEST);
    }

    private SQLException findSql(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sql) {
                return sql;
            }
            current = current.getCause();
        }
        return null;
    }
}
