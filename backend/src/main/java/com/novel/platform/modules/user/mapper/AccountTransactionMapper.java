package com.novel.platform.modules.user.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.platform.modules.user.dto.TransactionItemResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface AccountTransactionMapper {

    @Select("""
            SELECT txn_no AS txnNo,
                   biz_type AS bizType,
                   direction AS direction,
                   amount AS amount,
                   balance_before AS balanceBefore,
                   balance_after AS balanceAfter,
                   related_type AS relatedType,
                   related_id AS relatedId,
                   remark AS remark,
                   created_at AS createdAt
            FROM account_transaction
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, txn_id DESC
            """)
    IPage<TransactionItemResponse> selectTransactionPage(Page<TransactionItemResponse> page,
                                                          @Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(SUM(CASE WHEN direction = 'IN' THEN amount ELSE 0 END), 0.00)
            FROM account_transaction
            WHERE user_id = #{userId}
            """)
    BigDecimal sumIncome(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(SUM(CASE WHEN direction = 'OUT' THEN amount ELSE 0 END), 0.00)
            FROM account_transaction
            WHERE user_id = #{userId}
            """)
    BigDecimal sumConsumption(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM account_transaction WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);
}
