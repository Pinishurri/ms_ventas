package com.perfulandia.ms_ventas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ActiveProfiles("test")
class MsVentasApplicationTests {

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void mainEjecutaAplicacion() {
        MsVentasApplication.main(new String[] {});
    }
}