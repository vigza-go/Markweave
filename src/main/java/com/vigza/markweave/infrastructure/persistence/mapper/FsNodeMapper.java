package com.vigza.markweave.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vigza.markweave.infrastructure.persistence.entity.FsNode;

public interface FsNodeMapper extends BaseMapper<FsNode> {

    @Update("update fs_node set path = replace(path,#{oldPathPrefix},#{newPathPrefix})" + 
        "where path like concat(#{oldPathPrefix},'%')"
    )
    public void updateChildPaths(@Param("oldPathPrefix") String oldPathPrefix,@Param("newPathPrefix") String newPathPrefix);

    @Update("update fs_node set recycled = 1 where path like concat(#{pathPrefix},'%')")
    public void recycleChildPaths(@Param("pathPrefix") String pathPrefix);
}
