package com.example.drone.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    public static final String CLIENT_BEARER_AUTH = "clientBearerAuth";
    public static final String INTERNAL_API_KEY = "internalApiKey";

    @Bean
    public OpenAPI droneDeliveryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Drone Delivery API")
                        .version("1.0.0")
                        .description("""
                                API operacional para cadastro de drones, pedidos, planejamento de viagens, simulação, área do cliente e endpoints internos de apoio.

                                Unidades adotadas no contrato: peso e capacidade em quilogramas (kg); coordenadas, distâncias, alcance e raio em quilômetros (km); velocidade em quilômetros por hora (km/h); bateria e reserva em percentual (%); consumo de bateria em percentual por quilômetro (%/km); recarga em percentual por minuto (%/min); duração e estimativas em minutos (min).
                                """)
                        .contact(new Contact().name("Henrique Assuncao")))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Ambiente local")))
                .components(new Components()
                        .addSecuritySchemes(CLIENT_BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Signed token")
                                .description("Token retornado por POST /api/auth/register ou POST /api/auth/login."))
                        .addSecuritySchemes(INTERNAL_API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Internal-Api-Key")
                                .description("Chave configurada em drone.internal.api-key para endpoints /internal.")));
    }

    @Bean
    public GroupedOpenApi completeBackendApi() {
        return GroupedOpenApi.builder()
                .group("backend-completo")
                .pathsToMatch("/api/**", "/internal/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("api-publica")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("api-interna")
                .pathsToMatch("/internal/**")
                .build();
    }
}
