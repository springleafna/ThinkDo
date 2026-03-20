package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {

    /**
     * 查询用户的角色名称
     *
     * @param userId 用户ID
     * @return 角色名称，如 "USER"、"ADMIN"
     */
    @Select("SELECT r.name FROM tb_role r " +
            "INNER JOIN tb_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "LIMIT 1")
    String getUserRoleName(Long userId);
}
