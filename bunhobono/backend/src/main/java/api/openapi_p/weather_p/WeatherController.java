package api.openapi_p.weather_p;

import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    @Resource
    WeatherService weatherService;

    // 부산 지역 현재 날씨 조회
    @GetMapping("/today")
    public String today() {
        return weatherService.today();
    }
}