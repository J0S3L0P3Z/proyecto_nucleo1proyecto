package co.edu.unbosque.horarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Punto de entrada del sistema de horarios de la Universidad El Bosque.
 * <p>
 * Se puede ejecutar como aplicación independiente ({@code java -jar horarios.war}) o desplegar
 * el WAR en un servidor Tomcat 10.1 o superior.
 * </p>
 */
@SpringBootApplication
public class HorariosApplication extends SpringBootServletInitializer {
	/**
	 * Configura la aplicación cuando se despliega en un servidor de aplicaciones externo.
	 *
	 * @param builder constructor de la aplicación
	 * @return constructor con la clase principal registrada
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(HorariosApplication.class);
	}

	/**
	 * Arranca la aplicación con el servidor embebido.
	 *
	 * @param args argumentos de línea de comandos que se pasan a Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(HorariosApplication.class, args);
	}
}
