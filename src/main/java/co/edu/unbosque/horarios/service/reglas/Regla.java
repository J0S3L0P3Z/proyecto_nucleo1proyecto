package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.Sesion;
import java.util.List;

/**
 * Regla de asignación de sesiones.
 * <p>
 * Cada regla es una clase independiente: agregar una regla nueva no exige modificar las
 * existentes, solo registrarla en el validador.
 * </p>
 */
public interface Regla {
	/**
	 * Código de la regla.
	 *
	 * @return código, por ejemplo {@code R3}
	 */
	String codigo();

	/**
	 * Indica si la regla bloquea la asignación.
	 *
	 * @return {@code true} para reglas duras, {@code false} para reglas blandas que solo advierten
	 */
	boolean esDura();

	/**
	 * Evalúa la sesión.
	 *
	 * @param sesion sesión con su ubicación propuesta
	 * @param ctx    datos necesarios para la evaluación
	 * @return mensajes de incumplimiento; vacío si la regla se cumple
	 */
	List<String> evaluar(Sesion sesion, ContextoValidacion ctx);
}
