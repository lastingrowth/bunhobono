package api.openapi_p.weather_p;
import org.junit.jupiter.api.*; import org.springframework.test.util.ReflectionTestUtils; import static org.assertj.core.api.Assertions.*;
class WeatherServiceTest {
 @Test @DisplayName("UT-BE-WEATHER-001 잘못된 날씨 API 주소의 호출 실패를 서비스 예외로 변환한다") void wrapFailure(){ WeatherService s=new WeatherService(); ReflectionTestUtils.setField(s,"baseUrl","잘못된 주소"); ReflectionTestUtils.setField(s,"serviceKey",""); ReflectionTestUtils.setField(s,"nx",98); ReflectionTestUtils.setField(s,"ny",76); assertThatThrownBy(s::today).isInstanceOf(IllegalStateException.class).hasMessageContaining("날씨 정보를"); }
}
