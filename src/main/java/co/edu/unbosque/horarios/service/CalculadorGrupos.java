package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import org.springframework.stereotype.Component;

/**
 * Calcula cuántos grupos necesita un curso según su demanda.
 * <p>
 * Número mínimo de grupos = ⌈demanda / (cupo × (1 + tolerancia))⌉. Por ejemplo, con cupo 40
 * y tolerancia del 10 %, una demanda de 100 estudiantes requiere 3 grupos.
 * </p>
 */
@Component
public class CalculadorGrupos {
	/**
	 * Capacidad real de cada grupo.
	 *
	 * @param p parámetros generales
	 * @return cupo máximo con la tolerancia aplicada
	 */
	public int capacidad(Parametros p) {
		return p.capacidadGrupo();
	}

	/**
	 * Calcula el número mínimo de grupos de un curso.
	 *
	 * @param c curso
	 * @param p parámetros generales
	 * @return número mínimo de grupos para cubrir la demanda
	 */
	public int calcular(Curso c, Parametros p) {
		return (int) Math.ceil((double) c.demanda() / capacidad(p));
	}
}
