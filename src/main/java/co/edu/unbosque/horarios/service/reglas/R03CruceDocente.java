package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R3: el docente no puede tener otra sesión al mismo tiempo.
 */
public class R03CruceDocente implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R3";
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
		return ctx.ocupacion().docente(s.docenteId(), s.dia(), ctx.inicio(), ctx.fin())
				.map(otra -> List.of("Docente ocupado con " + ctx.etiqueta(otra))).orElse(List.of());
	}
}
