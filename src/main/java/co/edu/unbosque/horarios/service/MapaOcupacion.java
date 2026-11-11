package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import java.util.*;

/**
 * Mapas de ocupación de docentes, aulas y grupos dentro de un horario.
 * <p>
 * Cada mapa relaciona un identificador con las franjas de una hora que ocupa, de modo que
 * detectar un cruce cuesta una consulta por hora de la sesión, sin recorrer todas las sesiones.
 * </p>
 */
public final class MapaOcupacion {
	private final Map<String, Map<Integer, Sesion>> docentes = new HashMap<>();
	private final Map<String, Map<Integer, Sesion>> aulas = new HashMap<>();
	private final Map<String, Map<Integer, Sesion>> grupos = new HashMap<>();

	/**
	 * Construye los mapas a partir de las sesiones ubicadas de un horario.
	 *
	 * @param h        horario
	 * @param duracion duración de cada sesión en horas
	 * @param excluir  identificador de la sesión que se está evaluando, que no se registra
	 */
	public MapaOcupacion(Horario h, int duracion, String excluir) {
		for (Sesion s : h.sesiones()) {
			if (s.dia() == null || s.hora() == null || s.id().equals(excluir))
				continue;
			for (int hora = s.hora(); hora < s.hora() + duracion; hora++) {
				int clave = clave(s.dia(), hora);
				registrar(docentes, s.docenteId(), clave, s);
				registrar(aulas, s.aulaId(), clave, s);
				registrar(grupos, s.grupoId(), clave, s);
			}
		}
	}

	/**
	 * Clave única de una franja de una hora.
	 *
	 * @param dia  día de la semana
	 * @param hora hora
	 * @return clave numérica
	 */
	private static int clave(int dia, int hora) {
		return dia * 24 + hora;
	}

	/**
	 * Registra una sesión en la franja de un elemento.
	 *
	 * @param mapa  mapa de ocupación
	 * @param id    identificador del docente, aula o grupo
	 * @param clave franja
	 * @param s     sesión que ocupa la franja
	 */
	private static void registrar(Map<String, Map<Integer, Sesion>> mapa, String id, int clave, Sesion s) {
		if (id != null)
			mapa.computeIfAbsent(id, k -> new HashMap<>()).putIfAbsent(clave, s);
	}

	/**
	 * Busca una sesión que ocupe alguna hora del intervalo.
	 *
	 * @param mapa   mapa de ocupación
	 * @param id     identificador del docente, aula o grupo
	 * @param dia    día de la semana
	 * @param inicio hora de inicio
	 * @param fin    hora de fin (no incluida)
	 * @return la primera sesión en conflicto, si existe
	 */
	private static Optional<Sesion> buscar(Map<String, Map<Integer, Sesion>> mapa, String id, int dia, int inicio,
			int fin) {
		Map<Integer, Sesion> ocupadas = id == null ? null : mapa.get(id);
		if (ocupadas == null)
			return Optional.empty();
		for (int hora = inicio; hora < fin; hora++) {
			Sesion s = ocupadas.get(clave(dia, hora));
			if (s != null)
				return Optional.of(s);
		}
		return Optional.empty();
	}

	/**
	 * Sesión del docente que se cruza con el intervalo.
	 *
	 * @param docenteId identificador del docente
	 * @param dia       día de la semana
	 * @param inicio    hora de inicio
	 * @param fin       hora de fin (no incluida)
	 * @return sesión en conflicto, si existe
	 */
	public Optional<Sesion> docente(String docenteId, int dia, int inicio, int fin) {
		return buscar(docentes, docenteId, dia, inicio, fin);
	}

	/**
	 * Sesión del aula que se cruza con el intervalo.
	 *
	 * @param aulaId identificador del aula
	 * @param dia    día de la semana
	 * @param inicio hora de inicio
	 * @param fin    hora de fin (no incluida)
	 * @return sesión en conflicto, si existe
	 */
	public Optional<Sesion> aula(String aulaId, int dia, int inicio, int fin) {
		return buscar(aulas, aulaId, dia, inicio, fin);
	}

	/**
	 * Sesión del grupo que se cruza con el intervalo.
	 *
	 * @param grupoId identificador del grupo
	 * @param dia     día de la semana
	 * @param inicio  hora de inicio
	 * @param fin     hora de fin (no incluida)
	 * @return sesión en conflicto, si existe
	 */
	public Optional<Sesion> grupo(String grupoId, int dia, int inicio, int fin) {
		return buscar(grupos, grupoId, dia, inicio, fin);
	}
}
