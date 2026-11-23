package com.novel.content.mapper;

import com.novel.content.dto.NovelDetailVO;
import com.novel.content.dto.NovelListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelMapper {

    @Select("<script>"
            + "SELECT n.novel_id, n.author_id, n.title, n.cover_url, n.description, n.status, "
            + "n.total_words, n.latest_chapter_id, n.updated_at, u.nickname AS author_name "
            + "FROM novel n JOIN app_user u ON u.user_id = n.author_id "
            + "<if test='tagId != null'>JOIN novel_tag nt ON nt.novel_id = n.novel_id AND nt.tag_id = #{tagId} </if>"
            + "WHERE n.audit_status = 1 AND n.status IN (0, 1) "
            + "<if test='status != null'>AND n.status = #{status} </if>"
            + "<choose>"
            + "<when test='sortCode == 1'>ORDER BY n.total_words DESC, n.novel_id DESC</when>"
            + "<when test='sortCode == 2'>ORDER BY n.created_at DESC, n.novel_id DESC</when>"
            + "<otherwise>ORDER BY n.updated_at DESC, n.novel_id DESC</otherwise>"
            + "</choose> "
            + "LIMIT #{offset}, #{size}"
            + "</script>")
    List<NovelListItem> selectBookstore(@Param("status") Integer status,
                                        @Param("tagId") Long tagId,
                                        @Param("sortCode") int sortCode,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    @Select("<script>"
            + "SELECT COUNT(*) FROM novel n "
            + "<if test='tagId != null'>JOIN novel_tag nt ON nt.novel_id = n.novel_id AND nt.tag_id = #{tagId} </if>"
            + "WHERE n.audit_status = 1 AND n.status IN (0, 1) "
            + "<if test='status != null'>AND n.status = #{status} </if>"
            + "</script>")
    long countBookstore(@Param("status") Integer status, @Param("tagId") Long tagId);

    @Select("<script>"
            + "SELECT n.novel_id, n.author_id, n.title, n.cover_url, n.description, n.status, "
            + "n.total_words, n.latest_chapter_id, n.updated_at, u.nickname AS author_name "
            + "FROM novel n JOIN app_user u ON u.user_id = n.author_id "
            + "<if test='tagId != null'>JOIN novel_tag nt ON nt.novel_id = n.novel_id AND nt.tag_id = #{tagId} </if>"
            + "WHERE n.audit_status = 1 AND n.status IN (0, 1) "
            + "<if test='keyword != null'>"
            + "AND (n.title LIKE CONCAT(#{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%')) "
            + "</if>"
            + "ORDER BY n.updated_at DESC, n.novel_id DESC "
            + "LIMIT #{offset}, #{size}"
            + "</script>")
    List<NovelListItem> selectSearch(@Param("keyword") String keyword,
                                     @Param("tagId") Long tagId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    @Select("<script>"
            + "SELECT COUNT(*) FROM novel n "
            + "<if test='tagId != null'>JOIN novel_tag nt ON nt.novel_id = n.novel_id AND nt.tag_id = #{tagId} </if>"
            + "WHERE n.audit_status = 1 AND n.status IN (0, 1) "
            + "<if test='keyword != null'>"
            + "AND (n.title LIKE CONCAT(#{keyword}, '%') OR n.author_id IN "
            + "(SELECT user_id FROM app_user WHERE nickname LIKE CONCAT('%', #{keyword}, '%'))) "
            + "</if>"
            + "</script>")
    long countSearch(@Param("keyword") String keyword, @Param("tagId") Long tagId);

    @Select("SELECT n.novel_id, n.author_id, n.title, n.cover_url, n.description, n.status, "
            + "n.total_words, n.latest_chapter_id, n.updated_at, u.nickname AS author_name "
            + "FROM novel n JOIN app_user u ON u.user_id = n.author_id "
            + "WHERE n.novel_id = #{novelId} AND n.audit_status = 1 AND n.status IN (0, 1)")
    NovelDetailVO selectDetail(@Param("novelId") Long novelId);

    @Select("SELECT COUNT(*) FROM novel WHERE novel_id = #{novelId} AND audit_status = 1 AND status IN (0, 1)")
    int countDisplayable(@Param("novelId") Long novelId);

    @Select("SELECT author_id FROM novel WHERE novel_id = #{novelId}")
    Long selectAuthorId(@Param("novelId") Long novelId);

    @Select("<script>"
            + "SELECT n.novel_id, n.author_id, n.title, n.cover_url, n.description, n.status, n.audit_status, "
            + "n.total_words, n.latest_chapter_id, n.updated_at, u.nickname AS author_name, "
            + "(SELECT COUNT(*) FROM chapter c WHERE c.novel_id = n.novel_id "
            + "AND c.status = 1 AND c.audit_status = 1 AND c.publish_time IS NOT NULL) AS chapter_count "
            + "FROM novel n JOIN app_user u ON u.user_id = n.author_id "
            + "WHERE n.author_id = #{authorId} "
            + "<if test='status != null'>AND n.status = #{status} </if>"
            + "ORDER BY n.updated_at DESC, n.novel_id DESC "
            + "LIMIT #{offset}, #{size}"
            + "</script>")
    List<NovelListItem> selectByAuthor(@Param("authorId") long authorId,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    @Select("<script>"
            + "SELECT COUNT(*) FROM novel n WHERE n.author_id = #{authorId} "
            + "<if test='status != null'>AND n.status = #{status} </if>"
            + "</script>")
    long countByAuthor(@Param("authorId") long authorId, @Param("status") Integer status);
}
