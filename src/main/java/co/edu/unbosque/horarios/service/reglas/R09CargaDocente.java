package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R9: el docente no puede superar las horas máximas semanales de su vinculación.
 */
public class R09CargaDocente implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R9";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean esDura() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> evaluar(Sesion s, ContextoValidacion ctx) {
		Docente d = ctx.docente();
		if (d == null)
			return List.of();
		Integer max = ctx.parametros().horasMaximas().get(d.vinculacion());
		if (max == null)
			return List.of("Falta configurar horas máximas de vinculación " + d.vinculacion());
		long sesiones = ctx.horario().sesiones().stream().filter(x -> x.dia() != null && d.id().equals(x.docenteId()))
				.count();
		long horas = sesiones * ctx.parametros().duracion();
		if (horas > max)
			return List.of("Carga docente " + horas + " h supera máximo " + max + " h");
		return List.of();
	}
}
