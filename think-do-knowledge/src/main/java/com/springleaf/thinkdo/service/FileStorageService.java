package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.domain.dto.StoredFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {

    /**
     * 上传文件到指定的S3存储桶根目录
     *
     * @param bucketName 目标S3存储桶名称
     * @param file       待上传的文件
     * @return 文件元数据信息
     */
    StoredFileDTO upload(String bucketName, MultipartFile file);

    /**
     * 上传字节数组到指定的S3存储桶根目录
     *
     * @param bucketName      目标S3存储桶名称
     * @param content         文件内容
     * @param originalFilename 原始文件名
     * @param contentType     文件MIME类型
     * @return 文件元数据信息
     */
    StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType);

    /**
     * 上传文件到指定的S3存储桶，使用自定义路径前缀（多租户隔离）
     *
     * @param bucketName 目标S3存储桶名称
     * @param file       待上传的文件
     * @param pathPrefix 路径前缀，例如 "123456/kb_789/"
     * @return 文件元数据信息
     */
    StoredFileDTO upload(String bucketName, MultipartFile file, String pathPrefix);

    /**
     * 上传字节数组到指定的S3存储桶，使用自定义路径前缀（多租户隔离）
     *
     * @param bucketName      目标S3存储桶名称
     * @param content         文件内容
     * @param originalFilename 原始文件名
     * @param contentType     文件MIME类型
     * @param pathPrefix      路径前缀，例如 "123456/kb_789/"
     * @return 文件元数据信息
     */
    StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType, String pathPrefix);

    InputStream openStream(String url);

    void deleteByUrl(String url);
}
