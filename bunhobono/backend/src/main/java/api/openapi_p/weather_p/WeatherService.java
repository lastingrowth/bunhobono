package api.openapi_p.weather_p;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WeatherService {

    // application.yaml에 설정한 기상청 API 기본 주소
    @Value("${weather.api.base-url}")
    private String baseUrl;

    // application.yaml에 설정한 인코딩 인증키
    @Value("${weather.api.service-key}")
    private String serviceKey;

    // 부산 지역 기상청 X 격자 좌표
    @Value("${weather.location.nx}")
    private int nx;

    // 부산 지역 기상청 Y 격자 좌표
    @Value("${weather.location.ny}")
    private int ny;

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.create();
    }

    // 기상청 초단기실황 API에서 현재 날씨 JSON을 가져온다.
    public String today() {
        try {
            /*
             * 기상청 자료가 아직 생성되지 않은 시간을 요청하는 것을
             * 방지하기 위해 현재 시각보다 40분 전의 정시를 사용한다.
             */
            ZonedDateTime baseDateTime = ZonedDateTime
                    .now(ZoneId.of("Asia/Seoul"))
                    .minusMinutes(40)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            // 예: 20260806
            String baseDate = baseDateTime.format(
                    DateTimeFormatter.ofPattern("yyyyMMdd")
            );

            // 예: 1400
            String baseTime = baseDateTime.format(
                    DateTimeFormatter.ofPattern("HHmm")
            );

            /*
             * application.yaml에 저장한 인코딩 인증키를 그대로 붙인다.
             * serviceKey를 추가로 인코딩하거나 디코딩하지 않는다.
             */
            String requestUrl =
                    baseUrl + "/getUltraSrtNcst"
                            + "?serviceKey=" + serviceKey
                            + "&pageNo=1"
                            + "&numOfRows=1000"
                            + "&dataType=JSON"
                            + "&base_date=" + baseDate
                            + "&base_time=" + baseTime
                            + "&nx=" + nx
                            + "&ny=" + ny;

            // 기상청 API에 GET 요청을 보내고 JSON 문자열을 받는다.
            String responseBody = restClient
                    .get()
                    .uri(URI.create(requestUrl))
                    .retrieve()
                    .body(String.class);

            // 기상청에서 받은 JSON을 가공하지 않고 그대로 반환한다.
            return responseBody;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "기상청 날씨 정보를 불러오지 못했습니다.",
                    e
            );
        }
    }
}