package com.example.websitemotorbike.util;

import jakarta.servlet.http.Part;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

public class PhotoStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Thiếu biến môi trường " + name);
        }
        return value;
    }

    /** Ném IllegalArgumentException (thông báo tiếng Việt) nếu file không hợp lệ. */
    public static void validate(Part part) {
        if (part.getSize() == 0) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh");
        }
        if (part.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Mỗi ảnh tối đa 5MB");
        }
        if (!ALLOWED.containsKey(part.getContentType())) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP");
        }
    }

    /** Upload lên Supabase, trả về storagePath (vd: motorbikes/abc.jpg). */
    public static String upload(Part part) throws IOException {
        validate(part);
        String storagePath = "motorbikes/" + UUID.randomUUID() + ALLOWED.get(part.getContentType());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(env("SUPABASE_URL") + "/storage/v1/object/"
                        + env("SUPABASE_BUCKET") + "/" + storagePath))
                .header("apikey", env("SUPABASE_KEY"))
                .header("Authorization", "Bearer " + env("SUPABASE_KEY"))
                .header("Content-Type", part.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(part.getInputStream().readAllBytes()))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IOException("Upload thất bại (" + response.statusCode() + "): " + response.body());
        }
        return storagePath;
    }

    public static void delete(String storagePath) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(env("SUPABASE_URL") + "/storage/v1/object/"
                        + env("SUPABASE_BUCKET") + "/" + storagePath))
                .header("apikey", env("SUPABASE_KEY"))
                .header("Authorization", "Bearer " + env("SUPABASE_KEY"))
                .DELETE()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() >= 300) {
            throw new IOException("Xóa ảnh thất bại (" + response.statusCode() + "): " + response.body());
        }
    }

    public static String publicUrl(String storagePath) {
        return env("SUPABASE_URL") + "/storage/v1/object/public/"
                + env("SUPABASE_BUCKET") + "/" + storagePath;
    }

    private static HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Bị gián đoạn khi gọi Supabase", e);
        }
    }
}