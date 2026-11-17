package com.nguyen.motoparts.advisory.llm;

/** Dữ kiện về một sản phẩm thật đưa cho LLM viết lời tư vấn (không có mã sản phẩm, không có dữ liệu khách). */
public record ProductFact(String name, String type, long price, String description) {
}
