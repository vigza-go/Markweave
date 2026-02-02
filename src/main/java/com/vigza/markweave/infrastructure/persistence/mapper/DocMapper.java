package com.vigza.markweave.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vigza.markweave.infrastructure.persistence.entity.Doc;

public interface DocMapper extends BaseMapper<Doc> {

    @Update("update doc set content = #{content} where id = #{doc_id}")
    void updateContent(@Param("doc_id") Long docId,@Param("content") String content);
}
