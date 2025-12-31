package com.springleaf.thinkdo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.RoleEntity;
import com.springleaf.thinkdo.mapper.RoleMapper;
import com.springleaf.thinkdo.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色Service实现
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RoleEntity> implements RoleService {
}
