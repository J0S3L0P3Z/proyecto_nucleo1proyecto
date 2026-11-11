package co.edu.unbosque.horarios.service.reglas;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.MapaOcupacion;
import co.edu.unbosque.horarios.service.Soporte;

/**
 * Datos que necesitan las reglas para evaluar una sesión.
 *
 * @param estado     estado del sistema
 * @param horario    horario que contiene la sesión con su ubicación propuesta
 * @param parametros parámetros generales
 * @param grupo      grupo de la sesión
 * @param curso      curso del grupo
 * @param docente    docente propuesto, o {@code null} si no existe
 * @param aula       aula propuesta, o {@code null} si no existe
 * @param ocupacion  mapas de ocupación del horario sin la sesión evaluada
 * @param inicio     hora de inicio de la sesión
 * @param fin        hora de fin de la sesión (no incluida)
 */
public record ContextoValidacion(Estado estado, Horario horario, Parametros parametros, Grupo grupo, Curso curso,
		Docente docente, Aula aula, MapaOcupacion ocupacion, int inicio, int fin) {
	/**
	 * Nombre corto del grupo de una sesión, para los mensajes.
	 *
	 * @param s sesión
	 * @return nombre del grupo
	 */
	public String etiqueta(Sesion s) {
		return Soporte.etiquetaGrupo(estado, s.grupoId());
	}
}
