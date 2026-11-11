package co.edu.unbosque.horarios;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto de Spring, incluida la configuración de seguridad, arranque.
 */
@SpringBootTest(properties = { "app.bootstrap.director.password=ClaveDePrueba2026",
		"app.data-dir=target/datos-prueba", "app.demo.password=DemoDePrueba2026" })
class AplicacionTest {
	/**
	 * El contexto carga sin errores.
	 */
	@Test
	void contextoCarga() {
	}
}
