package com.novel.platform.modules.user.service;

import com.novel.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 直接调用 A 模块已冻结的存储过程；订阅扣费、打赏的余额与流水一致性由数据库事务保证。
 */
@Service
public class TradeProcedureService {

    private final DataSource dataSource;

    public TradeProcedureService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ProcedureSubscribeResult subscribe(Long userId, Long chapterId, String requestNo) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = connection.prepareCall("{call sp_subscribe_chapter(?, ?, ?, ?, ?, ?, ?)}")) {
            statement.setLong(1, userId);
            statement.setLong(2, chapterId);
            statement.setString(3, requestNo);
            statement.registerOutParameter(4, Types.VARCHAR);
            statement.registerOutParameter(5, Types.VARCHAR);
            statement.registerOutParameter(6, Types.CHAR);
            statement.registerOutParameter(7, Types.DECIMAL);
            statement.execute();

            return new ProcedureSubscribeResult(
                    statement.getString(4),
                    statement.getString(5),
                    statement.getString(6),
                    statement.getBigDecimal(7)
            );
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    public ProcedureTipResult tip(Long fromUserId, Long toUserId, Long novelId, Long chapterId,
                                  BigDecimal amount, String message, String requestNo) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = connection.prepareCall(
                     "{call sp_tip_author(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            statement.setLong(1, fromUserId);
            statement.setLong(2, toUserId);
            statement.setLong(3, novelId);
            if (chapterId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, chapterId);
            }
            statement.setBigDecimal(5, amount);
            statement.setString(6, message == null ? "" : message);
            statement.setString(7, requestNo);
            statement.registerOutParameter(8, Types.VARCHAR);
            statement.registerOutParameter(9, Types.VARCHAR);
            statement.registerOutParameter(10, Types.BIGINT);
            statement.registerOutParameter(11, Types.CHAR);
            statement.registerOutParameter(12, Types.DECIMAL);
            statement.registerOutParameter(13, Types.DECIMAL);
            statement.execute();

            return new ProcedureTipResult(
                    statement.getString(8),
                    statement.getString(9),
                    statement.getString(11),
                    statement.getBigDecimal(12),
                    statement.getBigDecimal(13)
            );
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    private BusinessException translate(SQLException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (exception.getErrorCode() == 1644 || "45000".equals(exception.getSQLState())) {
            if (message.contains("余额不足")) {
                return new BusinessException(40901, "账户余额不足", HttpStatus.CONFLICT);
            }
            if (message.contains("已订阅")) {
                return new BusinessException(40902, "该章节已订阅", HttpStatus.CONFLICT);
            }
            return new BusinessException(40903, message.isBlank() ? "交易未完成" : message, HttpStatus.CONFLICT);
        }
        if (exception.getErrorCode() == 1062) {
            return new BusinessException(40904, "请勿重复提交", HttpStatus.CONFLICT);
        }
        return new BusinessException(50000, "交易处理失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public record ProcedureSubscribeResult(String resultCode, String message, String txnNo,
                                           BigDecimal balanceAfter) {
    }

    public record ProcedureTipResult(String resultCode, String message, String tipNo,
                                     BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter) {
    }
}
