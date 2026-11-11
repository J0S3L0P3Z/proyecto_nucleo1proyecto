package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R7: el docente debe existir y estar habilitado para el curso.
 */
public class R07Docente implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R7";
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
			return List.of("Docente inexistente");
		if (!d.cursosHabilitados().contains(ctx.curso().id()))
			return List.of("Docente no habilitado para curso " + ctx.curso().codigo());
		return List.of();
	}
}
