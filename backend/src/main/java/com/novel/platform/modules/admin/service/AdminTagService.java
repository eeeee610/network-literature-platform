package com.novel.platform.modules.admin.service;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.admin.dto.AdminTagItem;
import com.novel.platform.modules.admin.dto.AdminTagRow;
import com.novel.platform.modules.admin.dto.TagRequest;
import com.novel.platform.modules.admin.mapper.AdminTagMapper;
import com.novel.platform.modules.novel.entity.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminTagService {

    private final AdminTagMapper tagMapper;

    public AdminTagService(AdminTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public PageResult<AdminTagItem> list(String tagName, Integer isActive, int pageParam, int pageSizeParam) {
        int page = AdminPagination.page(pageParam);
        int pageSize = AdminPagination.size(pageSizeParam);
        String normalizedName = trimToNull(tagName);
        List<AdminTagItem> items = tagMapper.selectTags(
                        normalizedName, isActive, AdminPagination.offset(page, pageSize), pageSize)
                .stream().map(this::toItem).toList();
        long total = tagMapper.countTags(normalizedName, isActive);
        return new PageResult<>(items, total, page, pageSize);
    }

    @Transactional
    public AdminTagItem create(TagRequest request) {
        Tag tag = new Tag();
        tag.setTagName(normalizedName(request.tagName()));
        tag.setIsActive(request.isActive());
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException exception) {
            throw duplicateName();
        }
        return getItem(tag.getTagId());
    }

    @Transactional
    public AdminTagItem update(Long tagId, TagRequest request) {
        if (tagMapper.selectById(tagId) == null) {
            throw new BusinessException(40401, "标签不存在", HttpStatus.NOT_FOUND);
        }
        Tag tag = new Tag();
        tag.setTagId(tagId);
        tag.setTagName(normalizedName(request.tagName()));
        tag.setIsActive(request.isActive());
        try {
            tagMapper.updateById(tag);
        } catch (DuplicateKeyException exception) {
            throw duplicateName();
        }
        return getItem(tagId);
    }

    @Transactional
    public void delete(Long tagId) {
        if (tagMapper.selectById(tagId) == null) {
            throw new BusinessException(40401, "标签不存在", HttpStatus.NOT_FOUND);
        }
        if (tagMapper.countNovelReferences(tagId) > 0) {
            throw referencedTag();
        }
        try {
            tagMapper.deleteById(tagId);
        } catch (DataIntegrityViolationException exception) {
            throw referencedTag();
        }
    }

    private AdminTagItem getItem(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        return new AdminTagItem(String.valueOf(tag.getTagId()), tag.getTagName(), tag.getIsActive(),
                tagMapper.countNovelReferences(tagId), tag.getCreatedAt());
    }

    private AdminTagItem toItem(AdminTagRow row) {
        return new AdminTagItem(String.valueOf(row.getTagId()), row.getTagName(), row.getIsActive(),
                row.getNovelCount(), row.getCreatedAt());
    }

    private String normalizedName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty() || name.length() > 32) {
            throw new BusinessException(40000, "标签名称必填且不能超过 32 字");
        }
        return name;
    }

    private static BusinessException duplicateName() {
        return new BusinessException(40901, "标签名称已存在", HttpStatus.CONFLICT);
    }

    private static BusinessException referencedTag() {
        return new BusinessException(40902, "标签已被作品引用，请改为停用", HttpStatus.CONFLICT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
