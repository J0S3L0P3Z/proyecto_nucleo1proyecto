package co.edu.unbosque.horarios.model;

import java.util.*;

/**
 * Modelo de dominio del sistema de horarios.
 * <p>
 * Agrupa los enumerados y los records inmutables que representan usuarios, catálogos
 * (docentes, cursos, aulas, estudiantes), grupos, sesiones, horarios, inscripciones,
 * reasignaciones, auditoría y parámetros generales.
 * </p>
 */
public final class Domain {
	/**
	 * Constructor privado: esta clase solo agrupa tipos y no debe instanciarse.
	 */
	private Domain() {
	}

	/**
	 * Rol de un usuario dentro del sistema, que determina a qué endpoints puede acceder.
	 */
	public enum Rol {
		DIRECTOR, COMITE, DOCENTE, ESTUDIANTE
	}

	/**
	 * Tipo de vinculación laboral de un docente; define sus horas máximas semanales.
	 */
	public enum Vinculacion {
		COMPLETO, TRES_CUARTOS, MEDIO, CUARTO
	}

	/**
	 * Restricción horaria de un docente, que limita las franjas en que puede dictar clase.
	 */
	public enum Restriccion {
		NINGUNA, SOLO_DIURNO, DESPUES_16, DESPUES_18
	}

	/**
	 * Estado de un grupo: abierto o cerrado por baja inscripción.
	 */
	public enum EstadoGrupo {
		ABIERTO, CERRADO
	}

	/**
	 * Estado de una versión de horario: borrador editable o publicado e inmutable.
	 */
	public enum EstadoHorario {
		BORRADOR, PUBLICADO
	}

	/**
	 * Estado del periodo de inscripción de estudiantes.
	 */
	public enum EstadoInscripcion {
		ABIERTA, CERRADA
	}

	/**
	 * Estado de una propuesta de reasignación.
	 */
	public enum EstadoReasignacion {
		PENDIENTE, APROBADA, RECHAZADA, SIN_ALTERNATIVA
	}

	/**
	 * Cuenta de acceso al sistema.
	 *
	 * @param id           identificador único del usuario
	 * @param username     nombre con el que inicia sesión
	 * @param passwordHash contraseña cifrada con BCrypt
	 * @param rol          rol del usuario
	 * @param docenteId    identificador del docente vinculado, o {@code null} si no es docente
	 * @param estudianteId identificador del estudiante vinculado, o {@code null} si no es estudiante
	 */
	public record Usuario(String id, String username, String passwordHash, Rol rol, String docenteId,
			String estudianteId) {
	}

	/**
	 * Docente que puede dictar cursos.
	 *
	 * @param id                identificador único del docente
	 * @param nombre            nombre completo
	 * @param vinculacion       tipo de vinculación laboral
	 * @param restriccion       restricción horaria del docente
	 * @param cursosHabilitados identificadores de los cursos que puede dictar
	 */
	public record Docente(String id, String nombre, Vinculacion vinculacion, Restriccion restriccion,
			Set<String> cursosHabilitados) {
	}

	/**
	 * Curso del plan de estudios.
	 *
	 * @param id         identificador único del curso
	 * @param codigo     código académico del curso
	 * @param nombre     nombre del curso
	 * @param semestre   semestre al que pertenece
	 * @param frecuencia número de sesiones semanales
	 * @param tipoAula   característica que debe tener el aula, o vacío si no exige ninguna
	 * @param demanda    número de estudiantes esperados
	 */
	public record Curso(String id, String codigo, String nombre, int semestre, int frecuencia, String tipoAula,
			int demanda) {
	}

	/**
	 * Aula física donde se dictan las sesiones.
	 *
	 * @param id              identificador único del aula
	 * @param codigo          código del aula
	 * @param capacidad       número de puestos
	 * @param caracteristicas características del aula (por ejemplo, video beam)
	 */
	public record Aula(String id, String codigo, int capacidad, Set<String> caracteristicas) {
	}

	/**
	 * Estudiante que puede inscribirse en grupos.
	 *
	 * @param id     identificador único del estudiante
	 * @param codigo código estudiantil
	 * @param nombre nombre completo
	 */
	public record Estudiante(String id, String codigo, String nombre) {
	}

	/**
	 * Grupo de un curso.
	 *
	 * @param id        identificador único del grupo
	 * @param cursoId   identificador del curso al que pertenece
	 * @param numero    número consecutivo del grupo dentro del curso
	 * @param cupo      cupo máximo de estudiantes
	 * @param inscritos estudiantes inscritos, calculado a partir de las inscripciones
	 * @param estado    estado del grupo
	 */
	public record Grupo(String id, String cursoId, int numero, int cupo, int inscritos, EstadoGrupo estado) {
	}

	/**
	 * Sesión semanal de un grupo dentro de un horario.
	 * <p>
	 * Mientras no se ubique, el día, la hora, el docente y el aula son {@code null}.
	 * </p>
	 *
	 * @param id        identificador único de la sesión
	 * @param grupoId   identificador del grupo
	 * @param indice    número de la sesión dentro de la semana (1 hasta la frecuencia del curso)
	 * @param dia       día de la semana, de 1 (lunes) a 6 (sábado)
	 * @param hora      hora de inicio
	 * @param docenteId identificador del docente asignado
	 * @param aulaId    identificador del aula asignada
	 */
	public record Sesion(String id, String grupoId, int indice, Integer dia, Integer hora, String docenteId,
			String aulaId) {
	}

	/**
	 * Versión de un horario semestral.
	 *
	 * @param id       identificador único de la versión
	 * @param semestre semestre académico (por ejemplo, 2026-2)
	 * @param version  número de versión dentro del semestre
	 * @param estado   estado de la versión
	 * @param sesiones sesiones que componen el horario
	 */
	public record Horario(String id, String semestre, int version, EstadoHorario estado, List<Sesion> sesiones) {
	}

	/**
	 * Inscripción de un estudiante en un grupo.
	 *
	 * @param id           identificador único de la inscripción
	 * @param estudianteId identificador del estudiante
	 * @param grupoId      identificador del grupo
	 * @param fecha        fecha y hora de la inscripción
	 */
	public record Inscripcion(String id, String estudianteId, String grupoId, String fecha) {
	}

	/**
	 * Propuesta de reasignación de un estudiante de un grupo que se va a cerrar a otro grupo
	 * del mismo curso.
	 *
	 * @param id             identificador único de la propuesta
	 * @param estudianteId   estudiante afectado
	 * @param grupoOrigenId  grupo por debajo del mínimo
	 * @param grupoDestinoId grupo propuesto, o {@code null} si no hay alternativa
	 * @param estado         estado de la propuesta
	 * @param motivo         explicación de la propuesta o de la decisión
	 * @param fecha          fecha y hora en que se generó
	 * @param decididaPor    usuario que aprobó o rechazó la propuesta
	 * @param fechaDecision  fecha y hora de la decisión
	 */
	public record Reasignacion(String id, String estudianteId, String grupoOrigenId, String grupoDestinoId,
			EstadoReasignacion estado, String motivo, String fecha, String decididaPor, String fechaDecision) {
	}

	/**
	 * Registro de auditoría. Solo se agrega; nunca se modifica ni se elimina.
	 *
	 * @param numero      número consecutivo del registro
	 * @param fecha       fecha y hora de la acción
	 * @param tipo        {@code EXITO}, {@code ERROR}, {@code ALERTA} o {@code INFO}
	 * @param accion      nombre corto de la acción
	 * @param descripcion detalle de la acción
	 * @param usuario     usuario que realizó la acción
	 */
	public record RegistroAuditoria(int numero, String fecha, String tipo, String accion, String descripcion,
			String usuario) {
	}

	/**
	 * Parámetros generales que usan las reglas de validación.
	 *
	 * @param inicioSemana    hora de inicio de clases de lunes a viernes
	 * @param finSemana       hora de fin de clases de lunes a viernes
	 * @param inicioSabado    hora de inicio de clases los sábados
	 * @param finSabado       hora de fin de clases los sábados
	 * @param descansoInicio  hora de inicio de la franja sin clases
	 * @param descansoFin     hora de fin de la franja sin clases
	 * @param duracion        duración de cada sesión en horas
	 * @param frecuenciaMax   máximo de sesiones semanales por curso
	 * @param cupoMax         cupo máximo por grupo
	 * @param tolerancia      porcentaje adicional permitido sobre el cupo (0.10 = 10 %)
	 * @param minimoInscritos mínimo de inscritos para mantener un grupo abierto
	 * @param horasMaximas    horas semanales máximas por tipo de vinculación
	 * @param finDiurno       hora en que termina la jornada diurna
	 * @param huecoLargo      horas libres entre sesiones que se consideran hueco largo
	 */
	public record Parametros(int inicioSemana, int finSemana, int inicioSabado, int finSabado, int descansoInicio,
			int descansoFin, int duracion, int frecuenciaMax, int cupoMax, double tolerancia, int minimoInscritos,
			Map<Vinculacion, Integer> horasMaximas, int finDiurno, int huecoLargo) {
		/**
		 * Crea los parámetros por defecto del sistema.
		 *
		 * @return parámetros iniciales
		 */
		public static Parametros iniciales() {
			return new Parametros(7, 22, 7, 13, 12, 13, 2, 4, 40, 0.10, 10, new EnumMap<>(Vinculacion.class), 18, 3);
		}

		/**
		 * Calcula la capacidad real de un grupo aplicando la tolerancia sobre el cupo máximo.
		 *
		 * @return número máximo de estudiantes por grupo
		 */
		public int capacidadGrupo() {
			return (int) Math.floor(cupoMax * (1 + tolerancia) + 1e-8);
		}
	}

	/**
	 * Estado completo del sistema, tal como se persiste en el archivo JSON.
	 * <p>
	 * Las colecciones nulas se reemplazan por colecciones vacías, de modo que un archivo
	 * de una versión anterior, sin inscripciones ni auditoría, se puede seguir leyendo.
	 * </p>
	 *
	 * @param usuarios       usuarios indexados por identificador
	 * @param docentes       docentes indexados por identificador
	 * @param cursos         cursos indexados por identificador
	 * @param aulas          aulas indexadas por identificador
	 * @param grupos         grupos indexados por identificador
	 * @param horarios       versiones de horario indexadas por identificador
	 * @param parametros     parámetros generales
	 * @param estudiantes    estudiantes indexados por identificador
	 * @param inscripciones  inscripciones indexadas por identificador
	 * @param reasignaciones propuestas de reasignación indexadas por identificador
	 * @param auditoria      registro de auditoría en orden cronológico
	 * @param inscripcion    estado del periodo de inscripción
	 */
	public record Estado(Map<String, Usuario> usuarios, Map<String, Docente> docentes, Map<String, Curso> cursos,
			Map<String, Aula> aulas, Map<String, Grupo> grupos, Map<String, Horario> horarios, Parametros parametros,
			Map<String, Estudiante> estudiantes, Map<String, Inscripcion> inscripciones,
			Map<String, Reasignacion> reasignaciones, List<RegistroAuditoria> auditoria,
			EstadoInscripcion inscripcion) {
		/**
		 * Normaliza los valores nulos.
		 */
		public Estado {
			usuarios = usuarios == null ? new LinkedHashMap<>() : usuarios;
			docentes = docentes == null ? new LinkedHashMap<>() : docentes;
			cursos = cursos == null ? new LinkedHashMap<>() : cursos;
			aulas = aulas == null ? new LinkedHashMap<>() : aulas;
			grupos = grupos == null ? new LinkedHashMap<>() : grupos;
			horarios = horarios == null ? new LinkedHashMap<>() : horarios;
			parametros = parametros == null ? Parametros.iniciales() : parametros;
			estudiantes = estudiantes == null ? new LinkedHashMap<>() : estudiantes;
			inscripciones = inscripciones == null ? new LinkedHashMap<>() : inscripciones;
			reasignaciones = reasignaciones == null ? new LinkedHashMap<>() : reasignaciones;
			auditoria = auditoria == null ? new ArrayList<>() : auditoria;
			inscripcion = inscripcion == null ? EstadoInscripcion.ABIERTA : inscripcion;
		}

		/**
		 * Crea un estado sin datos y con los parámetros iniciales.
		 *
		 * @return estado vacío
		 */
		public static Estado vacio() {
			return new Estado(null, null, null, null, null, null, null, null, null, null, null, null);
		}

		/**
		 * Devuelve una copia del estado con otros parámetros. Las colecciones se comparten.
		 *
		 * @param p nuevos parámetros
		 * @return estado con los parámetros reemplazados
		 */
		public Estado conParametros(Parametros p) {
			return new Estado(usuarios, docentes, cursos, aulas, grupos, horarios, p, estudiantes, inscripciones,
					reasignaciones, auditoria, inscripcion);
		}

		/**
		 * Devuelve una copia del estado con otro estado de inscripción. Las colecciones se comparten.
		 *
		 * @param i nuevo estado del periodo de inscripción
		 * @return estado con el periodo de inscripción reemplazado
		 */
		public Estado conInscripcion(EstadoInscripcion i) {
			return new Estado(usuarios, docentes, cursos, aulas, grupos, horarios, parametros, estudiantes,
					inscripciones, reasignaciones, auditoria, i);
		}
	}
}
