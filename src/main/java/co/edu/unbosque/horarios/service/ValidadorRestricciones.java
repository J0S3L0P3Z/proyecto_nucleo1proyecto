package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.reglas.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Evalúa una sesión frente a todas las reglas de asignación.
 * <p>
 * Las reglas duras (R1 a R10) impiden la asignación y las blandas (R11 y R12) solo generan
 * advertencias. Cada regla es una implementación de {@link Regla}.
 * </p>
 */
@Service
public class ValidadorRestricciones {
	private final List<Regla> reglas = List.of(new R01Franja(), new R02FranjaSinClases(), new R03CruceDocente(),
			new R04CruceAula(), new R05Capacidad(), new R06Aula(), new R07Docente(), new R08RestriccionDocente(),
			new R09CargaDocente(), new R10Grupo(), new R11DiasConsecutivos(), new R12HuecoLargo());

	/**
	 * Reglas registradas, en el orden en que se evalúan.
	 *
	 * @return lista inmutable de reglas
	 */
	public List<Regla> reglas() {
		return reglas;
	}

	/**
	 * Evalúa una sesión dentro de un horario.
	 *
	 * @param e estado del sistema
	 * @param h horario que contiene la sesión con su ubicación propuesta
	 * @param s sesión que se va a evaluar
	 * @return resultado con errores y advertencias
	 * @throws Errores.Negocio si el grupo o el curso de la sesión no existen
	 */
	public Resultado validar(Estado e, Horario h, Sesion s) {
		Grupo g = e.grupos().get(s.grupoId());
		if (g == null)
			throw new Errores.Negocio("Grupo inexistente");
		Curso c = e.cursos().get(g.cursoId());
		if (c == null)
			throw new Errores.Negocio("Curso inexistente");
		if (s.dia() == null || s.hora() == null || s.dia() < 1 || s.dia() > 6)
			return new Resultado(false, List.of(new Hallazgo("R1", "DURA", "Día u hora no válidos")), List.of());
		Parametros p = e.parametros();
		ContextoValidacion ctx = new ContextoValidacion(e, h, p, g, c, e.docentes().get(s.docenteId()),
				e.aulas().get(s.aulaId()), new MapaOcupacion(h, p.duracion(), s.id()), s.hora(),
				s.hora() + p.duracion());
		List<Hallazgo> errores = new ArrayList<>();
		List<Hallazgo> advertencias = new ArrayList<>();
		for (Regla r : reglas)
			for (String mensaje : r.evaluar(s, ctx))
				(r.esDura() ? errores : advertencias)
						.add(new Hallazgo(r.codigo(), r.esDura() ? "DURA" : "BLANDA", mensaje));
		return new Resultado(errores.isEmpty(), errores, advertencias);
	}
}
