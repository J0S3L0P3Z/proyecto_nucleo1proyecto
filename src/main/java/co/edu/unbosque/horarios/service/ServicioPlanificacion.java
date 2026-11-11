package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.reglas.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Planificación del horario: cálculo y apertura de grupos, creación de borradores y
 * asignación asistida de sesiones.
 */
@Service
public class ServicioPlanificacion {
	private final AlmacenJson db;
	private final ValidadorRestricciones validador;
	private final CalculadorGrupos calculador;

	/**
	 * Crea el servicio.
	 *
	 * @param db         almacenamiento del estado
	 * @param validador  validador de reglas de asignación
	 * @param calculador calculador del número de grupos
	 */
	public ServicioPlanificacion(AlmacenJson db, ValidadorRestricciones validador, CalculadorGrupos calculador) {
		this.db = db;
		this.validador = validador;
		this.calculador = calculador;
	}

	/**
	 * Plan de apertura de grupos de un curso.
	 *
	 * @param minimo    número mínimo de grupos para cubrir la demanda
	 * @param capacidad capacidad real de cada grupo
	 * @param demanda   estudiantes esperados
	 */
	public record Plan(int minimo, int capacidad, int demanda) {
	}

	/**
	 * Calcula cuántos grupos necesita un curso según su demanda.
	 *
	 * @param cursoId identificador del curso
	 * @return plan de grupos
	 * @throws Errores.Negocio si el curso no existe
	 */
	public Plan plan(String cursoId) {
		Estado e = db.leer();
		Curso c = Soporte.existe(e.cursos().get(cursoId), "Curso");
		return new Plan(calculador.calcular(c, e.parametros()), calculador.capacidad(e.parametros()), c.demanda());
	}

	/**
	 * Abre los grupos de un curso.
	 *
	 * @param cursoId  identificador del curso
	 * @param cantidad número de grupos que se van a crear
	 * @return grupos creados
	 * @throws Errores.Negocio si la cantidad es menor al mínimo o el curso ya tiene grupos
	 */
	public List<Grupo> grupos(String cursoId, int cantidad) {
		return db.cambio(e -> {
			Curso c = Soporte.existe(e.cursos().get(cursoId), "Curso");
			int cap = calculador.capacidad(e.parametros());
			int min = calculador.calcular(c, e.parametros());
			if (cantidad < min || cantidad < 1)
				throw new Errores.Negocio("Cantidad menor al mínimo requerido");
			if (e.grupos().values().stream().anyMatch(g -> g.cursoId().equals(cursoId)))
				throw new Errores.Negocio("Ya existen grupos de este curso; no se reemplazan automáticamente");
			List<Grupo> creados = new ArrayList<>();
			for (int i = 1; i <= cantidad; i++) {
				Grupo g = new Grupo(Soporte.nuevoId(), cursoId, i, cap, 0, EstadoGrupo.ABIERTO);
				e.grupos().put(g.id(), g);
				creados.add(g);
			}
			return creados;
		});
	}

	/**
	 * Crea un nuevo borrador de horario con una sesión sin ubicar por cada sesión semanal de
	 * cada grupo abierto.
	 *
	 * @param semestre semestre académico
	 * @return borrador creado
	 * @throws Errores.Negocio si el semestre está vacío
	 */
	public Horario crearHorario(String semestre) {
		if (semestre == null || semestre.isBlank())
			throw new Errores.Negocio("Semestre obligatorio");
		return db.cambio(e -> {
			String sem = semestre.trim();
			int version = e.horarios().values().stream().filter(h -> h.semestre().equals(sem))
					.mapToInt(Horario::version).max().orElse(0) + 1;
			List<Sesion> sesiones = new ArrayList<>();
			for (Grupo g : e.grupos().values()) {
				if (g.estado() == EstadoGrupo.CERRADO)
					continue;
				Curso c = e.cursos().get(g.cursoId());
				for (int i = 1; i <= c.frecuencia(); i++)
					sesiones.add(new Sesion(Soporte.nuevoId(), g.id(), i, null, null, null, null));
			}
			Horario h = new Horario(Soporte.nuevoId(), sem, version, EstadoHorario.BORRADOR, sesiones);
			e.horarios().put(h.id(), h);
			return h;
		});
	}

	/**
	 * Propuesta de ubicación de una sesión.
	 *
	 * @param dia       día de la semana, de 1 a 6
	 * @param hora      hora de inicio
	 * @param docenteId identificador del docente
	 * @param aulaId    identificador del aula
	 */
	public record Movimiento(int dia, int hora, String docenteId, String aulaId) {
	}

	/**
	 * Resultado de asignar una sesión.
	 *
	 * @param sesion     sesión con su nueva ubicación
	 * @param validacion resultado de la validación, incluidas las advertencias
	 */
	public record Asignacion(Sesion sesion, Resultado validacion) {
	}

	/**
	 * Franja de la semana evaluada para una sesión.
	 *
	 * @param dia          día de la semana
	 * @param hora         hora de inicio
	 * @param valida       {@code true} si la sesión puede ubicarse en esta franja
	 * @param errores      reglas duras incumplidas
	 * @param advertencias reglas blandas incumplidas
	 */
	public record Celda(int dia, int hora, boolean valida, List<Hallazgo> errores, List<Hallazgo> advertencias) {
	}

	/**
	 * Construye el horario con una sesión movida a otra ubicación, sin guardarlo.
	 *
	 * @param h         horario original
	 * @param candidata sesión con la nueva ubicación
	 * @return copia del horario con la sesión reemplazada
	 */
	private static Horario conSesion(Horario h, Sesion candidata) {
		List<Sesion> lista = new ArrayList<>(h.sesiones());
		for (int i = 0; i < lista.size(); i++)
			if (lista.get(i).id().equals(candidata.id()))
				lista.set(i, candidata);
		return new Horario(h.id(), h.semestre(), h.version(), h.estado(), lista);
	}

	/**
	 * Busca una sesión dentro de un horario.
	 *
	 * @param h  horario
	 * @param id identificador de la sesión
	 * @return sesión encontrada
	 * @throws Errores.Negocio si la sesión no existe
	 */
	private static Sesion sesion(Horario h, String id) {
		return Soporte.existe(h.sesiones().stream().filter(s -> s.id().equals(id)).findFirst().orElse(null), "Sesión");
	}

	/**
	 * Obtiene un horario y verifica que siga en borrador.
	 *
	 * @param e  estado del sistema
	 * @param id identificador del horario
	 * @return horario en borrador
	 * @throws Errores.Negocio si no existe o ya está publicado
	 */
	static Horario borrador(Estado e, String id) {
		Horario h = Soporte.existe(e.horarios().get(id), "Horario");
		if (h.estado() != EstadoHorario.BORRADOR)
			throw new Errores.Negocio("Versión publicada inmutable");
		return h;
	}

	/**
	 * Valida una ubicación de sesión sin guardarla.
	 *
	 * @param horarioId identificador del horario
	 * @param sesionId  identificador de la sesión
	 * @param m         ubicación propuesta
	 * @return resultado con errores y advertencias
	 */
	public Resultado validar(String horarioId, String sesionId, Movimiento m) {
		return validar(db.leer(), horarioId, sesionId, m);
	}

	/**
	 * Valida una ubicación de sesión sobre un estado ya leído.
	 *
	 * @param e         estado del sistema
	 * @param horarioId identificador del horario
	 * @param sesionId  identificador de la sesión
	 * @param m         ubicación propuesta
	 * @return resultado con errores y advertencias
	 */
	private Resultado validar(Estado e, String horarioId, String sesionId, Movimiento m) {
		Horario h = Soporte.existe(e.horarios().get(horarioId), "Horario");
		Sesion s = sesion(h, sesionId);
		Sesion candidata = new Sesion(s.id(), s.grupoId(), s.indice(), m.dia(), m.hora(), m.docenteId(), m.aulaId());
		return validador.validar(e, conSesion(h, candidata), candidata);
	}

	/**
	 * Ubica una sesión en el horario si cumple todas las reglas duras.
	 *
	 * @param horarioId identificador del horario en borrador
	 * @param sesionId  identificador de la sesión
	 * @param m         ubicación propuesta
	 * @return sesión asignada y validación
	 * @throws Errores.Negocio si el horario está publicado o la ubicación incumple alguna regla
	 */
	public Asignacion asignar(String horarioId, String sesionId, Movimiento m) {
		return db.cambio(e -> {
			Horario h = borrador(e, horarioId);
			Sesion s = sesion(h, sesionId);
			Sesion candidata = new Sesion(s.id(), s.grupoId(), s.indice(), m.dia(), m.hora(), m.docenteId(),
					m.aulaId());
			Horario nuevo = conSesion(h, candidata);
			Resultado r = validador.validar(e, nuevo, candidata);
			if (!r.valido())
				throw new Errores.Negocio("Asignación rechazada: " + r.errores().stream()
						.map(x -> x.regla() + " " + x.mensaje()).reduce((a, b) -> a + "; " + b).orElse(""));
			e.horarios().put(h.id(), nuevo);
			return new Asignacion(candidata, r);
		});
	}

	/**
	 * Quita la ubicación de una sesión y la deja pendiente.
	 *
	 * @param horarioId identificador del horario en borrador
	 * @param sesionId  identificador de la sesión
	 * @return sesión sin ubicar
	 * @throws Errores.Negocio si el horario está publicado o la sesión no existe
	 */
	public Sesion retirar(String horarioId, String sesionId) {
		return db.cambio(e -> {
			Horario h = borrador(e, horarioId);
			Sesion s = sesion(h, sesionId);
			Sesion limpia = new Sesion(s.id(), s.grupoId(), s.indice(), null, null, null, null);
			e.horarios().put(h.id(), conSesion(h, limpia));
			return limpia;
		});
	}

	/**
	 * Evalúa todas las franjas de la semana para una sesión con un docente y un aula dados.
	 *
	 * @param horarioId identificador del horario
	 * @param sesionId  identificador de la sesión
	 * @param docenteId identificador del docente
	 * @param aulaId    identificador del aula
	 * @return franjas evaluadas de lunes a sábado
	 */
	public List<Celda> celdas(String horarioId, String sesionId, String docenteId, String aulaId) {
		Estado e = db.leer();
		Parametros p = e.parametros();
		List<Celda> celdas = new ArrayList<>();
		for (int dia = 1; dia <= 6; dia++) {
			int ini = dia == 6 ? p.inicioSabado() : p.inicioSemana();
			int fin = dia == 6 ? p.finSabado() : p.finSemana();
			for (int hora = ini; hora + p.duracion() <= fin; hora++) {
				Resultado r = validar(e, horarioId, sesionId, new Movimiento(dia, hora, docenteId, aulaId));
				celdas.add(new Celda(dia, hora, r.valido(), r.errores(), r.advertencias()));
			}
		}
		return celdas;
	}
}
