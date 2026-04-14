package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.domain.entity.NoteCategoryEntity;
import com.springleaf.thinkdo.domain.entity.NoteEntity;
import com.springleaf.thinkdo.domain.entity.UserEntity;
import com.springleaf.thinkdo.domain.request.*;
import com.springleaf.thinkdo.domain.response.AdminNoteDetailResp;
import com.springleaf.thinkdo.domain.response.AdminNoteInfoResp;
import com.springleaf.thinkdo.domain.response.NoteInfoResp;
import com.springleaf.thinkdo.domain.response.NoteListItemResp;
import com.springleaf.thinkdo.domain.response.NoteStatisticsResp;
import com.springleaf.thinkdo.enums.AiActionEnum;
import com.springleaf.thinkdo.enums.NoteFavoritedEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.NoteCategoryMapper;
import com.springleaf.thinkdo.mapper.NoteMapper;
import com.springleaf.thinkdo.mapper.UserMapper;
import com.springleaf.thinkdo.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.springleaf.thinkdo.constant.NoteConstant;
import com.springleaf.thinkdo.domain.dto.StoredFileDTO;
import com.springleaf.thinkdo.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 笔记Service实现
 */
@Service
@Slf4j
public class NoteServiceImpl extends ServiceImpl<NoteMapper, NoteEntity> implements NoteService {

    private final NoteMapper noteMapper;
    private final NoteCategoryMapper noteCategoryMapper;
    private final UserMapper userMapper;
    private final ChatClient chatClient;
    private final ResourceLoader resourceLoader;
    private final FileStorageService fileStorageService;

    public NoteServiceImpl(NoteMapper noteMapper, NoteCategoryMapper noteCategoryMapper, UserMapper userMapper, ChatClient.Builder builder, ResourceLoader resourceLoader, FileStorageService fileStorageService) {
        this.noteMapper = noteMapper;
        this.noteCategoryMapper = noteCategoryMapper;
        this.userMapper = userMapper;
        this.chatClient = builder.build();
        this.resourceLoader = resourceLoader;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNote(CreateNoteReq createNoteReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 如果指定了分类ID，验证分类是否存在且属于当前用户
        if (createNoteReq.getCategoryId() != null) {
            validateCategoryOwnership(createNoteReq.getCategoryId(), userId);
        }

        NoteEntity note = new NoteEntity();
        note.setUserId(userId);
        note.setTitle(createNoteReq.getTitle());
        note.setContent(createNoteReq.getContent());
        note.setPreview(generatePreview(createNoteReq.getContent()));
        note.setCategoryId(createNoteReq.getCategoryId());
        note.setTags(createNoteReq.getTags());

        noteMapper.insert(note);
        log.info("创建笔记成功, userId={}, noteId={}", userId, note.getId());

        return note.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNote(UpdateNoteReq updateNoteReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(updateNoteReq.getId());
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此笔记");
        }

        // 如果指定了分类ID，验证分类是否存在且属于当前用户
        if (updateNoteReq.getCategoryId() != null) {
            validateCategoryOwnership(updateNoteReq.getCategoryId(), userId);
        }

        // 更新字段
        if (StringUtils.hasText(updateNoteReq.getTitle())) {
            note.setTitle(updateNoteReq.getTitle());
        }
        if (updateNoteReq.getContent() != null) {
            note.setContent(updateNoteReq.getContent());
            note.setPreview(generatePreview(updateNoteReq.getContent()));
        }
        if (updateNoteReq.getCategoryId() != null) {
            note.setCategoryId(updateNoteReq.getCategoryId());
        }
        if (updateNoteReq.getTags() != null) {
            note.setTags(updateNoteReq.getTags());
        }

        noteMapper.updateById(note);
        log.info("更新笔记成功, userId={}, noteId={}", userId, note.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此笔记");
        }

        noteMapper.deleteById(id);
        log.info("删除笔记成功, userId={}, noteId={}", userId, id);
    }

    @Override
    public NoteInfoResp getNoteById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此笔记");
        }

        return convertToResp(note);
    }

    @Override
    public List<NoteListItemResp> getNoteList(NoteQueryReq queryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteEntity::getUserId, userId);

        // 按分类筛选（包括未分类的笔记）
        if (queryReq.getCategoryId() != null) {
            wrapper.eq(NoteEntity::getCategoryId, queryReq.getCategoryId());
        }

        // 按收藏状态筛选
        if (queryReq.getFavorited() != null) {
            wrapper.eq(NoteEntity::getFavorited, queryReq.getFavorited());
        }

        // 关键词搜索
        if (StringUtils.hasText(queryReq.getKeyword())) {
            String keyword = queryReq.getKeyword();
            wrapper.and(w -> w.like(NoteEntity::getTitle, keyword)
                    .or()
                    .like(NoteEntity::getContent, keyword));
        }

        // 排序：收藏的在前，然后按更新时间倒序
        wrapper.orderByDesc(NoteEntity::getFavorited)
                .orderByDesc(NoteEntity::getUpdatedAt);

        List<NoteEntity> noteList = noteMapper.selectList(wrapper);

        // 批量获取分类名称
        List<Long> categoryIds = noteList.stream()
                .map(NoteEntity::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> categoryNameMap = Map.of();
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<NoteCategoryEntity> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.in(NoteCategoryEntity::getId, categoryIds);
            categoryWrapper.eq(NoteCategoryEntity::getUserId, userId);
            categoryWrapper.select(NoteCategoryEntity::getId, NoteCategoryEntity::getName);
            List<NoteCategoryEntity> categories = noteCategoryMapper.selectList(categoryWrapper);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(NoteCategoryEntity::getId, NoteCategoryEntity::getName));
        }

        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        return noteList.stream()
                .map(note -> {
                    NoteListItemResp resp = convertToListItemResp(note);
                    if (note.getCategoryId() != null) {
                        resp.setCategoryName(finalCategoryNameMap.get(note.getCategoryId()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<NoteListItemResp> searchNotes(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteEntity::getUserId, userId);
        wrapper.and(w -> w.like(NoteEntity::getTitle, keyword)
                .or()
                .like(NoteEntity::getContent, keyword));
        wrapper.orderByDesc(NoteEntity::getUpdatedAt);

        List<NoteEntity> noteList = noteMapper.selectList(wrapper);
        return noteList.stream()
                .map(this::convertToListItemResp)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleFavorited(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此笔记");
        }

        // 切换收藏状态
        NoteFavoritedEnum currentStatus = NoteFavoritedEnum.getByCode(note.getFavorited());
        NoteFavoritedEnum newStatus = currentStatus.toggle();
        note.setFavorited(newStatus.getCode());
        noteMapper.updateById(note);
        log.info("笔记{}成功, userId={}, noteId={}", newStatus.getDesc(), userId, id);
    }

    @Override
    public NoteStatisticsResp getStatistics() {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteStatisticsResp resp = new NoteStatisticsResp();

        // 全部笔记数量
        LambdaQueryWrapper<NoteEntity> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(NoteEntity::getUserId, userId);
        Long totalCount = noteMapper.selectCount(totalWrapper);
        resp.setTotalCount(totalCount.intValue());

        // 收藏笔记数量
        LambdaQueryWrapper<NoteEntity> favoritedWrapper = new LambdaQueryWrapper<>();
        favoritedWrapper.eq(NoteEntity::getUserId, userId);
        favoritedWrapper.eq(NoteEntity::getFavorited, NoteFavoritedEnum.FAVORITED.getCode());
        Long favoritedCount = noteMapper.selectCount(favoritedWrapper);
        resp.setFavoritedCount(favoritedCount.intValue());

        // 未分类笔记数量
        LambdaQueryWrapper<NoteEntity> unclassifiedWrapper = new LambdaQueryWrapper<>();
        unclassifiedWrapper.eq(NoteEntity::getUserId, userId);
        unclassifiedWrapper.isNull(NoteEntity::getCategoryId);
        Long unclassifiedCount = noteMapper.selectCount(unclassifiedWrapper);
        resp.setUnclassifiedCount(unclassifiedCount.intValue());

        // 获取所有分类
        LambdaQueryWrapper<NoteCategoryEntity> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(NoteCategoryEntity::getUserId, userId);
        List<NoteCategoryEntity> categories = noteCategoryMapper.selectList(categoryWrapper);

        // 各分类笔记数量
        List<NoteStatisticsResp.CategoryCount> categoryCounts = categories.stream()
                .map(category -> {
                    LambdaQueryWrapper<NoteEntity> noteWrapper = new LambdaQueryWrapper<>();
                    noteWrapper.eq(NoteEntity::getUserId, userId);
                    noteWrapper.eq(NoteEntity::getCategoryId, category.getId());
                    Long count = noteMapper.selectCount(noteWrapper);

                    NoteStatisticsResp.CategoryCount categoryCount = new NoteStatisticsResp.CategoryCount();
                    categoryCount.setCategoryId(category.getId());
                    categoryCount.setCategoryName(category.getName());
                    categoryCount.setCount(count.intValue());
                    return categoryCount;
                })
                .collect(Collectors.toList());

        resp.setCategoryCounts(categoryCounts);
        return resp;
    }

    @Override
    public Flux<String> aiTransformStream(AiTransformReq req) {
        PromptTemplate template = new PromptTemplate(selectTemplate(req.getAction()));
        Map<String, Object> params = buildParams(req);
        Prompt prompt = template.create(params);

        return chatClient.prompt(prompt)
                .stream()
                .content();
    }

    /**
     * 构建提示词参数
     */
    private Map<String, Object> buildParams(AiTransformReq req) {
        String tone = Optional.ofNullable(req.getOptions())
                .map(AiOptions::getTone)
                .orElse(NoteConstant.DEFAULT_TONE);

        String length = Optional.ofNullable(req.getOptions())
                .map(AiOptions::getTargetLength)
                .orElse(NoteConstant.DEFAULT_LENGTH);

        String language = Optional.ofNullable(req.getOptions())
                .map(AiOptions::getLanguage)
                .orElse(NoteConstant.DEFAULT_LANGUAGE);

        return Map.of(
                "text", req.getText(),
                "tone", tone,
                "length", length,
                "language", language
        );
    }

    /**
     * 根据操作类型选择对应的提示词模板
     */
    private Resource selectTemplate(AiActionEnum action) {
        String templatePath = NoteConstant.PROMPT_TEMPLATE_PREFIX + action.name().toLowerCase() + NoteConstant.PROMPT_TEMPLATE_SUFFIX;
        return resourceLoader.getResource(templatePath);
    }

    /**
     * 验证分类所有权
     */
    private void validateCategoryOwnership(Long categoryId, Long userId) {
        LambdaQueryWrapper<NoteCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteCategoryEntity::getId, categoryId);
        wrapper.eq(NoteCategoryEntity::getUserId, userId);
        Long count = noteCategoryMapper.selectCount(wrapper);
        if (count == 0) {
            throw new BusinessException("分类不存在或无权访问");
        }
    }

    /**
     * 转换为响应对象
     */
    private NoteInfoResp convertToResp(NoteEntity note) {
        NoteInfoResp resp = new NoteInfoResp();
        BeanUtils.copyProperties(note, resp);
        return resp;
    }

    /**
     * 转换为列表项响应对象
     */
    private NoteListItemResp convertToListItemResp(NoteEntity note) {
        NoteListItemResp resp = new NoteListItemResp();
        resp.setId(note.getId());
        resp.setTitle(note.getTitle());
        resp.setPreview(note.getPreview());
        resp.setCategoryId(note.getCategoryId());
        resp.setTags(note.getTags());
        resp.setFavorited(note.getFavorited());
        resp.setCreatedAt(note.getCreatedAt());
        resp.setUpdatedAt(note.getUpdatedAt());
        return resp;
    }

    /**
     * 生成预览内容（去除HTML标签，截取前N个字符）
     */
    private String generatePreview(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        // 使用 Jsoup 去除 HTML 标签
        String plainText = Jsoup.parse(content).text();
        // 截取前N个字符
        if (plainText.length() > NoteConstant.PREVIEW_MAX_LENGTH) {
            return plainText.substring(0, NoteConstant.PREVIEW_MAX_LENGTH);
        }
        return plainText;
    }

    @Override
    public List<NoteListItemResp> getRecentNotes() {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteEntity::getUserId, userId);
        wrapper.orderByDesc(NoteEntity::getUpdatedAt);
        wrapper.last("LIMIT 2");

        List<NoteEntity> noteList = noteMapper.selectList(wrapper);

        // 批量获取分类名称
        List<Long> categoryIds = noteList.stream()
                .map(NoteEntity::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> categoryNameMap = Map.of();
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<NoteCategoryEntity> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.in(NoteCategoryEntity::getId, categoryIds);
            categoryWrapper.eq(NoteCategoryEntity::getUserId, userId);
            categoryWrapper.select(NoteCategoryEntity::getId, NoteCategoryEntity::getName);
            List<NoteCategoryEntity> categories = noteCategoryMapper.selectList(categoryWrapper);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(NoteCategoryEntity::getId, NoteCategoryEntity::getName));
        }

        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        return noteList.stream()
                .map(note -> {
                    NoteListItemResp resp = convertToListItemResp(note);
                    if (note.getCategoryId() != null) {
                        resp.setCategoryName(finalCategoryNameMap.get(note.getCategoryId()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只支持上传图片文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过5MB");
        }
        String userId = StpUtil.getLoginIdAsString();
        StoredFileDTO stored = fileStorageService.upload(NoteConstant.NOTE_IMAGE_BUCKET, file, userId + NoteConstant.NOTE_IMAGE_PATH_SUFFIX);
        return stored.getUrl();
    }

    // ==================== 管理员方法 ====================

    @Override
    public PageResp<AdminNoteInfoResp> adminListNotes(AdminNoteQueryReq queryReq) {
        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<>();

        // 用户ID筛选
        if (queryReq.getUserId() != null) {
            wrapper.eq(NoteEntity::getUserId, queryReq.getUserId());
        }

        // 用户名模糊搜索：先查出匹配的用户ID
        if (StringUtils.hasText(queryReq.getUsername())) {
            List<Long> matchedUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<UserEntity>().like(UserEntity::getUsername, queryReq.getUsername())
            ).stream().map(UserEntity::getId).collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return PageResp.of(List.of(), 0L, queryReq.getPageNum(), queryReq.getPageSize());
            }
            wrapper.in(NoteEntity::getUserId, matchedUserIds);
        }

        // 收藏状态筛选
        if (queryReq.getFavorited() != null) {
            wrapper.eq(NoteEntity::getFavorited, queryReq.getFavorited());
        }

        // 关键词搜索
        if (StringUtils.hasText(queryReq.getKeyword())) {
            String keyword = queryReq.getKeyword();
            wrapper.and(w -> w.like(NoteEntity::getTitle, keyword)
                    .or()
                    .like(NoteEntity::getContent, keyword));
        }

        wrapper.orderByDesc(NoteEntity::getUpdatedAt);

        IPage<NoteEntity> page = new Page<>(queryReq.getPageNum(), queryReq.getPageSize());
        IPage<NoteEntity> result = noteMapper.selectPage(page, wrapper);

        // 批量解析用户名和分类名
        Map<Long, String> usernameMap = batchGetUsernames(
                result.getRecords().stream().map(NoteEntity::getUserId).collect(Collectors.toSet())
        );
        Map<Long, String> categoryNameMap = batchGetCategoryNames(
                result.getRecords().stream().map(NoteEntity::getCategoryId)
                        .filter(id -> id != null).collect(Collectors.toSet())
        );

        return PageResp.of(result, note -> {
            AdminNoteInfoResp resp = new AdminNoteInfoResp();
            resp.setId(note.getId());
            resp.setUserId(note.getUserId());
            resp.setUsername(usernameMap.getOrDefault(note.getUserId(), ""));
            resp.setTitle(note.getTitle());
            resp.setPreview(note.getPreview());
            resp.setCategoryId(note.getCategoryId());
            resp.setCategoryName(note.getCategoryId() != null ? categoryNameMap.get(note.getCategoryId()) : null);
            resp.setTags(note.getTags());
            resp.setFavorited(note.getFavorited());
            resp.setCreatedAt(note.getCreatedAt());
            resp.setUpdatedAt(note.getUpdatedAt());
            return resp;
        });
    }

    @Override
    public AdminNoteDetailResp adminGetNoteDetail(Long id) {
        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        AdminNoteDetailResp resp = new AdminNoteDetailResp();
        resp.setId(note.getId());
        resp.setUserId(note.getUserId());
        resp.setUsername(getUsernameById(note.getUserId()));
        resp.setTitle(note.getTitle());
        resp.setContent(note.getContent());
        resp.setPreview(note.getPreview());
        resp.setCategoryId(note.getCategoryId());
        resp.setTags(note.getTags());
        resp.setFavorited(note.getFavorited());
        resp.setCreatedAt(note.getCreatedAt());
        resp.setUpdatedAt(note.getUpdatedAt());

        // 解析分类名
        if (note.getCategoryId() != null) {
            NoteCategoryEntity category = noteCategoryMapper.selectById(note.getCategoryId());
            if (category != null) {
                resp.setCategoryName(category.getName());
            }
        }

        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteNote(Long id) {
        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        noteMapper.deleteById(id);
        log.info("管理员删除笔记成功, noteId={}, userId={}", id, note.getUserId());
    }

    /**
     * 批量获取用户名映射
     */
    private Map<Long, String> batchGetUsernames(java.util.Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername));
    }

    /**
     * 批量获取分类名映射（不限定用户）
     */
    private Map<Long, String> batchGetCategoryNames(java.util.Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return Map.of();
        List<NoteCategoryEntity> categories = noteCategoryMapper.selectBatchIds(categoryIds);
        return categories.stream().collect(Collectors.toMap(NoteCategoryEntity::getId, NoteCategoryEntity::getName));
    }

    /**
     * 根据用户ID获取用户名
     */
    private String getUsernameById(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : "";
    }
}
