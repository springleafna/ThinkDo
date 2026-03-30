package com.springleaf.thinkdo.service.impl;

import com.springleaf.thinkdo.domain.dto.StoredFileDTO;
import com.springleaf.thinkdo.service.FileStorageService;
import com.springleaf.thinkdo.util.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;

    @Value("${rustfs.url}")
    private String rustfsUrl;

    private static final Tika TIKA = new Tika();

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, MultipartFile file) {
        return upload(bucketName, file, null);
    }

    @Override
    public StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType) {
        return upload(bucketName, content, originalFilename, contentType, null);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, MultipartFile file, String pathPrefix) {
        if (!StringUtils.hasText(bucketName)) throw new IllegalArgumentException("bucketName 不能为空");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空");

        String originalFilename = file.getOriginalFilename();
        long size = file.getSize();

        String detected;
        try (InputStream is = file.getInputStream()) {
            detected = TIKA.detect(is, originalFilename);
        }
        try (InputStream uploadIs = file.getInputStream()) {
            return uploadInternal(bucketName, uploadIs, size, originalFilename, detected, pathPrefix);
        }
    }

    @Override
    public StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType, String pathPrefix) {
        if (!StringUtils.hasText(bucketName)) throw new IllegalArgumentException("bucketName 不能为空");
        if (content == null) throw new IllegalArgumentException("上传内容不能为空");

        String detected = (contentType == null || contentType.isBlank())
                ? TIKA.detect(content, originalFilename)
                : contentType;

        try (InputStream is = new ByteArrayInputStream(content)) {
            return uploadInternal(bucketName, is, content.length, originalFilename, detected, pathPrefix);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public InputStream openStream(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath();
        // path: /bucketName/key...
        String[] parts = path.substring(1).split("/", 2);
        String bucket = parts[0];
        String key = parts.length > 1 ? parts[1] : "";
        return s3Client.getObject(b -> b.bucket(bucket).key(key));
    }

    @Override
    public void deleteByUrl(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath();
        String[] parts = path.substring(1).split("/", 2);
        String bucket = parts[0];
        String key = parts.length > 1 ? parts[1] : "";
        s3Client.deleteObject(b -> b.bucket(bucket).key(key));
    }

    private StoredFileDTO uploadInternal(String bucketName, InputStream inputStream, long size,
                                         String originalFilename, String detectedContentType, String pathPrefix) {
        String suffix = extractSuffix(originalFilename);
        String fileName = UUID.randomUUID() + (suffix.isBlank() ? "" : "." + suffix);

        String s3Key;
        if (StringUtils.hasText(pathPrefix)) {
            String normalizedPrefix = pathPrefix.trim();
            if (!normalizedPrefix.endsWith("/")) normalizedPrefix += "/";
            s3Key = normalizedPrefix + fileName;
        } else {
            s3Key = fileName;
        }

        s3Client.putObject(
                b -> b.bucket(bucketName).key(s3Key).contentType(detectedContentType).build(),
                RequestBody.fromInputStream(inputStream, size)
        );

        String url = toS3Url(bucketName, s3Key);
        String detectedType = FileTypeDetector.detectType(originalFilename, detectedContentType);

        return StoredFileDTO.builder()
                .url(url)
                .detectedType(detectedType)
                .size(size)
                .originalFilename(originalFilename)
                .build();
    }

    private String toS3Url(String bucketName, String s3Key) {
        String base = rustfsUrl.endsWith("/") ? rustfsUrl : rustfsUrl + "/";
        return base + bucketName + "/" + s3Key;
    }

    private String extractSuffix(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).trim();
    }
}

