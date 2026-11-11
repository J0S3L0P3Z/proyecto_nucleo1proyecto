package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R12 (blanda): la jornada del docente no debería tener huecos largos entre sesiones.
 */
public class R12HuecoLargo implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R12";
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
		Docente d = ctx.docente();
		if (d == null)
			return List.of();
		List<Integer> horas = new ArrayList<>();
		for (Sesion x : ctx.horario().sesiones())
			if (!x.id().equals(s.id()) && s.dia().equals(x.dia()) && d.id().equals(x.docenteId()) && x.hora() != null)
				horas.add(x.hora());
		horas.add(ctx.inicio());
		Collections.sort(horas);
		for (int i = 1; i < horas.size(); i++)
			if (horas.get(i) - horas.get(i - 1) - ctx.parametros().duracion() >= ctx.parametros().huecoLargo())
				return List.of("Hueco largo en jornada del docente");
		return List.of();
	}
}
