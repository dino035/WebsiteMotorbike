package com.nguyen.motoparts.advisory.service;

/** Xe của khách (từ hồ sơ xe gắn với cuộc trò chuyện), dùng làm ngữ cảnh khi khách hỏi phụ tùng mà không nói rõ xe. */
public record VehicleContext(String brand, String model, int registrationYear) {
}
