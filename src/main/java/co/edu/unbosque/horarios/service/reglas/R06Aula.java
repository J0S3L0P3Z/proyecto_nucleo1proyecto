package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R6: el aula debe existir y tener la característica que exige el curso.
 */
public class R06Aula implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R6";
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
		if (a == null)
			return List.of("Aula inexistente");
		String tipo = ctx.curso().tipoAula();
		if (tipo != null && !tipo.isBlank() && !a.caracteristicas().contains(tipo))
			return List.of("Aula sin característica requerida: " + tipo);
		return List.of();
	}
}
