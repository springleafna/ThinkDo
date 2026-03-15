package com.springleaf.thinkdo.prompt;

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 提示词模板工具类
 * 提供提示词的清理和槽位填充功能
 */
public final class PromptTemplateUtils {
    /**
     * 匹配多个连续空行的正则表达式模式（3 个及以上换行符）
     */
    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("(\\n){3,}");

    /**
     * 清理提示词文本
     * 将多个连续空行替换为单个空行，并去除首尾空白字符
     *
     * @param prompt 待清理的提示词文本
     * @return 清理后的提示词，如果输入为 null 则返回空字符串
     */
    public static String cleanupPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        return MULTI_BLANK_LINES.matcher(prompt).replaceAll("\n\n").trim();
    }

    /**
     * 填充模板中的槽位变量
     * 将模板中形如 {key} 的占位符替换为对应的值
     *
     * @param template 包含占位符的模板字符串
     * @param slots    槽位键值对映射，key 为占位符名称，value 为替换值
     * @return 填充后的模板字符串，如果模板为 null 返回空字符串，如果槽位为空则返回原模板
     */
    public static String fillSlots(String template, Map<String, String> slots) {
        if (template == null) {
            return "";
        }
        if (slots == null || slots.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : slots.entrySet()) {
            String value = StrUtil.emptyIfNull(entry.getValue());
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }
}

