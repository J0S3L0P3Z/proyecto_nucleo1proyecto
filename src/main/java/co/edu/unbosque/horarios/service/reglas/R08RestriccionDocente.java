package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R8: se respeta la restricción horaria del docente (solo diurno, desde las 16:00 o desde las 18:00).
 */
public class R08RestriccionDocente implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R8";
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
		Restriccion r = d.restriccion() == null ? Restriccion.NINGUNA : d.restriccion();
		boolean incumple = switch (r) {
		case SOLO_DIURNO -> ctx.fin() > ctx.parametros().finDiurno();
		case DESPUES_16 -> ctx.inicio() < 16;
		case DESPUES_18 -> ctx.inicio() < 18;
		case NINGUNA -> false;
		};
		return incumple ? List.of("Restricción horaria del docente: " + r) : List.of();
	}
}
