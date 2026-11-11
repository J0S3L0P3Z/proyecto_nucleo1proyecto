package co.edu.unbosque.horarios.security;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.*;

/**
 * Configuración de seguridad de la API.
 * <p>
 * Usa autenticación HTTP Basic sin sesión. Los endpoints públicos son de libre acceso, los de
 * consulta e inscripción requieren usuario autenticado, los del Comité requieren el rol COMITE
 * o DIRECTOR y los del director requieren el rol DIRECTOR.
 * </p>
 */
@Configuration
public class Seguridad {
	/**
	 * Cifrador de contraseñas.
	 *
	 * @return codificador BCrypt
	 */
	@Bean
	PasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Busca usuarios en el almacenamiento JSON para autenticarlos.
	 *
	 * @param db almacenamiento del estado
	 * @return servicio de búsqueda de usuarios
	 */
	@Bean
	UserDetailsService usuarios(AlmacenJson db) {
		return username -> {
			Usuario u = db.leer().usuarios().values().stream().filter(x -> x.username().equals(username)).findFirst()
					.orElseThrow(() -> new UsernameNotFoundException("Usuario desconocido"));
			return new User(u.username(), u.passwordHash(), List.of(new SimpleGrantedAuthority("ROLE_" + u.rol())));
		};
	}

	/**
	 * Define los permisos de acceso a cada grupo de endpoints.
	 *
	 * @param http configuración HTTP de Spring Security
	 * @return cadena de filtros de seguridad
	 * @throws Exception si la configuración falla
	 */
	@Bean
	SecurityFilterChain filtro(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
						.requestMatchers("/api/director/**").hasRole("DIRECTOR")
						.requestMatchers("/api/comite/**").hasAnyRole("COMITE", "DIRECTOR")
						.requestMatchers("/api/consulta/**", "/api/inscripcion/**").authenticated()
						.anyRequest().denyAll())
				.httpBasic(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build();
	}

	/**
	 * Permite que el frontend, servido desde otro origen, consuma la API.
	 *
	 * @param origenes orígenes permitidos (propiedad {@code app.cors.origins}, separados por comas)
	 * @return configuración CORS para las rutas {@code /api/**}
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.origins:http://localhost:4200}") List<String> origenes) {
		CorsConfiguration c = new CorsConfiguration();
		c.setAllowedOrigins(origenes);
		c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
		c.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/api/**", c);
		return fuente;
	}

	/**
	 * Al iniciar, crea el director si no existe ninguno y, si se configuró una contraseña de
	 * demostración, crea los usuarios de prueba del Comité, de un docente y de un estudiante.
	 *
	 * @param db       almacenamiento del estado
	 * @param encoder  cifrador de contraseñas
	 * @param user     nombre del director (propiedad {@code app.bootstrap.director.username})
	 * @param pass     contraseña del director (propiedad {@code app.bootstrap.director.password})
	 * @param demoPass contraseña de los usuarios de demostración (propiedad {@code app.demo.password});
	 *                 si está vacía no se crean
	 * @return tarea que se ejecuta al iniciar la aplicación
	 */
	@Bean
	CommandLineRunner bootstrap(AlmacenJson db, PasswordEncoder encoder,
			@Value("${app.bootstrap.director.username:director}") String user,
			@Value("${app.bootstrap.director.password:}") String pass,
			@Value("${app.demo.password:}") String demoPass) {
		return args -> db.cambio(e -> {
			if (e.usuarios().values().stream().noneMatch(u -> u.rol() == Rol.DIRECTOR)) {
				if (pass.isBlank())
					throw new IllegalStateException(
							"Configure BOOTSTRAP_DIRECTOR_PASSWORD para iniciar por primera vez");
				agregar(e, user, encoder.encode(pass), Rol.DIRECTOR, null, null);
			}
			if (!demoPass.isBlank()) {
				String hash = encoder.encode(demoPass);
				agregar(e, "comite", hash, Rol.COMITE, null, null);
				if (e.docentes().containsKey("doc-wilson-rojas"))
					agregar(e, "wilson.rojas", hash, Rol.DOCENTE, "doc-wilson-rojas", null);
				e.estudiantes().values().stream().findFirst()
						.ifPresent(est -> agregar(e, "estudiante1", hash, Rol.ESTUDIANTE, null, est.id()));
			}
			return true;
		});
	}

	/**
	 * Agrega un usuario si no existe otro con el mismo nombre.
	 *
	 * @param e            estado dentro de una operación de cambio
	 * @param username     nombre de usuario
	 * @param hash         contraseña cifrada
	 * @param rol          rol
	 * @param docenteId    docente vinculado, o {@code null}
	 * @param estudianteId estudiante vinculado, o {@code null}
	 */
	private static void agregar(Estado e, String username, String hash, Rol rol, String docenteId,
			String estudianteId) {
		if (e.usuarios().values().stream().anyMatch(u -> u.username().equals(username)))
			return;
		String id = UUID.randomUUID().toString();
		e.usuarios().put(id, new Usuario(id, username, hash, rol, docenteId, estudianteId));
	}
}
