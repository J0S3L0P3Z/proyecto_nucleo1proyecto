package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R11 (blanda): las sesiones de un curso con dos o tres sesiones semanales no deberían quedar en días consecutivos.
 */
public class R11DiasConsecutivos implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R11";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean esDura() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> evaluar(Sesion s, ContextoValidacion ctx) {
		int frecuencia = ctx.curso().frecuencia();
		if (frecuencia != 2 && frecuencia != 3)
			return List.of();
		boolean consecutivas = ctx.horario().sesiones().stream()
				.anyMatch(otra -> !otra.id().equals(s.id()) && otra.grupoId().equals(s.grupoId()) && otra.dia() != null
						&& Math.abs(otra.dia() - s.dia()) == 1);
		return consecutivas ? List.of("Sesiones del grupo en días consecutivos") : List.of();
	}
}
