package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R1: la sesión debe quedar dentro de la franja del día (lunes a viernes o sábado).
 */
public class R01Franja implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R1";
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
		int desde = s.dia() == 6 ? p.inicioSabado() : p.inicioSemana();
		int hasta = s.dia() == 6 ? p.finSabado() : p.finSemana();
		if (ctx.inicio() < desde || ctx.fin() > hasta)
			return List.of("Sesión fuera de la franja del día");
		return List.of();
	}
}
