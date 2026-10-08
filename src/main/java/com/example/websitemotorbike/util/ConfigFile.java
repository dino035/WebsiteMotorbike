package com.example.websitemotorbike.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Đọc tệp cấu hình .properties trong thư mục resources (tệp chứa bí mật nằm ngoài Git, mỗi máy một tệp). */
public class ConfigFile {

    /** Trả Properties rỗng nếu tệp không tồn tại hoặc không đọc được. */
    public static Properties load(String fileName) {
        Properties props = new Properties();
        try (InputStream in = ConfigFile.class.getClassLoader().getResourceAsStream(fileName)) {
            if (in != null) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                // Notepad và PowerShell hay thêm ký tự BOM ở đầu tệp UTF-8, làm hỏng khóa đầu tiên nếu không bỏ đi
                if (text.startsWith("﻿")) {
                    text = text.substring(1);
                }
                props.load(new StringReader(text));
            }
        } catch (IOException e) {
            System.out.println("Không đọc được " + fileName + ": " + e);
        }
        return props;
    }

    public static String get(Properties props, String key) {
        String value = props.getProperty(key);
        return value == null ? "" : value.trim();
    }
}
