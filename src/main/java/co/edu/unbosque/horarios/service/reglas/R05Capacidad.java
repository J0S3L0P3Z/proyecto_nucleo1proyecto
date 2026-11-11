package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R5: la capacidad del aula debe alcanzar para el cupo del grupo.
 */
public class R05Capacidad implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R5";
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
		Aula a = ctx.aula();
		if (a != null && a.capacidad() < ctx.grupo().cupo())
			return List.of("Capacidad del aula insuficiente para cupo " + ctx.grupo().cupo());
		return List.of();
	}
}
