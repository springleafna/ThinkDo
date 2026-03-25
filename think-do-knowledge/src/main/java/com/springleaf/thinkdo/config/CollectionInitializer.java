package com.springleaf.thinkdo.config;

import com.springleaf.thinkdo.constant.KnowledgeBaseConstant;
import com.springleaf.thinkdo.domain.dto.VectorSpaceId;
import com.springleaf.thinkdo.domain.dto.VectorSpaceSpec;
import com.springleaf.thinkdo.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

/**
 * 知识库 Collection 和 Bucket 初始化器
 * <p>
 * 在应用启动时自动创建共享的 Milvus Collection 和 S3 Bucket
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionInitializer implements ApplicationRunner {

    private final VectorStoreService vectorStoreService;
    private final S3Client s3Client;

    @Override
    public void run(ApplicationArguments args) {
        // 初始化用户 Collection 和 Bucket
        initCollectionAndBucket(KnowledgeBaseConstant.USER_COLLECTION,
                KnowledgeBaseConstant.USER_BUCKET, "用户知识库");

        // 初始化系统 Collection 和 Bucket
        initCollectionAndBucket(KnowledgeBaseConstant.SYSTEM_COLLECTION,
                KnowledgeBaseConstant.SYSTEM_BUCKET, "系统知识库");
    }

    /**
     * 初始化 Collection 和 Bucket
     * <p>
     * 容错处理：如果 S3 或 Milvus 不可用，只记录警告而不抛出异常
     *
     * @param collectionName Collection 名称
     * @param bucketName     Bucket 名称
     * @param description    描述信息
     */
    private void initCollectionAndBucket(String collectionName, String bucketName, String description) {
        // 检查并创建 Bucket
        try {
            s3Client.createBucket(b -> b.bucket(bucketName));
            log.info("成功创建 Bucket: {}", bucketName);
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("Bucket 已存在（由本账户拥有）: {}", bucketName);
        } catch (BucketAlreadyExistsException e) {
            log.info("Bucket 已存在（由其他账户拥有）: {}", bucketName);
        } catch (Exception e) {
            log.warn("创建 Bucket 失败: {}, 错误: {}", bucketName, e.getMessage());
        }

        // 检查并创建 Collection
        VectorSpaceId spaceId = VectorSpaceId.builder().logicalName(collectionName).build();
        if (!vectorStoreService.vectorSpaceExists(spaceId)) {
            VectorSpaceSpec spaceSpec = VectorSpaceSpec.builder()
                    .spaceId(spaceId)
                    .remark(description)
                    .build();
            vectorStoreService.ensureVectorSpace(spaceSpec);
            log.info("成功创建 Collection: {}", collectionName);
        } else {
            log.info("Collection 已存在: {}", collectionName);
        }
    }
}
