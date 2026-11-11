package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Funciones de apoyo compartidas por los servicios.
 */
public final class Soporte {
	private static final ZoneId ZONA = ZoneId.of("America/Bogota");
	private static final String[] DIAS = { "", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado" };

	/**
	 * Constructor privado: clase de utilidades.
	 */
	private Soporte() {
	}

	/**
	 * Devuelve el identificador recibido o genera uno nuevo si viene vacío.
	 *
	 * @param s identificador recibido
	 * @return identificador para usar
	 */
	public static String id(String s) {
		return s == null || s.isBlank() ? nuevoId() : s;
	}

	/**
	 * Genera un identificador único.
	 *
	 * @return identificador aleatorio
	 */
	public static String nuevoId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Verifica que un objeto exista.
	 *
	 * @param <T>    tipo del objeto
	 * @param o      objeto que se va a verificar
	 * @param nombre nombre del elemento para el mensaje de error
	 * @return el mismo objeto si no es {@code null}
	 * @throws Errores.Negocio si el objeto es {@code null}
	 */
	public static <T> T existe(T o, String nombre) {
		if (o == null)
			throw new Errores.Negocio(nombre + " inexistente");
		return o;
	}

	/**
	 * Fecha y hora actual en Bogotá, sin fracciones de segundo.
	 *
	 * @return fecha y hora en formato ISO
	 */
	public static String ahora() {
		return LocalDateTime.now(ZONA).withNano(0).toString();
	}

	/**
	 * Nombre del día de la semana.
	 *
	 * @param dia número del día, de 1 (lunes) a 6 (sábado)
	 * @return nombre del día
	 */
	public static String dia(int dia) {
		return dia >= 1 && dia <= 6 ? DIAS[dia] : "Día " + dia;
	}

	/**
	 * Nombre corto de un grupo, formado por el código del curso y el número del grupo.
	 *
	 * @param e       estado del sistema
	 * @param grupoId identificador del grupo
	 * @return nombre del grupo, por ejemplo {@code 12994 G1}
	 */
	public static String etiquetaGrupo(Estado e, String grupoId) {
		Grupo g = e.grupos().get(grupoId);
		if (g == null)
			return "grupo desconocido";
		Curso c = e.cursos().get(g.cursoId());
		return (c == null ? "Curso" : c.codigo()) + " G" + g.numero();
	}

	/**
	 * Sesiones ya ubicadas de un grupo dentro de un horario.
	 *
	 * @param h       horario
	 * @param grupoId identificador del grupo
	 * @return sesiones con día y hora asignados
	 */
	public static List<Sesion> sesionesUbicadas(Horario h, String grupoId) {
		return h.sesiones().stream().filter(s -> s.grupoId().equals(grupoId) && s.dia() != null && s.hora() != null)
				.toList();
	}

	/**
	 * Indica si dos conjuntos de sesiones se cruzan en algún día y hora.
	 *
	 * @param a        primer conjunto de sesiones
	 * @param b        segundo conjunto de sesiones
	 * @param duracion duración de cada sesión en horas
	 * @return {@code true} si al menos una sesión de {@code a} se cruza con una de {@code b}
	 */
	public static boolean seCruzan(List<Sesion> a, List<Sesion> b, int duracion) {
		for (Sesion x : a)
			for (Sesion y : b)
				if (x.dia().equals(y.dia()) && x.hora() < y.hora() + duracion && y.hora() < x.hora() + duracion)
					return true;
		return false;
	}
}
