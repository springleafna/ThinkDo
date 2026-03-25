package com.springleaf.thinkdo.service.impl;

import com.springleaf.thinkdo.domain.dto.StoredFileDTO;
import com.springleaf.thinkdo.service.FileStorageService;
import com.springleaf.thinkdo.util.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;

    private static final Tika TIKA = new Tika();

    /**
     * 上传文件到指定的S3存储桶根目录
     *
     * @param bucketName 目标S3存储桶名称，不能为空
     * @param file       待上传的文件
     * @return 文件元数据信息
     */
    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, MultipartFile file) {
        return upload(bucketName, file, null);
    }

    /**
     * 上传字节数组到指定的S3存储桶根目录
     *
     * @param bucketName      目标S3存储桶名称
     * @param content         文件内容
     * @param originalFilename 原始文件名
     * @param contentType     文件MIME类型
     * @return 文件元数据信息
     */
    @Override
    public StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType) {
        return upload(bucketName, content, originalFilename, contentType, null);
    }

    /**
     * 上传文件到指定的S3存储桶，使用自定义路径前缀（多租户隔离）
     *
     * @param bucketName 目标S3存储桶名称
     * @param file       待上传的文件
     * @param pathPrefix 路径前缀，例如 "123456/kb_789/"
     * @return 文件元数据信息
     */
    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, MultipartFile file, String pathPrefix) {
        // 校验bucketName和文件的有效性
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalArgumentException("bucketName 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 获取文件原始名称和大小
        String originalFilename = file.getOriginalFilename();
        long size = file.getSize();

        // 使用Tika库检测文件的MIME类型
        String detected;
        try (InputStream is = file.getInputStream()) {
            detected = TIKA.detect(is, originalFilename);
        }

        // 重新获取输入流并调用内部上传方法
        try (InputStream uploadIs = file.getInputStream()) {
            return uploadInternal(bucketName, uploadIs, size, originalFilename, detected, pathPrefix);
        }
    }

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
    @Override
    public StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType, String pathPrefix) {
        // 校验bucketName和content的有效性
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalArgumentException("bucketName 不能为空");
        }
        if (content == null) {
            throw new IllegalArgumentException("上传内容不能为空");
        }

        // 若未提供有效的MIME类型，则使用Tika库自动检测
        String detected = contentType;
        if (detected == null || detected.isBlank()) {
            detected = TIKA.detect(content, originalFilename);
        }

        // 调用内部上传方法完成文件上传
        return uploadInternal(bucketName, new ByteArrayInputStream(content), content.length, originalFilename, detected, pathPrefix);
    }

    @Override
    public InputStream openStream(String url) {
        S3Location loc = parseS3Url(url);
        return s3Client.getObject(b -> b.bucket(loc.bucket()).key(loc.key()));
    }

    @Override
    @SneakyThrows
    public void deleteByUrl(String url) {
        FileSystemUtils.deleteRecursively(Path.of(url));
    }

    private String toS3Url(String bucket, String key) {
        return "s3://" + bucket + "/" + key;
    }

    private S3Location parseS3Url(String url) {
        try {
            URI uri = URI.create(url);
            if (!"s3".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Unsupported url scheme: " + url);
            }

            String bucket = uri.getHost();
            String path = uri.getPath(); // /key...
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("Invalid s3 url(bucket missing): " + url);
            }

            String key = (path != null && path.startsWith("/")) ? path.substring(1) : path;
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Invalid s3 url(key missing): " + url);
            }

            return new S3Location(bucket, key);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid s3 url: " + url, e);
        }
    }

    private record S3Location(String bucket, String key) {
    }

    /**
     * 上传文件到指定的S3存储桶（根目录），并返回文件的元数据信息
     */
    private StoredFileDTO uploadInternal(String bucketName,
                                         InputStream inputStream,
                                         long size,
                                         String originalFilename,
                                         String detectedContentType) {
        return uploadInternal(bucketName, inputStream, size, originalFilename, detectedContentType, null);
    }

    /**
     * 上传文件到指定的S3存储桶，支持路径前缀（多租户隔离），并返回文件的元数据信息
     *
     * @param bucketName          目标S3存储桶名称，不能为空
     * @param inputStream         文件输入流，包含待上传的文件内容
     * @param size                文件大小（字节）
     * @param originalFilename    原始文件名，可能为null
     * @param detectedContentType 检测到的文件MIME类型
     * @param pathPrefix          路径前缀，例如 "123456/kb_789/"，可为null
     * @return StoredFileDTO      包含文件URL、类型、大小和原始文件名的元数据对象
     */
    private StoredFileDTO uploadInternal(String bucketName,
                                         InputStream inputStream,
                                         long size,
                                         String originalFilename,
                                         String detectedContentType,
                                         String pathPrefix) {
        // 处理原始文件名，若为null则替换为空字符串
        String safeName = originalFilename == null ? "" : originalFilename;

        // 提取文件后缀名
        String suffix = extractSuffix(safeName);

        // 生成唯一的S3对象键（key）
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + (suffix.isBlank() ? "" : "." + suffix);

        // 构建完整的S3对象键：路径前缀 + 文件名
        String s3Key;
        if (StringUtils.hasText(pathPrefix)) {
            // 规范化路径前缀：确保不以 / 开头，以 / 结尾
            String normalizedPrefix = pathPrefix.startsWith("/")
                ? pathPrefix.substring(1)
                : pathPrefix;
            if (!normalizedPrefix.endsWith("/")) {
                normalizedPrefix += "/";
            }
            s3Key = normalizedPrefix + fileName;
        } else {
            s3Key = fileName;
        }

        // 将文件上传到S3存储桶
        s3Client.putObject(
                b -> b.bucket(bucketName)
                        .key(s3Key)
                        .contentType(detectedContentType)
                        .build(),
                RequestBody.fromInputStream(inputStream, size)
        );

        // 构造文件的S3访问URL
        String url = toS3Url(bucketName, s3Key);

        // 检测文件的实际类型
        String detectedType = FileTypeDetector.detectType(originalFilename, detectedContentType);

        // 构建并返回文件元数据对象
        return StoredFileDTO.builder()
                .url(url)
                .detectedType(detectedType)
                .size(size)
                .originalFilename(originalFilename)
                .build();
    }


    /**
     * 获取文件名后缀
     */
    private String extractSuffix(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).trim();
    }

}
