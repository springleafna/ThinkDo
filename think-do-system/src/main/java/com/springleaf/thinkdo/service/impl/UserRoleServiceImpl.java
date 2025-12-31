package com.springleaf.thinkdo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.UserRoleEntity;
import com.springleaf.thinkdo.mapper.UserRoleMapper;
import com.springleaf.thinkdo.service.UserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户角色关联Service实现
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRoleEntity> implements UserRoleService {
}
