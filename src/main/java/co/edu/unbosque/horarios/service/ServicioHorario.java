package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Publicación, versiones y consulta de horarios, y carga docente.
 */
@Service
public class ServicioHorario {
	private final AlmacenJson db;
	private final ValidadorRestricciones validador;
	private final ServicioAuditoria auditoria;

	/**
	 * Crea el servicio.
	 *
	 * @param db        almacenamiento del estado
	 * @param validador validador de reglas de asignación
	 * @param auditoria registro de auditoría
	 */
	public ServicioHorario(AlmacenJson db, ValidadorRestricciones validador, ServicioAuditoria auditoria) {
		this.db = db;
		this.validador = validador;
		this.auditoria = auditoria;
	}

	/**
	 * Obtiene una copia del estado actual del sistema.
	 *
	 * @return estado del sistema
	 */
	public Estado estado() {
		return db.leer();
	}

	/**
	 * Horario vigente: la versión publicada más reciente del semestre más reciente.
	 *
	 * @param e estado del sistema
	 * @return horario vigente, si hay alguno publicado
	 */
	public static Optional<Horario> vigente(Estado e) {
		return e.horarios().values().stream().filter(h -> h.estado() == EstadoHorario.PUBLICADO)
				.max(Comparator.comparing(Horario::semestre).thenComparingInt(Horario::version));
	}

	/**
	 * Lista los horarios publicados.
	 *
	 * @return horarios publicados
	 */
	public List<Horario> publicados() {
		return db.leer().horarios().values().stream().filter(h -> h.estado() == EstadoHorario.PUBLICADO).toList();
	}

	/**
	 * Publica un borrador. A partir de ese momento la versión es inmutable.
	 *
	 * @param id      identificador del borrador
	 * @param usuario usuario que publica
	 * @return horario publicado
	 * @throws Errores.Negocio si hay sesiones pendientes o alguna sesión incumple las reglas
	 */
	public Horario publicar(String id, String usuario) {
		return db.cambio(e -> {
			Horario h = ServicioPlanificacion.borrador(e, id);
			if (h.sesiones().stream().anyMatch(s -> s.dia() == null))
				throw new Errores.Negocio("Existen sesiones pendientes");
			for (Sesion s : h.sesiones())
				if (!validador.validar(e, h, s).valido())
					throw new Errores.Negocio("Horario inválido en " + Soporte.etiquetaGrupo(e, s.grupoId()));
			Horario pub = new Horario(h.id(), h.semestre(), h.version(), EstadoHorario.PUBLICADO, h.sesiones());
			e.horarios().put(id, pub);
			auditoria.registrar(e, "EXITO", "Horario publicado",
					"Horario " + h.semestre() + " versión " + h.version() + " publicado", usuario);
			return pub;
		});
	}

	/**
	 * Crea un nuevo borrador a partir de un horario publicado, copiando sus sesiones salvo las
	 * de los grupos cerrados.
	 *
	 * @param id identificador del horario publicado
	 * @return nueva versión en borrador
	 * @throws Errores.Negocio si el horario no existe o no está publicado
	 */
	public Horario nuevaVersion(String id) {
		return db.cambio(e -> {
			Horario anterior = Soporte.existe(e.horarios().get(id), "Horario");
			if (anterior.estado() != EstadoHorario.PUBLICADO)
				throw new Errores.Negocio("Solo se versionan horarios publicados");
			int version = e.horarios().values().stream().filter(h -> h.semestre().equals(anterior.semestre()))
					.mapToInt(Horario::version).max().orElse(0) + 1;
			List<Sesion> sesiones = anterior.sesiones().stream().filter(s -> abierto(e, s.grupoId())).toList();
			Horario h = new Horario(Soporte.nuevoId(), anterior.semestre(), version, EstadoHorario.BORRADOR,
					new ArrayList<>(sesiones));
			e.horarios().put(h.id(), h);
			return h;
		});
	}

	/**
	 * Indica si un grupo existe y está abierto.
	 *
	 * @param e       estado del sistema
	 * @param grupoId identificador del grupo
	 * @return {@code true} si el grupo está abierto
	 */
	private static boolean abierto(Estado e, String grupoId) {
		Grupo g = e.grupos().get(grupoId);
		return g != null && g.estado() == EstadoGrupo.ABIERTO;
	}

	/**
	 * Consulta las sesiones ubicadas de un horario aplicando un filtro. Las sesiones de los
	 * grupos cerrados no se muestran.
	 *
	 * @param horarioId     identificador del horario
	 * @param tipo          filtro: {@code docente}, {@code aula}, {@code grupo}, {@code curso} o {@code todos}
	 * @param id            identificador del elemento filtrado
	 * @param soloPublicado si es {@code true}, rechaza horarios que no estén publicados
	 * @return sesiones que cumplen el filtro
	 * @throws Errores.Negocio si el horario no existe o no está publicado cuando se exige
	 */
	public List<Sesion> consultar(String horarioId, String tipo, String id, boolean soloPublicado) {
		Estado e = db.leer();
		Horario h = Soporte.existe(e.horarios().get(horarioId), "Horario");
		if (soloPublicado && h.estado() != EstadoHorario.PUBLICADO)
			throw new Errores.Negocio("Horario no publicado");
		return h.sesiones().stream().filter(s -> s.dia() != null && abierto(e, s.grupoId()))
				.filter(s -> switch (tipo) {
				case "docente" -> id.equals(s.docenteId());
				case "aula" -> id.equals(s.aulaId());
				case "grupo" -> id.equals(s.grupoId());
				case "curso" -> id.equals(e.grupos().get(s.grupoId()).cursoId());
				case "todos" -> true;
				default -> false;
				}).toList();
	}

	/**
	 * Carga horaria de un docente en un horario.
	 *
	 * @param docenteId      identificador del docente
	 * @param horasAsignadas horas semanales asignadas
	 * @param horasMaximas   horas máximas según su vinculación, o {@code null} si no están configuradas
	 */
	public record Carga(String docenteId, int horasAsignadas, Integer horasMaximas) {
	}

	/**
	 * Calcula la carga horaria de todos los docentes en un horario.
	 *
	 * @param horarioId identificador del horario
	 * @return carga de cada docente
	 */
	public List<Carga> cargas(String horarioId) {
		Estado e = db.leer();
		Horario h = Soporte.existe(e.horarios().get(horarioId), "Horario");
		return e.docentes().values().stream()
				.map(d -> new Carga(d.id(),
						(int) h.sesiones().stream().filter(s -> s.dia() != null && d.id().equals(s.docenteId())).count()
								* e.parametros().duracion(),
						e.parametros().horasMaximas().get(d.vinculacion())))
				.toList();
	}
}
