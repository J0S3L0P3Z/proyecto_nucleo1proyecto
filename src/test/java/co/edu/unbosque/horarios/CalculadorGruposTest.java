package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.CalculadorGrupos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del cálculo del número mínimo de grupos.
 */
class CalculadorGruposTest {
	private final CalculadorGrupos calculador = new CalculadorGrupos();

	/**
	 * Con cupo 40 y tolerancia del 10 %, cada grupo admite 44 estudiantes y 100 requieren 3 grupos.
	 */
	@Test
	void cienEstudiantesRequierenTresGrupos() {
		Parametros p = Parametros.iniciales();
		assertEquals(44, calculador.capacidad(p));
		assertEquals(3, calculador.calcular(new Curso("c", "X", "X", 1, 2, "", 100), p));
		assertEquals(1, calculador.calcular(new Curso("c", "X", "X", 1, 2, "", 44), p));
	}
}
