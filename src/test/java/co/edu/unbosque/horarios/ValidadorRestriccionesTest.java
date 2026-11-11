package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.ValidadorRestricciones;
import co.edu.unbosque.horarios.service.reglas.Resultado;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las reglas de asignación R1 a R12.
 */
class ValidadorRestriccionesTest {
	private final ValidadorRestricciones validador = new ValidadorRestricciones();

	/**
	 * Estado con un curso, dos grupos, dos aulas y un docente habilitado.
	 *
	 * @return estado de prueba
	 */
	private Estado estado() {
		Estado e = Estado.vacio();
		e.cursos().put("c", new Curso("c", "MAT", "Mat", 1, 2, "PC", 30));
		e.grupos().put("g", new Grupo("g", "c", 1, 44, 0, EstadoGrupo.ABIERTO));
		e.grupos().put("g2", new Grupo("g2", "c", 2, 44, 0, EstadoGrupo.ABIERTO));
		e.aulas().put("a", new Aula("a", "A1", 44, Set.of("PC")));
		e.aulas().put("b", new Aula("b", "B1", 44, Set.of("PC")));
		e.docentes().put("d", new Docente("d", "Ana", Vinculacion.COMPLETO, Restriccion.NINGUNA, Set.of("c")));
		Parametros p = e.parametros();
		return e.conParametros(new Parametros(p.inicioSemana(), p.finSemana(), p.inicioSabado(), p.finSabado(),
				p.descansoInicio(), p.descansoFin(), p.duracion(), p.frecuenciaMax(), p.cupoMax(), p.tolerancia(),
				p.minimoInscritos(), Map.of(Vinculacion.COMPLETO, 40), p.finDiurno(), p.huecoLargo()));
	}

	/**
	 * Evalúa la última sesión de la lista dentro de un horario con todas las sesiones.
	 *
	 * @param e        estado
	 * @param sesiones sesiones del horario
	 * @return resultado de la validación
	 */
	private Resultado evaluar(Estado e, Sesion... sesiones) {
		Horario h = new Horario("h", "2026-2", 1, EstadoHorario.BORRADOR, List.of(sesiones));
		return validador.validar(e, h, sesiones[sesiones.length - 1]);
	}

	/**
	 * Indica si el resultado contiene un error de la regla.
	 *
	 * @param r     resultado
	 * @param regla código de la regla
	 * @return {@code true} si la regla aparece entre los errores
	 */
	private static boolean error(Resultado r, String regla) {
		return r.errores().stream().anyMatch(x -> x.regla().equals(regla));
	}

	/**
	 * Hay doce reglas registradas.
	 */
	@Test
	void doceReglas() {
		assertEquals(12, validador.reglas().size());
	}

	/**
	 * Una sesión válida no genera errores.
	 */
	@Test
	void sesionValida() {
		assertTrue(evaluar(estado(), new Sesion("s", "g", 1, 1, 7, "d", "a")).valido());
	}

	/**
	 * R1: un día fuera de rango se rechaza.
	 */
	@Test
	void diaInvalidoBloquea() {
		assertTrue(error(evaluar(estado(), new Sesion("s", "g", 1, 7, 7, "d", "a")), "R1"));
	}

	/**
	 * R2: la franja sin clases bloquea.
	 */
	@Test
	void descansoBloquea() {
		Resultado r = evaluar(estado(), new Sesion("s", "g", 1, 1, 11, "d", "a"));
		assertFalse(r.valido());
		assertTrue(error(r, "R2"));
	}

	/**
	 * R3: el docente no puede estar en dos sesiones a la vez.
	 */
	@Test
	void docenteCruzadoBloquea() {
		Resultado r = evaluar(estado(), new Sesion("s1", "g", 1, 1, 7, "d", "a"), new Sesion("s2", "g2", 1, 1, 8, "d", "b"));
		assertTrue(error(r, "R3"));
	}

	/**
	 * R4: el aula no puede estar en dos sesiones a la vez.
	 */
	@Test
	void aulaCruzadaBloquea() {
		Estado e = estado();
		e.docentes().put("d2", new Docente("d2", "Beto", Vinculacion.COMPLETO, Restriccion.NINGUNA, Set.of("c")));
		Resultado r = evaluar(e, new Sesion("s1", "g", 1, 1, 7, "d", "a"), new Sesion("s2", "g2", 1, 1, 7, "d2", "a"));
		assertTrue(error(r, "R4"));
	}

	/**
	 * R5: un aula pequeña no alcanza para el cupo.
	 */
	@Test
	void aulaInsuficienteBloquea() {
		Estado e = estado();
		e.aulas().put("a", new Aula("a", "A1", 20, Set.of("PC")));
		assertTrue(error(evaluar(e, new Sesion("s", "g", 1, 1, 7, "d", "a")), "R5"));
	}

	/**
	 * R7: un docente no habilitado se rechaza.
	 */
	@Test
	void docenteNoHabilitadoBloquea() {
		Estado e = estado();
		e.docentes().put("d", new Docente("d", "Ana", Vinculacion.COMPLETO, Restriccion.NINGUNA, Set.of()));
		assertTrue(error(evaluar(e, new Sesion("s", "g", 1, 1, 7, "d", "a")), "R7"));
	}

	/**
	 * R8: un docente que solo dicta desde las 18:00 no puede ir a las 7:00.
	 */
	@Test
	void restriccionHorariaBloquea() {
		Estado e = estado();
		e.docentes().put("d", new Docente("d", "Ana", Vinculacion.COMPLETO, Restriccion.DESPUES_18, Set.of("c")));
		assertTrue(error(evaluar(e, new Sesion("s", "g", 1, 1, 7, "d", "a")), "R8"));
	}

	/**
	 * R11: días consecutivos generan una advertencia sin bloquear.
	 */
	@Test
	void diasConsecutivosAdvierten() {
		Resultado r = evaluar(estado(), new Sesion("s1", "g", 1, 1, 7, "d", "a"), new Sesion("s2", "g", 2, 2, 7, "d", "a"));
		assertTrue(r.valido());
		assertTrue(r.advertencias().stream().anyMatch(x -> x.regla().equals("R11")));
	}
}
