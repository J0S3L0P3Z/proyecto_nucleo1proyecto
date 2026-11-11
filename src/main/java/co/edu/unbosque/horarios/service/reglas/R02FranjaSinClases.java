package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R2: la sesión no puede cruzar la franja sin clases (por defecto de 12:00 a 13:00).
 */
public class R02FranjaSinClases implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R2";
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
		Parametros p = ctx.parametros();
		if (ctx.inicio() < p.descansoFin() && ctx.fin() > p.descansoInicio())
			return List.of("La sesión cruza la franja sin clases");
		return List.of();
	}
}
