package com.springleaf.thinkdo.constant;

/**
 * 知识库多租户常量定义
 * <p>
 * 定义用户和系统级别的共享 Collection 和 Bucket 名称
 * </p>
 */
public class KnowledgeBaseConstant {

    /**
     * 用户知识库 Milvus Collection 名称
     * <p>
     * 所有普通用户的知识库向量数据共享此 Collection
     * </p>
     */
    public static final String USER_COLLECTION = "thinkdo_user_collection";

    /**
     * 用户知识库 S3 Bucket 名称
     * <p>
     * 所有普通用户的知识库文档文件共享此 Bucket
     * </p>
     */
    public static final String USER_BUCKET = "thinkdo-user-bucket";

    /**
     * 系统知识库 Milvus Collection 名称
     * <p>
     * 所有管理员的知识库向量数据共享此 Collection
     * </p>
     */
    public static final String SYSTEM_COLLECTION = "thinkdo_system_collection";

    /**
     * 系统知识库 S3 Bucket 名称
     * <p>
     * 所有管理员的知识库文档文件共享此 Bucket
     * </p>
     */
    public static final String SYSTEM_BUCKET = "thinkdo-system-bucket";

    private KnowledgeBaseConstant() {
        // 防止实例化
    }
}
