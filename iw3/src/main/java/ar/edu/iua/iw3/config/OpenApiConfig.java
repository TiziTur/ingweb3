package ar.edu.iua.iw3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;


@Configuration
@OpenAPIDefinition(
       security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
		  name = "Bearer Authentication",
		  type = SecuritySchemeType.HTTP,
		  bearerFormat = "JWT",
		  scheme = "bearer",
	      in = SecuritySchemeIn.HEADER
		)
public class OpenApiConfig {
 ///Doc: https://springdoc.org/#Introduction
   @Bean
   public OpenAPI openApi() {
       return new OpenAPI()
               .info(new Info()
                       .title("Trabajo Integrador IW3 - Órdenes de carga de gas")
                       .description("API del backend: recepción de órdenes (SAP), pesajes (TMS), carga en tiempo real y conciliación. "
                               + "Primero hacer login en POST /api/v1/login y usar el token con el botón Authorize.")
                       .version("v1.0")
                       .contact(new Contact()
                               .name("Grupo IW3")
                               .url("https://github.com/TiziTur/ingweb3"))
               );
   }
}
