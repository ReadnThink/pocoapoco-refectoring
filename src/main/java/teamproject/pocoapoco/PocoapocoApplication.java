package teamproject.pocoapoco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.HiddenHttpMethodFilter;

@SpringBootApplication
@EnableRedisHttpSession
public class PocoapocoApplication {
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter(){
        // put / delete HTTP 메서드 호출 용도
        // (적용 후 성공)
        return new HiddenHttpMethodFilter();
    }

    public static void main(String[] args) {
        SpringApplication.run(PocoapocoApplication.class, args);
    }

}
