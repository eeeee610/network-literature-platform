package com.novel.platform.modules.interaction.service;

import com.novel.platform.common.exception.BusinessException;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

/**
 * 将存储过程 SIGNAL SQLSTATE '45000' 的业务提示转换为可读异常。
 */
final class ProcedureErrors {

    private static final String BUSINESS_SQL_STATE = "45000";

    private ProcedureErrors() {
    }

    static BusinessException toBusiness(DataAccessException exception) {
        SQLException sqlException = findSqlException(exception);
        if (sqlException != null
                && BUSINESS_SQL_STATE.equals(sqlException.getSQLState())
                && sqlException.getMessage() != null
                && !sqlException.getMessage().isBlank()) {
            return new BusinessException(40001, sqlException.getMessage());
        }
        return null;
    }

    private static SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }
}
