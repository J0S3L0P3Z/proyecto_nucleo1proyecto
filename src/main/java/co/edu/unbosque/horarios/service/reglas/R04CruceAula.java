package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R4: el aula no puede tener otra sesión al mismo tiempo.
 */
public class R04CruceAula implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R4";
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
		return ctx.ocupacion().aula(s.aulaId(), s.dia(), ctx.inicio(), ctx.fin())
				.map(otra -> List.of("Aula ocupada por " + ctx.etiqueta(otra))).orElse(List.of());
	}
}
