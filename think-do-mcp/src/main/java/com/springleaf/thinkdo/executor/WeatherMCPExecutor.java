package com.springleaf.thinkdo.executor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.springleaf.thinkdo.core.MCPToolDefinition;
import com.springleaf.thinkdo.core.MCPToolExecutor;
import com.springleaf.thinkdo.core.MCPToolRequest;
import com.springleaf.thinkdo.core.MCPToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 天气查询 MCP 工具
 * <p>
 * 通过 Open-Meteo 地理编码 API 自动解析城市名称为经纬度，
 * 再调用天气 API 获取真实天气数据，支持全球任意城市
 */
@Slf4j
@Component
public class WeatherMCPExecutor implements MCPToolExecutor {

    private static final String TOOL_ID = "weather_query";

    @Value("${open-meteo.url:https://api.open-meteo.com}")
    private String openMeteoUrl;

    @Value("${open-meteo.timeout:15}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Gson gson = new Gson();

    @Override
    public MCPToolDefinition getToolDefinition() {
        Map<String, MCPToolDefinition.ParameterDef> parameters = new LinkedHashMap<>();

        parameters.put("city", MCPToolDefinition.ParameterDef.builder()
                .description("城市名称，如北京、上海、广州等，支持全球任意城市")
                .type("string")
                .required(true)
                .build());

        parameters.put("queryType", MCPToolDefinition.ParameterDef.builder()
                .description("查询类型：current(当前天气)、forecast(未来预报)")
                .type("string")
                .required(false)
                .defaultValue("current")
                .enumValues(List.of("current", "forecast"))
                .build());

        parameters.put("days", MCPToolDefinition.ParameterDef.builder()
                .description("预报天数，仅forecast模式有效，默认3天，最多7天")
                .type("integer")
                .required(false)
                .defaultValue(3)
                .build());

        return MCPToolDefinition.builder()
                .toolId(TOOL_ID)
                .description("查询城市天气信息，支持查看当前实时天气和未来多天天气预报，包含温度、湿度、风力、天气状况等信息。支持全球任意城市。")
                .parameters(parameters)
                .requireUserId(false)
                .build();
    }

    @Override
    public MCPToolResponse execute(MCPToolRequest request) {
        try {
            String city = request.getStringParameter("city");
            String queryType = request.getStringParameter("queryType");
            Integer days = request.getParameter("days");

            if (city == null || city.isBlank()) {
                return MCPToolResponse.error(TOOL_ID, "INVALID_PARAMS", "请提供城市名称");
            }
            if (queryType == null || queryType.isBlank()) queryType = "current";
            if (days == null || days <= 0) days = 3;
            if (days > 7) days = 7;

            // 通过地理编码 API 解析城市名 → 经纬度
            double[] coords = geocode(city);
            double lat = coords[0];
            double lon = coords[1];

            String result = switch (queryType) {
                case "forecast" -> buildForecastResult(city, lat, lon, days);
                default -> buildCurrentResult(city, lat, lon);
            };

            return MCPToolResponse.success(TOOL_ID, result);
        } catch (Exception e) {
            log.error("天气数据查询失败", e);
            return MCPToolResponse.error(TOOL_ID, "EXECUTION_ERROR", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 地理编码 API 基础 URL（与天气 API 域名不同）
     */
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com";

    /**
     * 调用 Open-Meteo 地理编码 API，将城市名解析为经纬度
     * <p>
     * 请求多个候选结果，按行政级别和人口排序，选取最匹配的主要城市
     *
     * @return [latitude, longitude]
     */
    private double[] geocode(String cityName) {
        String encodedName = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        String url = String.format("%s/v1/search?name=%s&count=10&language=zh", GEOCODING_URL, encodedName);

        log.info("地理编码: city={}", cityName);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("地理编码 API 返回错误: " + response.statusCode());
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            JsonArray results = json.getAsJsonArray("results");

            if (results == null || results.isEmpty()) {
                throw new RuntimeException("未找到城市「" + cityName + "」，请检查城市名称是否正确");
            }

            // 从候选结果中选择最佳匹配：优先行政级别高、人口多的
            JsonObject best = null;
            int bestScore = -1;

            for (int i = 0; i < results.size(); i++) {
                JsonObject candidate = results.get(i).getAsJsonObject();
                int score = geocodeScore(candidate);
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            double lat = best.get("latitude").getAsDouble();
            double lon = best.get("longitude").getAsDouble();
            String resolvedName = best.has("name") ? best.get("name").getAsString() : cityName;

            log.info("地理编码结果: {} → lat={}, lon={}, resolvedName={}, score={}", cityName, lat, lon, resolvedName, bestScore);

            return new double[]{lat, lon};
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("地理编码查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为地理编码候选结果计算匹配得分
     * <p>
     * 优先选择行政级别高的（首都 > 省会 > 地级市 > 普通村镇），其次选人口多的
     */
    private int geocodeScore(JsonObject candidate) {
        int score = 0;

        // 行政级别加分
        String featureCode = candidate.has("feature_code") ? candidate.get("feature_code").getAsString() : "";
        score += switch (featureCode) {
            case "PPLC" -> 10000; // 首都
            case "PPLA" -> 5000;  // 一级行政中心（省会）
            case "PPLA2" -> 3000; // 二级行政中心（地级市）
            case "PPLA3" -> 2000; // 三级行政中心
            case "PPL" -> 1000;   // 普通居民点
            default -> 500;
        };

        // 人口加分（有 population 字段说明是较大城市）
        if (candidate.has("population") && !candidate.get("population").isJsonNull()) {
            long population = candidate.get("population").getAsLong();
            // 每10万人加1分，上限500
            score += Math.min(500, (int) (population / 100_000));
        }

        return score;
    }

    /**
     * 查询当前天气，调用 Open-Meteo current weather API
     */
    private String buildCurrentResult(String city, double lat, double lon) {
        String url = String.format(
                "%s/v1/forecast?latitude=%.2f&longitude=%.2f"
                        + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl"
                        + "&timezone=auto",
                openMeteoUrl, lat, lon);

        log.info("查询当前天气: city={}, lat={}, lon={}", city, lat, lon);

        JsonObject json = fetchJson(url);
        JsonObject current = json.getAsJsonObject("current");

        double temperature = current.get("temperature_2m").getAsDouble();
        int humidity = current.get("relative_humidity_2m").getAsInt();
        int weatherCode = current.get("weather_code").getAsInt();
        double windSpeed = current.get("wind_speed_10m").getAsDouble();
        int windDirection = current.get("wind_direction_10m").getAsInt();
        double windGusts = current.get("wind_gusts_10m").getAsDouble();
        double pressure = current.get("pressure_msl").getAsDouble();

        String weatherDesc = weatherCodeToDescription(weatherCode);
        String windDirChinese = windDirectionToChinese(windDirection);
        String windLevel = beaufortScale(windSpeed);

        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【%s 今日天气】\n\n", city));
        sb.append(String.format("日期: %s\n", today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))));
        sb.append(String.format("天气: %s\n", weatherDesc));
        sb.append(String.format("当前温度: %.1f°C\n", temperature));
        sb.append(String.format("相对湿度: %d%%\n", humidity));
        sb.append(String.format("风向: %s\n", windDirChinese));
        sb.append(String.format("风速: %.1f km/h（%s）\n", windSpeed, windLevel));
        sb.append(String.format("阵风: %.1f km/h\n", windGusts));
        sb.append(String.format("气压: %.1f hPa\n", pressure));

        if (weatherDesc.contains("雨") || weatherDesc.contains("雪")) {
            sb.append("\n提示: 今日有降水，出行请携带雨具。");
        } else if (temperature >= 35) {
            sb.append("\n提示: 今日高温，注意防暑降温。");
        } else if (temperature <= 0) {
            sb.append("\n提示: 今日气温较低，注意防寒保暖。");
        }

        return sb.toString().trim();
    }

    /**
     * 查询未来多天天气预报，调用 Open-Meteo daily forecast API
     */
    private String buildForecastResult(String city, double lat, double lon, int days) {
        String url = String.format(
                "%s/v1/forecast?latitude=%.2f&longitude=%.2f"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_direction_10m_dominant,sunrise,sunset"
                        + "&timezone=auto&forecast_days=%d",
                openMeteoUrl, lat, lon, days);

        log.info("查询天气预报: city={}, days={}", city, days);

        JsonObject json = fetchJson(url);
        JsonObject daily = json.getAsJsonObject("daily");

        JsonArray dateArr = daily.getAsJsonArray("time");
        JsonArray weatherCodeArr = daily.getAsJsonArray("weather_code");
        JsonArray tempMaxArr = daily.getAsJsonArray("temperature_2m_max");
        JsonArray tempMinArr = daily.getAsJsonArray("temperature_2m_min");
        JsonArray precipSumArr = daily.getAsJsonArray("precipitation_sum");
        JsonArray precipProbArr = daily.getAsJsonArray("precipitation_probability_max");
        JsonArray windSpeedMaxArr = daily.getAsJsonArray("wind_speed_10m_max");
        JsonArray windDirArr = daily.getAsJsonArray("wind_direction_10m_dominant");
        JsonArray sunriseArr = daily.getAsJsonArray("sunrise");
        JsonArray sunsetArr = daily.getAsJsonArray("sunset");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【%s 未来%d天天气预报】\n\n", city, days));

        for (int i = 0; i < dateArr.size(); i++) {
            LocalDate date = LocalDate.parse(dateArr.get(i).getAsString());
            String dayLabel = i == 0 ? "今天" : i == 1 ? "明天" : i == 2 ? "后天"
                    : date.format(DateTimeFormatter.ofPattern("MM月dd日"));

            String weatherDesc = weatherCodeToDescription(weatherCodeArr.get(i).getAsInt());
            double tempMax = tempMaxArr.get(i).getAsDouble();
            double tempMin = tempMinArr.get(i).getAsDouble();
            double precip = precipSumArr.get(i).getAsDouble();
            String precipProb = precipProbArr.get(i).isJsonNull() ? "-" : precipProbArr.get(i).getAsInt() + "%";
            double windSpeedMax = windSpeedMaxArr.get(i).getAsDouble();
            String windDir = windDirectionToChinese(windDirArr.get(i).getAsInt());
            String sunrise = sunriseArr.get(i).getAsString().substring(11, 16);
            String sunset = sunsetArr.get(i).getAsString().substring(11, 16);

            sb.append(String.format("%s（%s）\n", dayLabel, date.format(DateTimeFormatter.ofPattern("MM-dd"))));
            sb.append(String.format("   天气: %s | 温度: %.1f°C ~ %.1f°C\n", weatherDesc, tempMin, tempMax));
            sb.append(String.format("   降水: %.1fmm（概率%s）| %s %.1fkm/h\n", precip, precipProb, windDir, windSpeedMax));
            sb.append(String.format("   日出: %s | 日落: %s\n\n", sunrise, sunset));
        }

        return sb.toString().trim();
    }

    /**
     * 通用 HTTP GET 请求并解析 JSON
     */
    private JsonObject fetchJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("API 返回错误状态码: " + response.statusCode() + ", body: " + response.body());
            }

            return gson.fromJson(response.body(), JsonObject.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * WMO 天气代码 → 中文天气描述
     */
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

    /**
     * 风向角度 → 中文风向名称（16方位）
     */
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

    /**
     * 风速 km/h → 蒲福风级描述
     */
    static String beaufortScale(double windSpeedKmh) {
        if (windSpeedKmh < 1) return "0级（无风）";
        if (windSpeedKmh < 6) return "1级（软风）";
        if (windSpeedKmh < 12) return "2级（轻风）";
        if (windSpeedKmh < 20) return "3级（微风）";
        if (windSpeedKmh < 29) return "4级（和风）";
        if (windSpeedKmh < 39) return "5级（清风）";
        if (windSpeedKmh < 50) return "6级（强风）";
        if (windSpeedKmh < 62) return "7级（疾风）";
        if (windSpeedKmh < 75) return "8级（大风）";
        if (windSpeedKmh < 89) return "9级（烈风）";
        if (windSpeedKmh < 103) return "10级（狂风）";
        if (windSpeedKmh < 118) return "11级（暴风）";
        return "12级（飓风）";
    }
}
