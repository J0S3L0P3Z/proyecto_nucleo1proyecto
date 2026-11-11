package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * R10: no se supera la frecuencia semanal del curso y el grupo no tiene dos sesiones al mismo tiempo.
 */
public class R10Grupo implements Regla {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String codigo() {
		return "R10";
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
		List<String> mensajes = new ArrayList<>();
		long sesionesGrupo = ctx.horario().sesiones().stream().filter(x -> x.grupoId().equals(s.grupoId())).count();
		if (sesionesGrupo > ctx.curso().frecuencia())
			mensajes.add("Frecuencia semanal superada");
		ctx.ocupacion().grupo(s.grupoId(), s.dia(), ctx.inicio(), ctx.fin())
				.ifPresent(otra -> mensajes.add("El grupo ya tiene otra sesión en esa franja"));
		return mensajes;
	}
}
