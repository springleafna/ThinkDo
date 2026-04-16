package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.domain.dto.StoredFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {

    StoredFileDTO upload(String bucketName, MultipartFile file);

    StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType);

    StoredFileDTO upload(String bucketName, MultipartFile file, String pathPrefix);

    StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType, String pathPrefix);

    InputStream openStream(String url);

    void deleteByUrl(String url);
}
