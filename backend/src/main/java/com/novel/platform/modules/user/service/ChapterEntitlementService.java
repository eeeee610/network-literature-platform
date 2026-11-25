package com.novel.platform.modules.user.service;

import com.novel.platform.modules.user.dto.SubscriptionItem;
import com.novel.platform.modules.user.mapper.UserTradeMapper;
import org.springframework.stereotype.Service;

/**
 * A 模块阅读权益查询契约：只有未退款的有效订阅允许读取 VIP 正文。
 */
@Service
public class ChapterEntitlementService {

    private final UserTradeMapper tradeMapper;

    public ChapterEntitlementService(UserTradeMapper tradeMapper) {
        this.tradeMapper = tradeMapper;
    }

    public boolean hasActiveSubscription(Long userId, long chapterId) {
        if (userId == null) {
            return false;
        }
        SubscriptionItem subscription = tradeMapper.selectSubscription(userId, chapterId);
        return subscription != null && "ACTIVE".equals(subscription.status());
    }
}
