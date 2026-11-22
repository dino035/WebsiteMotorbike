package com.nguyen.motoparts.common.storage;

import com.nguyen.motoparts.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

/** Lưu và xóa ảnh sản phẩm trên Supabase Storage. Cơ sở dữ liệu chỉ giữ đường dẫn, không giữ nội dung ảnh. */
@Service
public class PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorageService.class);

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    public static final int MAX_PHOTOS = 10;
    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final HttpClient client = HttpClient.newHttpClient();
    private final String url;
    private final String bucket;
    private final String key;

    public PhotoStorageService(@Value("${app.supabase.url:}") String url,
                               @Value("${app.supabase.bucket:photos}") String bucket,
                               @Value("${app.supabase.key:}") String key) {
        this.url = url;
        this.bucket = bucket;
        this.key = key;
    }

    private boolean isConfigured() {
        return !url.isBlank() && !key.isBlank();
    }

    /** Tải ảnh lên, trả về storagePath (ví dụ "motorbikes/abc-123.jpg"). */
    public String upload(MultipartFile file, String folder) {
        if (!isConfigured()) {
            throw new BadRequestException("Máy chủ chưa cấu hình nơi lưu ảnh.");
        }
        validate(file);

        String storagePath = folder + "/" + UUID.randomUUID() + ALLOWED.get(file.getContentType());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/storage/v1/object/" + bucket + "/" + storagePath))
                    .header("apikey", key)
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", file.getContentType())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = send(request);
            if (response.statusCode() != 200) {
                log.warn("Supabase tra ve {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("Tải ảnh lên thất bại, vui lòng thử lại.");
            }
            return storagePath;
        } catch (IOException e) {
            throw new BadRequestException("Không đọc được tệp ảnh.");
        }
    }

    /** Xóa file trên Supabase. Thất bại chỉ ghi log, không làm hỏng thao tác chính. */
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || !isConfigured()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/storage/v1/object/" + bucket + "/" + storagePath))
                    .header("apikey", key)
                    .header("Authorization", "Bearer " + key)
                    .DELETE()
                    .build();
            HttpResponse<String> response = send(request);
            if (response.statusCode() >= 300) {
                log.warn("Xoa anh that bai {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException e) {
            log.warn("Khong goi duoc Supabase de xoa anh {}", storagePath, e);
        }
    }

    /**
     * Xóa file sau khi giao dịch commit xong.
     * Cơ sở dữ liệu là nguồn sự thật: commit hỏng thì dòng ảnh quay lại, nên file phải còn.
     * Gọi ngoài giao dịch thì xóa luôn.
     */
    public void deleteAfterCommit(String storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(storagePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete(storagePath);
            }
        });
    }

    /** Đường dẫn công khai để trình duyệt hiển thị ảnh. */
    public String publicUrl(String storagePath) {
        return url + "/storage/v1/object/public/" + bucket + "/" + storagePath;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ảnh.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("Mỗi ảnh tối đa 5MB.");
        }
        if (!ALLOWED.containsKey(file.getContentType())) {
            throw new BadRequestException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.");
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Bị gián đoạn khi gọi Supabase", e);
        }
    }
}