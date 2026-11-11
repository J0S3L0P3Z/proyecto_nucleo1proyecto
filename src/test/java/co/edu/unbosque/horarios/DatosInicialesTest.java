package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que los datos precargados se lean correctamente con Jackson.
 */
class DatosInicialesTest {
	@TempDir
	Path dir;

	/**
	 * Sin {@code estado.json}, se cargan los datos iniciales con un horario publicado.
	 */
	@Test
	void cargaDatosIniciales() {
		Estado e = new AlmacenJson(new ObjectMapper(), dir.toString()).leer();
		assertEquals(7, e.docentes().size());
		assertEquals(7, e.cursos().size());
		assertEquals(11, e.aulas().size());
		assertFalse(e.estudiantes().isEmpty());
		assertTrue(ServicioHorario.vigente(e).isPresent());
		assertEquals(EstadoInscripcion.ABIERTA, e.inscripcion());
		assertEquals(24, e.parametros().horasMaximas().get(Vinculacion.COMPLETO));
	}
}
