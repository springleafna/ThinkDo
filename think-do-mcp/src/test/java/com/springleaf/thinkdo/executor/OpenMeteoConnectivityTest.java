package com.springleaf.thinkdo.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Open-Meteo 免费天气 API 连通性测试（独立运行，不依赖 Spring/Lombok）
 * 验证能否通过经纬度查询中国城市的实时天气和预报数据
 */
public class OpenMeteoConnectivityTest {

    private static final String BASE_URL = "https://api.open-meteo.com";

    public static void main(String[] args) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        System.out.println("========== 测试1: 北京当前天气 ==========");
        testCurrentWeather(httpClient, "北京", 39.9, 116.4);

        System.out.println("\n========== 测试2: 上海7天预报 ==========");
        testForecast(httpClient, "上海", 31.2, 121.5, 7);

        System.out.println("\n========== 测试3: 三亚当前天气（热带城市） ==========");
        testCurrentWeather(httpClient, "三亚", 18.3, 109.5);

        System.out.println("\n========== 测试4: 哈尔滨7天预报（高纬度城市） ==========");
        testForecast(httpClient, "哈尔滨", 45.8, 126.5, 3);

        System.out.println("\n========== 所有测试完成 ==========");
    }

    static void testCurrentWeather(HttpClient httpClient, String city, double lat, double lon) throws Exception {
        String url = String.format(
                "%s/v1/forecast?latitude=%.2f&longitude=%.2f"
                        + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl"
                        + "&timezone=auto",
                BASE_URL, lat, lon);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assert200(response.statusCode());

        String body = response.body();
        System.out.println("[PASS] 状态码: " + response.statusCode());
        System.out.println("[PASS] 响应长度: " + body.length() + " bytes");

        // 简单校验关键字段
        if (!body.contains("current")) {
            throw new AssertionError("响应中缺少 current 字段");
        }
        if (!body.contains("temperature_2m")) {
            throw new AssertionError("响应中缺少 temperature_2m 字段");
        }
        if (!body.contains("weather_code")) {
            throw new AssertionError("响应中缺少 weather_code 字段");
        }

        // 提取关键数据
        System.out.println("[PASS] 响应数据:");
        // 手动解析简单 JSON
        String currentBlock = extractJsonBlock(body, "current");
        if (currentBlock != null) {
            String temp = extractValue(currentBlock, "temperature_2m");
            String humidity = extractValue(currentBlock, "relative_humidity_2m");
            String weatherCode = extractValue(currentBlock, "weather_code");
            String windSpeed = extractValue(currentBlock, "wind_speed_10m");
            String windDir = extractValue(currentBlock, "wind_direction_10m");

            int wc = Integer.parseInt(weatherCode);
            int wd = Integer.parseInt(windDir);
            double ws = Double.parseDouble(windSpeed);

            System.out.println("  温度: " + temp + "°C");
            System.out.println("  湿度: " + humidity + "%");
            System.out.println("  天气代码: " + weatherCode + " → " + weatherCodeToDescription(wc));
            System.out.println("  风速: " + windSpeed + " km/h");
            System.out.println("  风向: " + wd + "° → " + windDirectionToChinese(wd));
        }
    }

    static void testForecast(HttpClient httpClient, String city, double lat, double lon, int days) throws Exception {
        String url = String.format(
                "%s/v1/forecast?latitude=%.2f&longitude=%.2f"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_direction_10m_dominant,sunrise,sunset"
                        + "&timezone=auto&forecast_days=%d",
                BASE_URL, lat, lon, days);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assert200(response.statusCode());

        String body = response.body();
        System.out.println("[PASS] 状态码: " + response.statusCode());
        System.out.println("[PASS] 响应长度: " + body.length() + " bytes");

        if (!body.contains("daily")) {
            throw new AssertionError("响应中缺少 daily 字段");
        }

        System.out.println("[PASS] " + days + "天预报数据获取成功");
        System.out.println("  原始响应前500字符: " + body.substring(0, Math.min(500, body.length())));
    }

    private static String extractJsonBlock(String json, String key) {
        String searchKey = "\"" + key + "\":{";
        int start = json.indexOf(searchKey);
        if (start < 0) return null;
        start += key.length() + 3;
        int braceCount = 1;
        int end = start;
        while (end < json.length() && braceCount > 0) {
            char c = json.charAt(end);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            end++;
        }
        return json.substring(start - 1, end);
    }

    private static String extractValue(String block, String key) {
        String searchKey = "\"" + key + "\":";
        int start = block.indexOf(searchKey);
        if (start < 0) return null;
        start += searchKey.length();
        while (start < block.length() && block.charAt(start) == ' ') start++;
        int end = start;
        while (end < block.length() && block.charAt(end) != ',' && block.charAt(end) != '}') end++;
        return block.substring(start, end).trim();
    }

    static String weatherCodeToDescription(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1 -> "大部晴";
            case 2 -> "多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51 -> "小毛毛雨";
            case 53 -> "中毛毛雨";
            case 55 -> "大毛毛雨";
            case 56, 57 -> "冻毛毛雨";
            case 61 -> "小雨";
            case 63 -> "中雨";
            case 65 -> "大雨";
            case 66 -> "冻雨（小）";
            case 67 -> "冻雨（大）";
            case 71 -> "小雪";
            case 73 -> "中雪";
            case 75 -> "大雪";
            case 77 -> "米雪（雪粒）";
            case 80 -> "阵雨（小）";
            case 81 -> "阵雨（中）";
            case 82 -> "阵雨（大）";
            case 85 -> "阵雪（小）";
            case 86 -> "阵雪（大）";
            case 95 -> "雷阵雨";
            case 96 -> "雷阵雨伴冰雹（小）";
            case 99 -> "雷阵雨伴冰雹（大）";
            default -> "未知（代码" + code + "）";
        };
    }

    static String windDirectionToChinese(int degree) {
        String[] directions = {
                "北风", "北东北风", "东北风", "东东北风",
                "东风", "东东南风", "东南风", "南东南风",
                "南风", "南西南风", "西南风", "西西南风",
                "西风", "西西北风", "西北风", "北西北风"
        };
        int index = (int) Math.round(degree / 22.5) % 16;
        return directions[index];
    }

    private static void assert200(int statusCode) {
        if (statusCode != 200) {
            throw new AssertionError("期望状态码 200，实际: " + statusCode);
        }
    }
}
