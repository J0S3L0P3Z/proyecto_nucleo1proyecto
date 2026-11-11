package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Módulo de inscripción y cierre de grupos.
 * <p>
 * El estudiante se inscribe en grupos del horario vigente; el sistema valida el cupo y los
 * cruces de horario. Al cerrar el periodo de inscripción, se detectan los grupos por debajo
 * del mínimo y, para cada estudiante afectado, se propone el primer grupo del mismo curso con
 * cupo y sin cruce (enfoque greedy / first-fit). El Comité Táctico aprueba o rechaza cada
 * propuesta, y todas las operaciones quedan en la auditoría.
 * </p>
 */
@Service
public class ServicioInscripcion {
	private final AlmacenJson db;
	private final ServicioAuditoria auditoria;

	/**
	 * Crea el servicio.
	 *
	 * @param db        almacenamiento del estado
	 * @param auditoria registro de auditoría
	 */
	public ServicioInscripcion(AlmacenJson db, ServicioAuditoria auditoria) {
		this.db = db;
		this.auditoria = auditoria;
	}

	/**
	 * Sesión de un grupo, con nombres legibles.
	 *
	 * @param dia     día de la semana
	 * @param hora    hora de inicio
	 * @param horaFin hora de fin
	 * @param aula    código del aula
	 * @param docente nombre del docente
	 */
	public record SesionOferta(int dia, int hora, int horaFin, String aula, String docente) {
	}

	/**
	 * Grupo ofrecido para inscripción.
	 *
	 * @param grupoId     identificador del grupo
	 * @param cursoId     identificador del curso
	 * @param cursoCodigo código del curso
	 * @param cursoNombre nombre del curso
	 * @param numero      número del grupo
	 * @param cupo        cupo máximo
	 * @param inscritos   estudiantes inscritos
	 * @param disponibles cupos libres
	 * @param estado      estado del grupo
	 * @param inscrito    {@code true} si el estudiante que consulta está inscrito en el grupo
	 * @param sesiones    sesiones del grupo en el horario vigente
	 */
	public record GrupoOferta(String grupoId, String cursoId, String cursoCodigo, String cursoNombre, int numero,
			int cupo, int inscritos, int disponibles, EstadoGrupo estado, boolean inscrito,
			List<SesionOferta> sesiones) {
	}

	/**
	 * Oferta de grupos del horario vigente.
	 *
	 * @param horarioId   identificador del horario vigente, o {@code null} si no hay
	 * @param semestre    semestre del horario
	 * @param version     versión del horario
	 * @param inscripcion estado del periodo de inscripción
	 * @param grupos      grupos ofrecidos
	 */
	public record Oferta(String horarioId, String semestre, Integer version, EstadoInscripcion inscripcion,
			List<GrupoOferta> grupos) {
	}

	/**
	 * Estudiante inscrito en un grupo.
	 *
	 * @param estudianteId identificador del estudiante
	 * @param codigo       código estudiantil
	 * @param nombre       nombre completo
	 * @param fecha        fecha de la inscripción
	 */
	public record Inscrito(String estudianteId, String codigo, String nombre, String fecha) {
	}

	/**
	 * Resultado de cerrar el periodo de inscripción.
	 *
	 * @param gruposCriticos grupos por debajo del mínimo
	 * @param propuestas     propuestas de reasignación generadas
	 * @param sinAlternativa estudiantes para los que no se encontró grupo
	 */
	public record ResumenCierre(List<String> gruposCriticos, int propuestas, int sinAlternativa) {
	}

	/**
	 * Resultado de aprobar todas las propuestas pendientes.
	 *
	 * @param aprobadas número de propuestas aprobadas
	 * @param omitidas  propuestas que ya no eran válidas, con el motivo
	 */
	public record ResumenAprobacion(int aprobadas, List<String> omitidas) {
	}

	/**
	 * Propuesta de reasignación con los datos que necesita el Comité para decidir.
	 *
	 * @param id                 identificador de la propuesta
	 * @param estado             estado de la propuesta
	 * @param motivo             explicación
	 * @param fecha              fecha de generación
	 * @param decididaPor        usuario que decidió
	 * @param fechaDecision      fecha de la decisión
	 * @param estudianteId       identificador del estudiante
	 * @param estudianteCodigo   código del estudiante
	 * @param estudianteNombre   nombre del estudiante
	 * @param curso              nombre del curso
	 * @param origenId           grupo de origen
	 * @param origenEtiqueta     nombre del grupo de origen
	 * @param origenSesiones     sesiones del grupo de origen
	 * @param origenInscritos    inscritos en el grupo de origen
	 * @param destinoId          grupo de destino, o {@code null}
	 * @param destinoEtiqueta    nombre del grupo de destino, o {@code null}
	 * @param destinoSesiones    sesiones del grupo de destino
	 * @param destinoDisponibles cupos libres del grupo de destino, o {@code null}
	 */
	public record DetalleReasignacion(String id, EstadoReasignacion estado, String motivo, String fecha,
			String decididaPor, String fechaDecision, String estudianteId, String estudianteCodigo,
			String estudianteNombre, String curso, String origenId, String origenEtiqueta, List<SesionOferta> origenSesiones,
			int origenInscritos, String destinoId, String destinoEtiqueta, List<SesionOferta> destinoSesiones,
			Integer destinoDisponibles) {
	}

	/**
	 * Estado resumido de un grupo para el panel de control.
	 *
	 * @param grupoId   identificador del grupo
	 * @param etiqueta  nombre corto del grupo
	 * @param curso     nombre del curso
	 * @param inscritos estudiantes inscritos
	 * @param cupo      cupo máximo
	 * @param situacion {@code LLENO}, {@code CASI_LLENO}, {@code CRITICO}, {@code NORMAL} o {@code CERRADO}
	 */
	public record GrupoPanel(String grupoId, String etiqueta, String curso, int inscritos, int cupo,
			String situacion) {
	}

	/**
	 * Indicadores del panel de control.
	 *
	 * @param semestre     semestre del horario vigente
	 * @param version      versión del horario vigente
	 * @param inscripcion  estado del periodo de inscripción
	 * @param minimo       mínimo de inscritos
	 * @param totalGrupos  grupos abiertos ofrecidos
	 * @param criticos     grupos por debajo del mínimo
	 * @param llenos       grupos sin cupos
	 * @param pendientes   propuestas pendientes de aprobación
	 * @param grupos       estado de cada grupo
	 * @param actividad    registros de auditoría más recientes
	 */
	public record Panel(String semestre, Integer version, EstadoInscripcion inscripcion, int minimo,
			int totalGrupos, int criticos, int llenos, int pendientes, List<GrupoPanel> grupos,
			List<RegistroAuditoria> actividad) {
	}

	/**
	 * Resultado interno de un intento de inscripción.
	 *
	 * @param inscripcion inscripción creada, o {@code null} si se rechazó
	 * @param error       motivo del rechazo, o {@code null}
	 */
	private record Intento(Inscripcion inscripcion, String error) {
	}

	/**
	 * Obtiene el horario vigente o lanza un error.
	 *
	 * @param e estado del sistema
	 * @return horario vigente
	 * @throws Errores.Negocio si no hay ningún horario publicado
	 */
	private static Horario vigente(Estado e) {
		return ServicioHorario.vigente(e)
				.orElseThrow(() -> new Errores.Negocio("No hay un horario publicado para inscribirse"));
	}

	/**
	 * Grupos abiertos que tienen sesiones ubicadas en un horario.
	 *
	 * @param e estado del sistema
	 * @param h horario
	 * @return identificadores de los grupos ofrecidos
	 */
	private static Set<String> ofrecidos(Estado e, Horario h) {
		return h.sesiones().stream().filter(s -> s.dia() != null).map(Sesion::grupoId)
				.filter(id -> e.grupos().containsKey(id)).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Grupos en los que está inscrito un estudiante.
	 *
	 * @param e            estado del sistema
	 * @param estudianteId identificador del estudiante
	 * @return identificadores de los grupos
	 */
	private static Set<String> gruposDe(Estado e, String estudianteId) {
		return e.inscripciones().values().stream().filter(i -> i.estudianteId().equals(estudianteId))
				.map(Inscripcion::grupoId).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Inscripciones de un grupo, en orden de inscripción.
	 *
	 * @param e       estado del sistema
	 * @param grupoId identificador del grupo
	 * @return inscripciones del grupo
	 */
	private static List<Inscripcion> inscripcionesDe(Estado e, String grupoId) {
		return e.inscripciones().values().stream().filter(i -> i.grupoId().equals(grupoId))
				.sorted(Comparator.comparing(Inscripcion::fecha)).toList();
	}

	/**
	 * Recalcula el número de inscritos de un grupo a partir de las inscripciones.
	 *
	 * @param e       estado dentro de una operación de cambio
	 * @param grupoId identificador del grupo
	 */
	private static void recalcular(Estado e, String grupoId) {
		Grupo g = e.grupos().get(grupoId);
		int n = (int) e.inscripciones().values().stream().filter(i -> i.grupoId().equals(grupoId)).count();
		e.grupos().put(grupoId, new Grupo(g.id(), g.cursoId(), g.numero(), g.cupo(), n, g.estado()));
	}

	/**
	 * Sesiones ubicadas de varios grupos.
	 *
	 * @param h       horario
	 * @param grupos  identificadores de los grupos
	 * @return sesiones de esos grupos
	 */
	private static List<Sesion> sesionesDe(Horario h, Collection<String> grupos) {
		return grupos.stream().flatMap(id -> Soporte.sesionesUbicadas(h, id).stream()).toList();
	}

	/**
	 * Grupo del estudiante con el que se cruza un grupo candidato.
	 *
	 * @param e        estado del sistema
	 * @param h        horario vigente
	 * @param propios  grupos del estudiante que se deben respetar
	 * @param grupoId  grupo candidato
	 * @return grupo en conflicto, si existe
	 */
	private static Optional<String> cruce(Estado e, Horario h, Collection<String> propios, String grupoId) {
		List<Sesion> candidato = Soporte.sesionesUbicadas(h, grupoId);
		int duracion = e.parametros().duracion();
		return propios.stream().filter(id -> !id.equals(grupoId))
				.filter(id -> Soporte.seCruzan(Soporte.sesionesUbicadas(h, id), candidato, duracion)).findFirst();
	}

	/**
	 * Valida si un estudiante puede inscribirse en un grupo.
	 *
	 * @param e   estado del sistema
	 * @param h   horario vigente
	 * @param est estudiante
	 * @param g   grupo
	 * @return motivo del rechazo, o vacío si puede inscribirse
	 */
	private static Optional<String> validarInscripcion(Estado e, Horario h, Estudiante est, Grupo g) {
		if (g.estado() == EstadoGrupo.CERRADO)
			return Optional.of("El grupo está cerrado");
		if (Soporte.sesionesUbicadas(h, g.id()).isEmpty())
			return Optional.of("El grupo no tiene sesiones en el horario publicado");
		Set<String> propios = gruposDe(e, est.id());
		if (propios.contains(g.id()))
			return Optional.of("Ya está inscrito en este grupo");
		Optional<String> mismoCurso = propios.stream()
				.filter(id -> e.grupos().get(id) != null && e.grupos().get(id).cursoId().equals(g.cursoId())).findFirst();
		if (mismoCurso.isPresent())
			return Optional.of("Ya está inscrito en " + Soporte.etiquetaGrupo(e, mismoCurso.get()));
		if (g.inscritos() >= g.cupo())
			return Optional.of("El grupo no tiene cupos disponibles (" + g.inscritos() + "/" + g.cupo() + ")");
		return cruce(e, h, propios, g.id()).map(id -> "Cruce de horario con " + Soporte.etiquetaGrupo(e, id));
	}

	/**
	 * Convierte las sesiones de un grupo en datos legibles.
	 *
	 * @param e       estado del sistema
	 * @param h       horario
	 * @param grupoId identificador del grupo
	 * @return sesiones ordenadas por día y hora
	 */
	private static List<SesionOferta> sesionesOferta(Estado e, Horario h, String grupoId) {
		if (h == null || grupoId == null)
			return List.of();
		int duracion = e.parametros().duracion();
		return Soporte.sesionesUbicadas(h, grupoId).stream()
				.sorted(Comparator.comparing(Sesion::dia).thenComparing(Sesion::hora))
				.map(s -> new SesionOferta(s.dia(), s.hora(), s.hora() + duracion,
						Optional.ofNullable(e.aulas().get(s.aulaId())).map(Aula::codigo).orElse(""),
						Optional.ofNullable(e.docentes().get(s.docenteId())).map(Docente::nombre).orElse("")))
				.toList();
	}

	/**
	 * Construye la oferta de un grupo.
	 *
	 * @param e        estado del sistema
	 * @param h        horario vigente
	 * @param g        grupo
	 * @param inscrito si el estudiante que consulta está inscrito
	 * @return grupo ofrecido
	 */
	private static GrupoOferta grupoOferta(Estado e, Horario h, Grupo g, boolean inscrito) {
		Curso c = e.cursos().get(g.cursoId());
		return new GrupoOferta(g.id(), g.cursoId(), c.codigo(), c.nombre(), g.numero(), g.cupo(), g.inscritos(),
				Math.max(0, g.cupo() - g.inscritos()), g.estado(), inscrito, sesionesOferta(e, h, g.id()));
	}

	/**
	 * Oferta de grupos del horario vigente.
	 *
	 * @param estudianteId estudiante que consulta, o {@code null} si consulta otro rol
	 * @return grupos ofrecidos, ordenados por curso y número
	 */
	public Oferta oferta(String estudianteId) {
		Estado e = db.leer();
		Optional<Horario> vigente = ServicioHorario.vigente(e);
		if (vigente.isEmpty())
			return new Oferta(null, null, null, e.inscripcion(), List.of());
		Horario h = vigente.get();
		Set<String> propios = estudianteId == null ? Set.of() : gruposDe(e, estudianteId);
		List<GrupoOferta> grupos = ofrecidos(e, h).stream().map(id -> e.grupos().get(id))
				.map(g -> grupoOferta(e, h, g, propios.contains(g.id())))
				.sorted(Comparator.comparing(GrupoOferta::cursoCodigo).thenComparingInt(GrupoOferta::numero)).toList();
		return new Oferta(h.id(), h.semestre(), h.version(), e.inscripcion(), grupos);
	}

	/**
	 * Inscribe a un estudiante en un grupo del horario vigente.
	 * <p>
	 * Un intento rechazado también queda registrado en la auditoría.
	 * </p>
	 *
	 * @param estudianteId identificador del estudiante
	 * @param grupoId      identificador del grupo
	 * @param usuario      usuario que realiza la inscripción
	 * @return inscripción creada
	 * @throws Errores.Negocio si el periodo está cerrado, no hay horario vigente o la inscripción
	 *                         no cumple las restricciones de cupo y horario
	 */
	public Inscripcion inscribir(String estudianteId, String grupoId, String usuario) {
		Intento intento = db.cambio(e -> {
			if (e.inscripcion() != EstadoInscripcion.ABIERTA)
				throw new Errores.Negocio("El periodo de inscripción está cerrado");
			Horario h = vigente(e);
			Estudiante est = Soporte.existe(e.estudiantes().get(estudianteId), "Estudiante");
			Grupo g = Soporte.existe(e.grupos().get(grupoId), "Grupo");
			Optional<String> error = validarInscripcion(e, h, est, g);
			if (error.isPresent()) {
				auditoria.registrar(e, "ERROR", "Inscripción rechazada", "Estudiante " + est.codigo()
						+ " intentó inscribirse en " + Soporte.etiquetaGrupo(e, g.id()) + ": " + error.get(), usuario);
				return new Intento(null, error.get());
			}
			Inscripcion i = new Inscripcion(Soporte.nuevoId(), est.id(), g.id(), Soporte.ahora());
			e.inscripciones().put(i.id(), i);
			recalcular(e, g.id());
			Grupo actualizado = e.grupos().get(g.id());
			auditoria.registrar(e, "EXITO", "Inscripción exitosa",
					"Estudiante " + est.codigo() + " inscrito en " + Soporte.etiquetaGrupo(e, g.id()) + ", cupo "
							+ actualizado.inscritos() + "/" + actualizado.cupo(),
					usuario);
			return new Intento(i, null);
		});
		if (intento.error() != null)
			throw new Errores.Negocio(intento.error());
		return intento.inscripcion();
	}

	/**
	 * Cancela la inscripción de un estudiante en un grupo.
	 *
	 * @param estudianteId identificador del estudiante
	 * @param grupoId      identificador del grupo
	 * @param usuario      usuario que cancela
	 * @throws Errores.Negocio si el periodo está cerrado o la inscripción no existe
	 */
	public void cancelar(String estudianteId, String grupoId, String usuario) {
		db.cambio(e -> {
			if (e.inscripcion() != EstadoInscripcion.ABIERTA)
				throw new Errores.Negocio("El periodo de inscripción está cerrado");
			Inscripcion i = Soporte.existe(e.inscripciones().values().stream()
					.filter(x -> x.estudianteId().equals(estudianteId) && x.grupoId().equals(grupoId)).findFirst()
					.orElse(null), "Inscripción");
			e.inscripciones().remove(i.id());
			recalcular(e, grupoId);
			Estudiante est = e.estudiantes().get(estudianteId);
			auditoria.registrar(e, "INFO", "Inscripción cancelada", "Estudiante "
					+ (est == null ? estudianteId : est.codigo()) + " salió de " + Soporte.etiquetaGrupo(e, grupoId),
					usuario);
			return true;
		});
	}

	/**
	 * Grupos en los que está inscrito un estudiante.
	 *
	 * @param estudianteId identificador del estudiante
	 * @return grupos del estudiante
	 */
	public List<GrupoOferta> gruposDelEstudiante(String estudianteId) {
		Estado e = db.leer();
		Horario h = ServicioHorario.vigente(e).orElse(null);
		return gruposDe(e, estudianteId).stream().map(id -> e.grupos().get(id)).filter(Objects::nonNull)
				.map(g -> grupoOferta(e, h, g, true)).toList();
	}

	/**
	 * Estudiantes inscritos en un grupo.
	 *
	 * @param grupoId identificador del grupo
	 * @return inscritos en orden de inscripción
	 */
	public List<Inscrito> inscritos(String grupoId) {
		Estado e = db.leer();
		Soporte.existe(e.grupos().get(grupoId), "Grupo");
		return inscripcionesDe(e, grupoId).stream().map(i -> {
			Estudiante est = e.estudiantes().get(i.estudianteId());
			return new Inscrito(i.estudianteId(), est == null ? "" : est.codigo(), est == null ? "" : est.nombre(),
					i.fecha());
		}).toList();
	}

	/**
	 * Reabre el periodo de inscripción y descarta las propuestas que aún no se han decidido.
	 *
	 * @param usuario usuario que reabre
	 * @return estado del periodo
	 * @throws Errores.Negocio si el periodo ya está abierto
	 */
	public EstadoInscripcion abrir(String usuario) {
		return db.transformar(e -> {
			if (e.inscripcion() == EstadoInscripcion.ABIERTA)
				throw new Errores.Negocio("Las inscripciones ya están abiertas");
			List<String> descartadas = e.reasignaciones().values().stream()
					.filter(r -> r.estado() == EstadoReasignacion.PENDIENTE
							|| r.estado() == EstadoReasignacion.SIN_ALTERNATIVA)
					.map(Reasignacion::id).toList();
			descartadas.forEach(e.reasignaciones()::remove);
			auditoria.registrar(e, "INFO", "Inscripciones abiertas",
					"Periodo de inscripción reabierto; " + descartadas.size() + " propuestas sin decidir descartadas",
					usuario);
			return new AlmacenJson.Cambio<>(e.conInscripcion(EstadoInscripcion.ABIERTA), EstadoInscripcion.ABIERTA);
		});
	}

	/**
	 * Cierra el periodo de inscripción, detecta los grupos por debajo del mínimo y genera
	 * propuestas de reasignación con el enfoque first-fit.
	 *
	 * @param usuario usuario que cierra el periodo
	 * @return resumen del cierre
	 * @throws Errores.Negocio si el periodo ya está cerrado o no hay horario vigente
	 */
	public ResumenCierre cerrar(String usuario) {
		return db.transformar(e -> {
			if (e.inscripcion() == EstadoInscripcion.CERRADA)
				throw new Errores.Negocio("Las inscripciones ya están cerradas");
			Horario h = vigente(e);
			int minimo = e.parametros().minimoInscritos();
			Set<String> ofrecidos = ofrecidos(e, h);
			List<Grupo> criticos = ofrecidos.stream().map(id -> e.grupos().get(id))
					.filter(g -> g.estado() == EstadoGrupo.ABIERTO && g.inscritos() < minimo)
					.sorted(Comparator.comparing((Grupo g) -> e.cursos().get(g.cursoId()).codigo())
							.thenComparingInt(Grupo::numero))
					.toList();
			Set<String> idsCriticos = criticos.stream().map(Grupo::id).collect(Collectors.toSet());
			Map<String, Integer> reservados = new HashMap<>();
			Map<String, List<String>> destinosPorEstudiante = new HashMap<>();
			int propuestas = 0, sinAlternativa = 0;
			auditoria.registrar(e, "INFO", "Inscripciones cerradas", "Se cerró el periodo de inscripción", usuario);
			for (Grupo g : criticos) {
				String etiqueta = Soporte.etiquetaGrupo(e, g.id());
				auditoria.registrar(e, "ALERTA", "Cierre detectado",
						"Grupo " + etiqueta + " por debajo del mínimo (" + g.inscritos() + "/" + minimo + ")", usuario);
				for (Inscripcion ins : inscripcionesDe(e, g.id())) {
					List<String> propios = new ArrayList<>(gruposDe(e, ins.estudianteId()));
					propios.remove(g.id());
					propios.addAll(destinosPorEstudiante.getOrDefault(ins.estudianteId(), List.of()));
					Optional<Grupo> destino = primerDisponible(e, h, g, propios, ofrecidos, idsCriticos, reservados);
					Reasignacion r;
					if (destino.isPresent()) {
						Grupo d = destino.get();
						reservados.merge(d.id(), 1, Integer::sum);
						destinosPorEstudiante.computeIfAbsent(ins.estudianteId(), k -> new ArrayList<>()).add(d.id());
						r = new Reasignacion(Soporte.nuevoId(), ins.estudianteId(), g.id(), d.id(),
								EstadoReasignacion.PENDIENTE, "Grupo " + etiqueta + " por debajo del mínimo ("
										+ g.inscritos() + "/" + minimo + "). Sin conflicto horario.",
								Soporte.ahora(), null, null);
						propuestas++;
					} else {
						r = new Reasignacion(Soporte.nuevoId(), ins.estudianteId(), g.id(), null,
								EstadoReasignacion.SIN_ALTERNATIVA,
								"No hay otro grupo del curso con cupo y sin cruce de horario", Soporte.ahora(), null,
								null);
						sinAlternativa++;
					}
					e.reasignaciones().put(r.id(), r);
				}
			}
			if (!criticos.isEmpty())
				auditoria.registrar(e, "INFO", "Propuesta generada", propuestas
						+ " reasignaciones propuestas al Comité Táctico"
						+ (sinAlternativa > 0 ? "; " + sinAlternativa + " estudiantes sin alternativa" : ""), usuario);
			ResumenCierre resumen = new ResumenCierre(
					criticos.stream().map(g -> Soporte.etiquetaGrupo(e, g.id())).toList(), propuestas, sinAlternativa);
			return new AlmacenJson.Cambio<>(e.conInscripcion(EstadoInscripcion.CERRADA), resumen);
		});
	}

	/**
	 * Busca, en orden de número de grupo, el primer grupo del mismo curso que esté abierto, no
	 * esté por debajo del mínimo, tenga cupo y no se cruce con el horario del estudiante.
	 *
	 * @param e          estado del sistema
	 * @param h          horario vigente
	 * @param origen     grupo que se va a cerrar
	 * @param propios    grupos que conserva el estudiante
	 * @param ofrecidos  grupos con sesiones en el horario vigente
	 * @param criticos   grupos por debajo del mínimo
	 * @param reservados cupos ya comprometidos por otras propuestas de este cierre
	 * @return primer grupo que cumple, si existe
	 */
	private static Optional<Grupo> primerDisponible(Estado e, Horario h, Grupo origen, List<String> propios,
			Set<String> ofrecidos, Set<String> criticos, Map<String, Integer> reservados) {
		return e.grupos().values().stream()
				.filter(g -> g.cursoId().equals(origen.cursoId()) && !g.id().equals(origen.id())
						&& g.estado() == EstadoGrupo.ABIERTO && ofrecidos.contains(g.id())
						&& !criticos.contains(g.id()))
				.sorted(Comparator.comparingInt(Grupo::numero))
				.filter(g -> g.cupo() - g.inscritos() - reservados.getOrDefault(g.id(), 0) > 0)
				.filter(g -> cruce(e, h, propios, g.id()).isEmpty()).findFirst();
	}

	/**
	 * Verifica que una propuesta pendiente todavía se pueda aplicar.
	 *
	 * @param e estado del sistema
	 * @param r propuesta
	 * @return motivo por el que ya no es válida, o vacío si se puede aplicar
	 */
	private static Optional<String> problema(Estado e, Reasignacion r) {
		Optional<Horario> h = ServicioHorario.vigente(e);
		if (h.isEmpty())
			return Optional.of("no hay horario vigente");
		Grupo destino = e.grupos().get(r.grupoDestinoId());
		if (destino == null || destino.estado() != EstadoGrupo.ABIERTO)
			return Optional.of("el grupo destino ya no está abierto");
		Set<String> propios = gruposDe(e, r.estudianteId());
		if (!propios.contains(r.grupoOrigenId()))
			return Optional.of("el estudiante ya no está inscrito en el grupo origen");
		if (propios.contains(destino.id()))
			return Optional.of("el estudiante ya está en el grupo destino");
		if (destino.inscritos() >= destino.cupo())
			return Optional.of("el grupo destino ya no tiene cupo");
		propios.remove(r.grupoOrigenId());
		return cruce(e, h.get(), propios, destino.id())
				.map(id -> "cruce de horario con " + Soporte.etiquetaGrupo(e, id));
	}

	/**
	 * Mueve al estudiante al grupo destino y marca la propuesta como aprobada.
	 *
	 * @param e       estado dentro de una operación de cambio
	 * @param r       propuesta pendiente y válida
	 * @param usuario usuario que aprueba
	 * @return propuesta aprobada
	 */
	private Reasignacion aplicar(Estado e, Reasignacion r, String usuario) {
		e.inscripciones().values().stream()
				.filter(i -> i.estudianteId().equals(r.estudianteId()) && i.grupoId().equals(r.grupoOrigenId()))
				.map(Inscripcion::id).toList().forEach(e.inscripciones()::remove);
		Inscripcion nueva = new Inscripcion(Soporte.nuevoId(), r.estudianteId(), r.grupoDestinoId(), Soporte.ahora());
		e.inscripciones().put(nueva.id(), nueva);
		recalcular(e, r.grupoOrigenId());
		recalcular(e, r.grupoDestinoId());
		Reasignacion aprobada = new Reasignacion(r.id(), r.estudianteId(), r.grupoOrigenId(), r.grupoDestinoId(),
				EstadoReasignacion.APROBADA, r.motivo(), r.fecha(), usuario, Soporte.ahora());
		e.reasignaciones().put(r.id(), aprobada);
		Estudiante est = e.estudiantes().get(r.estudianteId());
		auditoria.registrar(e, "EXITO", "Reasignación aprobada",
				"Estudiante " + (est == null ? r.estudianteId() : est.codigo()) + " movido de "
						+ Soporte.etiquetaGrupo(e, r.grupoOrigenId()) + " a "
						+ Soporte.etiquetaGrupo(e, r.grupoDestinoId()) + " por el Comité Táctico",
				usuario);
		return aprobada;
	}

	/**
	 * Aprueba una propuesta pendiente y mueve al estudiante.
	 *
	 * @param id      identificador de la propuesta
	 * @param usuario usuario del Comité que aprueba
	 * @return propuesta aprobada
	 * @throws Errores.Negocio si la propuesta no está pendiente o ya no es válida
	 */
	public Reasignacion aprobar(String id, String usuario) {
		return db.cambio(e -> {
			Reasignacion r = Soporte.existe(e.reasignaciones().get(id), "Reasignación");
			if (r.estado() != EstadoReasignacion.PENDIENTE)
				throw new Errores.Negocio("Solo se aprueban propuestas pendientes");
			problema(e, r).ifPresent(p -> {
				throw new Errores.Negocio("La propuesta ya no es válida: " + p);
			});
			return aplicar(e, r, usuario);
		});
	}

	/**
	 * Aprueba todas las propuestas pendientes que sigan siendo válidas.
	 *
	 * @param usuario usuario del Comité que aprueba
	 * @return número de aprobadas y motivos de las omitidas
	 */
	public ResumenAprobacion aprobarTodas(String usuario) {
		return db.cambio(e -> {
			int aprobadas = 0;
			List<String> omitidas = new ArrayList<>();
			List<Reasignacion> pendientes = e.reasignaciones().values().stream()
					.filter(r -> r.estado() == EstadoReasignacion.PENDIENTE).toList();
			for (Reasignacion r : pendientes) {
				Optional<String> p = problema(e, r);
				if (p.isPresent()) {
					Estudiante est = e.estudiantes().get(r.estudianteId());
					omitidas.add((est == null ? r.estudianteId() : est.codigo()) + ": " + p.get());
				} else {
					aplicar(e, r, usuario);
					aprobadas++;
				}
			}
			return new ResumenAprobacion(aprobadas, omitidas);
		});
	}

	/**
	 * Rechaza una propuesta pendiente. El estudiante permanece en su grupo de origen.
	 *
	 * @param id      identificador de la propuesta
	 * @param motivo  motivo del rechazo; si está vacío se usa uno genérico
	 * @param usuario usuario del Comité que rechaza
	 * @return propuesta rechazada
	 * @throws Errores.Negocio si la propuesta no está pendiente
	 */
	public Reasignacion rechazar(String id, String motivo, String usuario) {
		return db.cambio(e -> {
			Reasignacion r = Soporte.existe(e.reasignaciones().get(id), "Reasignación");
			if (r.estado() != EstadoReasignacion.PENDIENTE)
				throw new Errores.Negocio("Solo se rechazan propuestas pendientes");
			String texto = motivo == null || motivo.isBlank() ? "Rechazada por el Comité Táctico" : motivo.trim();
			Reasignacion rechazada = new Reasignacion(r.id(), r.estudianteId(), r.grupoOrigenId(), r.grupoDestinoId(),
					EstadoReasignacion.RECHAZADA, texto, r.fecha(), usuario, Soporte.ahora());
			e.reasignaciones().put(id, rechazada);
			Estudiante est = e.estudiantes().get(r.estudianteId());
			auditoria.registrar(e, "ERROR", "Reasignación rechazada",
					"Propuesta de " + (est == null ? r.estudianteId() : est.codigo()) + " de "
							+ Soporte.etiquetaGrupo(e, r.grupoOrigenId()) + " a "
							+ Soporte.etiquetaGrupo(e, r.grupoDestinoId()) + " rechazada: " + texto,
					usuario);
			return rechazada;
		});
	}

	/**
	 * Cierra un grupo por debajo del mínimo después de decidir sus propuestas. Los estudiantes
	 * que siguen inscritos en él quedan sin ese grupo.
	 *
	 * @param grupoId identificador del grupo
	 * @param usuario usuario del Comité que cierra el grupo
	 * @return grupo cerrado
	 * @throws Errores.Negocio si las inscripciones siguen abiertas, el grupo no está por debajo del
	 *                         mínimo o tiene propuestas pendientes
	 */
	public Grupo cerrarGrupo(String grupoId, String usuario) {
		return db.cambio(e -> {
			Grupo g = Soporte.existe(e.grupos().get(grupoId), "Grupo");
			if (e.inscripcion() != EstadoInscripcion.CERRADA)
				throw new Errores.Negocio("Primero cierre el periodo de inscripción");
			if (g.estado() == EstadoGrupo.CERRADO)
				throw new Errores.Negocio("El grupo ya está cerrado");
			if (g.inscritos() >= e.parametros().minimoInscritos())
				throw new Errores.Negocio("El grupo no está por debajo del mínimo");
			if (e.reasignaciones().values().stream().anyMatch(
					r -> r.grupoOrigenId().equals(grupoId) && r.estado() == EstadoReasignacion.PENDIENTE))
				throw new Errores.Negocio("Decida primero las propuestas pendientes de este grupo");
			List<Inscripcion> restantes = inscripcionesDe(e, grupoId);
			restantes.forEach(i -> e.inscripciones().remove(i.id()));
			Grupo cerrado = new Grupo(g.id(), g.cursoId(), g.numero(), g.cupo(), 0, EstadoGrupo.CERRADO);
			e.grupos().put(grupoId, cerrado);
			auditoria.registrar(e, "ALERTA", "Grupo cerrado", "Grupo " + Soporte.etiquetaGrupo(e, grupoId)
					+ " cerrado; " + restantes.size() + " estudiantes quedaron sin reasignar", usuario);
			return cerrado;
		});
	}

	/**
	 * Lista las propuestas de reasignación con los datos que necesita el Comité.
	 *
	 * @param estado filtro por estado, o {@code null} para todas
	 * @return propuestas, de la más reciente a la más antigua
	 */
	public List<DetalleReasignacion> reasignaciones(EstadoReasignacion estado) {
		Estado e = db.leer();
		Horario h = ServicioHorario.vigente(e).orElse(null);
		List<DetalleReasignacion> lista = new ArrayList<>();
		for (Reasignacion r : e.reasignaciones().values()) {
			if (estado != null && r.estado() != estado)
				continue;
			Estudiante est = e.estudiantes().get(r.estudianteId());
			Grupo origen = e.grupos().get(r.grupoOrigenId());
			Grupo destino = r.grupoDestinoId() == null ? null : e.grupos().get(r.grupoDestinoId());
			Curso c = origen == null ? null : e.cursos().get(origen.cursoId());
			lista.add(new DetalleReasignacion(r.id(), r.estado(), r.motivo(), r.fecha(), r.decididaPor(),
					r.fechaDecision(), r.estudianteId(), est == null ? "" : est.codigo(),
					est == null ? "" : est.nombre(), c == null ? "" : c.nombre(), r.grupoOrigenId(),
					Soporte.etiquetaGrupo(e, r.grupoOrigenId()), sesionesOferta(e, h, r.grupoOrigenId()),
					origen == null ? 0 : origen.inscritos(), r.grupoDestinoId(),
					destino == null ? null : Soporte.etiquetaGrupo(e, destino.id()),
					sesionesOferta(e, h, r.grupoDestinoId()),
					destino == null ? null : Math.max(0, destino.cupo() - destino.inscritos())));
		}
		Collections.reverse(lista);
		return lista;
	}

	/**
	 * Indicadores del panel de control del Comité.
	 *
	 * @return panel con grupos, propuestas pendientes y actividad reciente
	 */
	public Panel panel() {
		Estado e = db.leer();
		Optional<Horario> h = ServicioHorario.vigente(e);
		int minimo = e.parametros().minimoInscritos();
		Collection<Grupo> grupos = h.map(x -> ofrecidos(e, x).stream().map(id -> e.grupos().get(id)).toList())
				.orElse(List.copyOf(e.grupos().values()));
		List<GrupoPanel> lista = grupos.stream().map(g -> {
			Curso c = e.cursos().get(g.cursoId());
			String situacion = g.estado() == EstadoGrupo.CERRADO ? "CERRADO"
					: g.inscritos() >= g.cupo() ? "LLENO"
							: g.inscritos() >= g.cupo() * 0.9 ? "CASI_LLENO"
									: g.inscritos() < minimo ? "CRITICO" : "NORMAL";
			return new GrupoPanel(g.id(), Soporte.etiquetaGrupo(e, g.id()), c == null ? "" : c.nombre(),
					g.inscritos(), g.cupo(), situacion);
		}).sorted(Comparator.comparing(GrupoPanel::etiqueta)).toList();
		List<RegistroAuditoria> actividad = new ArrayList<>(e.auditoria());
		Collections.reverse(actividad);
		return new Panel(h.map(Horario::semestre).orElse(null), h.map(Horario::version).orElse(null),
				e.inscripcion(), minimo, (int) lista.stream().filter(g -> !g.situacion().equals("CERRADO")).count(),
				(int) lista.stream().filter(g -> g.situacion().equals("CRITICO")).count(),
				(int) lista.stream().filter(g -> g.situacion().equals("LLENO")).count(),
				(int) e.reasignaciones().values().stream().filter(r -> r.estado() == EstadoReasignacion.PENDIENTE)
						.count(),
				lista, actividad.stream().limit(8).toList());
	}
}
