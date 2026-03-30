package com.springleaf.thinkdo.constant;

public final class NoteConstant {

    private NoteConstant() {
    }

    /** 提示词模板路径前缀 */
    public static final String PROMPT_TEMPLATE_PREFIX = "classpath:prompts/ai-";

    /** 提示词模板路径后缀 */
    public static final String PROMPT_TEMPLATE_SUFFIX = ".st";

    /** HTML 安全标签白名单 */
    public static final String[] SAFE_HTML_TAGS = {"h1", "h2", "h3", "p", "ul", "ol",
            "li", "blockquote", "code", "pre", "a", "strong", "em", "del"};

    /** HTML 安全属性白名单（格式：标签名:属性名） */
    public static final String[][] SAFE_HTML_ATTRIBUTES = {{"a", "href"}};

    /** 默认语气 */
    public static final String DEFAULT_TONE = "neutral";

    /** 默认扩写程度 */
    public static final String DEFAULT_LENGTH = "medium";

    /** 默认语言 */
    public static final String DEFAULT_LANGUAGE = "zh";

    /** 预览内容最大长度 */
    public static final int PREVIEW_MAX_LENGTH = 100;

    /** 笔记图片存储桶名称 */
    public static final String NOTE_IMAGE_BUCKET = "thinkdo-note-image-bucket";

    /** 笔记图片存储路径模板（拼接用户ID） */
    public static final String NOTE_IMAGE_PATH_SUFFIX = "/notes/";
}
