package kz.cyber.acf.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioService {

    private static final String BUCKET = "acf";
    private static final int MAX_CACHE_HOURS = 6;

    private final MinioClient minioClient;

    // Cached URL TTL is capped at MAX_CACHE_HOURS so an entry is never served
    // after the presigned URL itself has expired in MinIO (see expiryHours).
    private final Cache<String, CachedUrl> presignedUrlCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfter(new Expiry<String, CachedUrl>() {
                @Override
                public long expireAfterCreate(String key, CachedUrl value, long currentTime) {
                    return TimeUnit.HOURS.toNanos(Math.min(MAX_CACHE_HOURS, value.expiryHours()));
                }

                @Override
                public long expireAfterUpdate(String key, CachedUrl value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(String key, CachedUrl value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    private record CachedUrl(String url, int expiryHours) {
    }

    public String upload(String folder, MultipartFile file) {
        try {
            String objectName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }

    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    public String presignedUrl(String objectName, int expiryHours) {
        return presignedUrl(objectName, expiryHours, Map.of());
    }

    public String presignedUrl(String objectName, int expiryHours, Map<String, String> responseHeaders) {
        String cacheKey = objectName + "|" + expiryHours + "|" + new TreeMap<>(responseHeaders);
        return presignedUrlCache.get(cacheKey, key ->
                new CachedUrl(generatePresignedUrl(objectName, expiryHours, responseHeaders), expiryHours)
        ).url();
    }

    private String generatePresignedUrl(String objectName, int expiryHours, Map<String, String> responseHeaders) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(BUCKET)
                            .object(objectName)
                            .expiry(expiryHours, TimeUnit.HOURS)
                            .extraQueryParams(responseHeaders)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
    }
}
