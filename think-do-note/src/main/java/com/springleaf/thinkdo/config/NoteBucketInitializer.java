package com.springleaf.thinkdo.config;

import com.springleaf.thinkdo.constant.NoteConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteBucketInitializer implements ApplicationRunner {

    private final S3Client s3Client;

    @Override
    public void run(ApplicationArguments args) {
        String bucket = NoteConstant.NOTE_IMAGE_BUCKET;
        try {
            s3Client.createBucket(b -> b.bucket(bucket));
            log.info("成功创建笔记图片 Bucket: {}", bucket);
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("笔记图片 Bucket 已存在（由本账户拥有）: {}", bucket);
        } catch (BucketAlreadyExistsException e) {
            log.info("笔记图片 Bucket 已存在（由其他账户拥有）: {}", bucket);
        } catch (Exception e) {
            log.warn("创建笔记图片 Bucket 失败: {}, 错误: {}", bucket, e.getMessage());
        }
    }
}
