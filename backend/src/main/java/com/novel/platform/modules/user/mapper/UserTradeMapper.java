package com.novel.platform.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.user.dto.SubscribeParam;
import com.novel.platform.modules.user.dto.SubscriptionItem;
import com.novel.platform.modules.user.dto.TipItem;
import com.novel.platform.modules.user.dto.TipParam;
import com.novel.platform.modules.user.entity.AccountTransaction;
import com.novel.platform.modules.user.entity.ChapterSubscription;
import com.novel.platform.modules.user.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTradeMapper extends BaseMapper<AccountTransaction> {

    void subscribe(SubscribeParam param);

    void tip(TipParam param);

    Long selectNovelAuthorId(@Param("novelId") Long novelId);

    long countTransactions(@Param("userId") Long userId);

    List<AccountTransaction> selectTransactionPage(@Param("userId") Long userId,
                                                   @Param("offset") long offset,
                                                   @Param("pageSize") int pageSize);

    UserAccount selectAccountForUpdate(@Param("userId") Long userId);

    ChapterSubscription selectSubscriptionForUpdate(@Param("userId") Long userId,
                                                    @Param("chapterId") Long chapterId);

    int markSubscriptionRefunded(@Param("userId") Long userId, @Param("chapterId") Long chapterId);

    SubscriptionItem selectSubscription(@Param("userId") Long userId, @Param("chapterId") Long chapterId);

    SubscriptionItem selectSubscriptionByRequestNo(@Param("userId") Long userId,
                                                   @Param("requestNo") String requestNo);

    long countSubscriptions(@Param("userId") Long userId);

    List<SubscriptionItem> selectSubscriptionPage(@Param("userId") Long userId,
                                                  @Param("offset") long offset,
                                                  @Param("pageSize") int pageSize);

    long countTips(@Param("userId") Long userId, @Param("direction") String direction);

    List<TipItem> selectTipPage(@Param("userId") Long userId,
                                @Param("direction") String direction,
                                @Param("offset") long offset,
                                @Param("pageSize") int pageSize);

    TipItem selectTipByRequestNo(@Param("userId") Long userId, @Param("requestNo") String requestNo);
}
