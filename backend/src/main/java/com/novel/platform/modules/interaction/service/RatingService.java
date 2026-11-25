package com.novel.platform.modules.interaction.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.util.IdUtils;
import com.novel.platform.modules.interaction.dto.NovelBrief;
import com.novel.platform.modules.interaction.dto.RatingAuditRequest;
import com.novel.platform.modules.interaction.dto.RatingReviewItem;
import com.novel.platform.modules.interaction.dto.RatingRow;
import com.novel.platform.modules.interaction.dto.RatingSummaryResponse;
import com.novel.platform.modules.interaction.dto.RatingUpsertRequest;
import com.novel.platform.modules.interaction.entity.NovelRating;
import com.novel.platform.modules.interaction.entity.NovelRatingStat;
import com.novel.platform.modules.interaction.mapper.AuditLogMapper;
import com.novel.platform.modules.interaction.mapper.NovelLookupMapper;
import com.novel.platform.modules.interaction.mapper.NovelRatingMapper;
import com.novel.platform.modules.interaction.mapper.NovelRatingStatMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RatingService {

    private final NovelRatingMapper ratingMapper;
    private final NovelRatingStatMapper statMapper;
    private final NovelLookupMapper novelLookupMapper;
    private final AuditLogMapper auditLogMapper;

    public RatingService(NovelRatingMapper ratingMapper, NovelRatingStatMapper statMapper,
                         NovelLookupMapper novelLookupMapper, AuditLogMapper auditLogMapper) {
        this.ratingMapper = ratingMapper;
        this.statMapper = statMapper;
        this.novelLookupMapper = novelLookupMapper;
        this.auditLogMapper = auditLogMapper;
    }

    public RatingSummaryResponse summary(String novelIdValue, Long viewerUserId) {
        Long novelId = IdUtils.parse(novelIdValue, "novelId");
        NovelBrief novel = novelLookupMapper.selectBrief(novelId);
        if (novel == null) {
            throw new BusinessException(40401, "作品不存在", HttpStatus.NOT_FOUND);
        }

        NovelRatingStat stat = statMapper.selectById(novelId);
        NovelRating mine = viewerUserId == null ? null : ratingMapper.selectOne(
                Wrappers.<NovelRating>lambdaQuery()
                        .eq(NovelRating::getUserId, viewerUserId)
                        .eq(NovelRating::getNovelId, novelId));

        return new RatingSummaryResponse(
                String.valueOf(novelId),
                novel.getTitle(),
                stat == null ? 0 : stat.getRatingCount(),
                stat == null ? 0L : stat.getScoreTotal(),
                stat == null ? BigDecimal.ZERO : stat.getAvgScore(),
                mine == null ? null : mine.getScore(),
                mine == null ? null : mine.getReviewContent(),
                mine == null ? null : mine.getReviewAuditStatus()
        );
    }

    @Transactional
    public RatingSummaryResponse save(RatingUpsertRequest request, Long userId) {
        Long novelId = IdUtils.parse(request.novelId(), "novelId");
        NovelBrief novel = novelLookupMapper.selectBrief(novelId);
        if (novel == null) {
            throw new BusinessException(40401, "作品不存在", HttpStatus.NOT_FOUND);
        }
        if (novel.getStatus() != null && novel.getStatus() == 2) {
            throw new BusinessException(40001, "作品已下架，不能评分");
        }

        String reviewContent = trimToNull(request.reviewContent());
        NovelRating existing = ratingMapper.selectOne(
                Wrappers.<NovelRating>lambdaQuery()
                        .eq(NovelRating::getUserId, userId)
                        .eq(NovelRating::getNovelId, novelId));

        if (existing == null) {
            NovelRating rating = new NovelRating();
            rating.setUserId(userId);
            rating.setNovelId(novelId);
            rating.setScore(request.score());
            rating.setReviewContent(reviewContent);
            rating.setStatus(1);
            ratingMapper.insert(rating);
        } else {
            existing.setScore(request.score());
            existing.setReviewContent(reviewContent);
            ratingMapper.updateById(existing);
        }
        return summary(String.valueOf(novelId), userId);
    }

    public PageResult<RatingReviewItem> publicReviews(String novelIdValue, int pageParam, int pageSizeParam) {
        Long novelId = IdUtils.parse(novelIdValue, "novelId");
        int page = Pagination.page(pageParam);
        int pageSize = Pagination.size(pageSizeParam);
        List<RatingRow> rows = ratingMapper.selectPublicReviews(
                novelId, Pagination.offset(page, pageSize), pageSize);
        long total = ratingMapper.countPublicReviews(novelId);
        List<RatingReviewItem> items = rows.stream().map(this::toItem).toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    public PageResult<RatingReviewItem> adminReviews(Integer reviewAuditStatus,
                                                     int pageParam, int pageSizeParam) {
        int page = Pagination.page(pageParam);
        int pageSize = Pagination.size(pageSizeParam);
        List<RatingRow> rows = ratingMapper.selectAdminReviews(
                reviewAuditStatus, Pagination.offset(page, pageSize), pageSize);
        long total = ratingMapper.countAdminReviews(reviewAuditStatus);
        List<RatingReviewItem> items = rows.stream().map(this::toItem).toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    @Transactional
    public void auditReview(Long ratingId, RatingAuditRequest request, Long adminId) {
        if (request.action() == 2 && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException(40000, "驳回时必须填写审核原因");
        }
        NovelRating rating = ratingMapper.selectById(ratingId);
        if (rating == null) {
            throw new BusinessException(40401, "评分记录不存在", HttpStatus.NOT_FOUND);
        }
        if (rating.getReviewContent() == null || rating.getReviewContent().isBlank()) {
            throw new BusinessException(40001, "该评分没有短评，无需审核");
        }

        int auditStatus = request.action() == 1 ? 1 : 2;
        ratingMapper.update(null, Wrappers.<NovelRating>lambdaUpdate()
                .eq(NovelRating::getRatingId, ratingId)
                .set(NovelRating::getReviewAuditStatus, auditStatus));
        auditLogMapper.insert(adminId, "RATING", ratingId, request.action(), trimToNull(request.reason()));
    }

    private RatingReviewItem toItem(RatingRow row) {
        return new RatingReviewItem(
                String.valueOf(row.getRatingId()),
                String.valueOf(row.getUserId()),
                row.getNickname(),
                row.getAvatarUrl(),
                String.valueOf(row.getNovelId()),
                row.getNovelTitle(),
                row.getScore() == null ? 0 : row.getScore(),
                row.getReviewContent(),
                row.getStatus() == null ? 0 : row.getStatus(),
                row.getReviewAuditStatus(),
                row.getCreatedAt()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
