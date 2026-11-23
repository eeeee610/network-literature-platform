package com.novel.content.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;

/**
 * 全部写操作走存储过程（过程内自带事务），此处不加 @Transactional。
 */
@Repository
public class ProcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createNovel(long authorId, String title, String coverUrl, String description, List<Long> tagIds) {
        String tagJson = (tagIds == null || tagIds.isEmpty()) ? null : toJson(tagIds);
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try (CallableStatement cs = connection.prepareCall("{call sp_create_novel(?, ?, ?, ?, ?, ?)}")) {
                cs.setLong(1, authorId);
                cs.setString(2, title);
                setNullableString(cs, 3, coverUrl);
                setNullableString(cs, 4, description);
                setNullableString(cs, 5, tagJson);
                cs.registerOutParameter(6, Types.BIGINT);
                cs.execute();
                return cs.getLong(6);
            }
        });
    }

    public Long saveChapterDraft(Long chapterId, long novelId, String title, String content,
                                 int isVip, BigDecimal vipPrice) {
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try (CallableStatement cs = connection.prepareCall("{call sp_save_chapter_draft(?, ?, ?, ?, ?, ?, ?)}")) {
                if (chapterId == null) {
                    cs.setNull(1, Types.BIGINT);
                } else {
                    cs.setLong(1, chapterId);
                }
                cs.setLong(2, novelId);
                cs.setString(3, title);
                cs.setString(4, content);
                cs.setInt(5, isVip);
                cs.setBigDecimal(6, vipPrice);
                cs.registerOutParameter(7, Types.BIGINT);
                cs.execute();
                return cs.getLong(7);
            }
        });
    }

    public void publishChapter(long chapterId) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (CallableStatement cs = connection.prepareCall("{call sp_publish_chapter(?)}")) {
                cs.setLong(1, chapterId);
                cs.execute();
            }
            return null;
        });
    }

    public void offlineChapter(long chapterId) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (CallableStatement cs = connection.prepareCall("{call sp_offline_chapter(?)}")) {
                cs.setLong(1, chapterId);
                cs.execute();
            }
            return null;
        });
    }

    private void setNullableString(CallableStatement cs, int index, String value) throws java.sql.SQLException {
        if (value == null) {
            cs.setNull(index, Types.VARCHAR);
        } else {
            cs.setString(index, value);
        }
    }

    private String toJson(List<Long> tagIds) {
        try {
            return objectMapper.writeValueAsString(tagIds);
        } catch (Exception e) {
            throw new IllegalStateException("标签参数序列化失败", e);
        }
    }
}
