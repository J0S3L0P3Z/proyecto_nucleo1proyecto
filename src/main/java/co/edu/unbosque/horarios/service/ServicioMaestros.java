package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Administración de los datos maestros: parámetros, docentes, cursos, aulas, estudiantes y usuarios.
 */
@Service
public class ServicioMaestros {
	private final AlmacenJson db;
	private final PasswordEncoder encoder;

	/**
	 * Crea el servicio.
	 *
	 * @param db      almacenamiento del estado
	 * @param encoder cifrador de contraseñas para los usuarios nuevos
	 */
	public ServicioMaestros(AlmacenJson db, PasswordEncoder encoder) {
		this.db = db;
		this.encoder = encoder;
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
	 * Busca el usuario autenticado por su nombre.
	 *
	 * @param username nombre de usuario
	 * @return usuario encontrado
	 * @throws Errores.Negocio si no existe
	 */
	public Usuario usuario(String username) {
		return Soporte.existe(estado().usuarios().values().stream().filter(u -> u.username().equals(username))
				.findFirst().orElse(null), "Usuario");
	}

	/**
	 * Valida y reemplaza los parámetros generales.
	 *
	 * @param p nuevos parámetros
	 * @return parámetros guardados
	 * @throws Errores.Negocio si algún valor es inválido o los rangos de horas son incoherentes
	 */
	public Parametros parametros(Parametros p) {
		if (p.duracion() < 1 || p.cupoMax() < 1 || p.frecuenciaMax() < 1 || p.tolerancia() < 0
				|| p.inicioSemana() >= p.finSemana() || p.inicioSabado() >= p.finSabado()
				|| p.descansoInicio() >= p.descansoFin() || p.minimoInscritos() < 0 || p.huecoLargo() < 0
				|| p.horasMaximas() == null)
			throw new Errores.Negocio("Parámetros inválidos");
		return db.transformar(e -> new AlmacenJson.Cambio<>(e.conParametros(p), p));
	}

	/**
	 * Crea o actualiza un docente. Si no trae identificador, se genera uno nuevo.
	 *
	 * @param d datos del docente
	 * @return docente guardado
	 * @throws Errores.Negocio si faltan el nombre, la vinculación o los cursos habilitados
	 */
	public Docente docente(Docente d) {
		if (d.nombre() == null || d.nombre().isBlank() || d.vinculacion() == null || d.cursosHabilitados() == null)
			throw new Errores.Negocio("Docente incompleto");
		return db.cambio(e -> {
			String id = Soporte.id(d.id());
			Docente x = new Docente(id, d.nombre().trim(), d.vinculacion(),
					d.restriccion() == null ? Restriccion.NINGUNA : d.restriccion(), Set.copyOf(d.cursosHabilitados()));
			e.docentes().put(id, x);
			return x;
		});
	}

	/**
	 * Actualiza un docente existente.
	 *
	 * @param id identificador del docente
	 * @param d  nuevos datos
	 * @return docente actualizado
	 * @throws Errores.Negocio si el docente no existe o los datos son inválidos
	 */
	public Docente actualizarDocente(String id, Docente d) {
		Soporte.existe(estado().docentes().get(id), "Docente");
		return docente(new Docente(id, d.nombre(), d.vinculacion(), d.restriccion(), d.cursosHabilitados()));
	}

	/**
	 * Crea o actualiza un curso. Si no trae identificador, se genera uno nuevo.
	 *
	 * @param c datos del curso
	 * @return curso guardado
	 * @throws Errores.Negocio si el código está vacío, la frecuencia supera el máximo o los valores son negativos
	 */
	public Curso curso(Curso c) {
		return db.cambio(e -> {
			if (c.codigo() == null || c.codigo().isBlank() || c.frecuencia() < 1
					|| c.frecuencia() > e.parametros().frecuenciaMax() || c.demanda() < 0 || c.semestre() < 1)
				throw new Errores.Negocio("Curso inválido");
			String id = Soporte.id(c.id());
			Curso x = new Curso(id, c.codigo().trim(), c.nombre(), c.semestre(), c.frecuencia(), c.tipoAula(),
					c.demanda());
			e.cursos().put(id, x);
			return x;
		});
	}

	/**
	 * Actualiza un curso existente.
	 *
	 * @param id identificador del curso
	 * @param c  nuevos datos
	 * @return curso actualizado
	 * @throws Errores.Negocio si el curso no existe o los datos son inválidos
	 */
	public Curso actualizarCurso(String id, Curso c) {
		Soporte.existe(estado().cursos().get(id), "Curso");
		return curso(new Curso(id, c.codigo(), c.nombre(), c.semestre(), c.frecuencia(), c.tipoAula(), c.demanda()));
	}

	/**
	 * Crea o actualiza un aula. Si no trae identificador, se genera uno nuevo.
	 *
	 * @param a datos del aula
	 * @return aula guardada
	 * @throws Errores.Negocio si el código está vacío, la capacidad es menor que 1 o faltan las características
	 */
	public Aula aula(Aula a) {
		if (a.codigo() == null || a.codigo().isBlank() || a.capacidad() < 1 || a.caracteristicas() == null)
			throw new Errores.Negocio("Aula inválida");
		return db.cambio(e -> {
			String id = Soporte.id(a.id());
			Aula x = new Aula(id, a.codigo().trim(), a.capacidad(), Set.copyOf(a.caracteristicas()));
			e.aulas().put(id, x);
			return x;
		});
	}

	/**
	 * Actualiza un aula existente.
	 *
	 * @param id identificador del aula
	 * @param a  nuevos datos
	 * @return aula actualizada
	 * @throws Errores.Negocio si el aula no existe o los datos son inválidos
	 */
	public Aula actualizarAula(String id, Aula a) {
		Soporte.existe(estado().aulas().get(id), "Aula");
		return aula(new Aula(id, a.codigo(), a.capacidad(), a.caracteristicas()));
	}

	/**
	 * Crea o actualiza un estudiante. Si no trae identificador, se genera uno nuevo.
	 *
	 * @param est datos del estudiante
	 * @return estudiante guardado
	 * @throws Errores.Negocio si faltan el código o el nombre, o el código ya está registrado
	 */
	public Estudiante estudiante(Estudiante est) {
		if (est.codigo() == null || est.codigo().isBlank() || est.nombre() == null || est.nombre().isBlank())
			throw new Errores.Negocio("Estudiante incompleto: código y nombre son obligatorios");
		return db.cambio(e -> {
			String id = Soporte.id(est.id());
			String codigo = est.codigo().trim();
			if (e.estudiantes().values().stream().anyMatch(x -> x.codigo().equals(codigo) && !x.id().equals(id)))
				throw new Errores.Negocio("Ya existe un estudiante con el código " + codigo);
			Estudiante x = new Estudiante(id, codigo, est.nombre().trim());
			e.estudiantes().put(id, x);
			return x;
		});
	}

	/**
	 * Actualiza un estudiante existente.
	 *
	 * @param id  identificador del estudiante
	 * @param est nuevos datos
	 * @return estudiante actualizado
	 * @throws Errores.Negocio si el estudiante no existe o los datos son inválidos
	 */
	public Estudiante actualizarEstudiante(String id, Estudiante est) {
		Soporte.existe(estado().estudiantes().get(id), "Estudiante");
		return estudiante(new Estudiante(id, est.codigo(), est.nombre()));
	}

	/**
	 * Elimina un elemento de un catálogo si no está en uso.
	 *
	 * @param tipo catálogo: {@code docentes}, {@code cursos}, {@code aulas} o {@code estudiantes}
	 * @param id   identificador del elemento
	 * @throws Errores.Negocio si el elemento está en uso o el catálogo no existe
	 */
	public void eliminar(String tipo, String id) {
		db.cambio(e -> {
			switch (tipo) {
			case "docentes" -> {
				if (e.horarios().values().stream().flatMap(h -> h.sesiones().stream())
						.anyMatch(s -> id.equals(s.docenteId())))
					throw new Errores.Negocio("Docente utilizado en sesiones");
				e.docentes().remove(id);
			}
			case "cursos" -> {
				if (e.grupos().values().stream().anyMatch(g -> id.equals(g.cursoId())))
					throw new Errores.Negocio("Curso con grupos");
				e.cursos().remove(id);
			}
			case "aulas" -> {
				if (e.horarios().values().stream().flatMap(h -> h.sesiones().stream())
						.anyMatch(s -> id.equals(s.aulaId())))
					throw new Errores.Negocio("Aula utilizada en sesiones");
				e.aulas().remove(id);
			}
			case "estudiantes" -> {
				if (e.inscripciones().values().stream().anyMatch(i -> id.equals(i.estudianteId())))
					throw new Errores.Negocio("Estudiante con inscripciones");
				e.estudiantes().remove(id);
			}
			default -> throw new Errores.Negocio("Catálogo desconocido");
			}
			return true;
		});
	}

	/**
	 * Datos para crear un usuario.
	 *
	 * @param username     nombre de usuario
	 * @param password     contraseña en texto plano (mínimo 12 caracteres)
	 * @param rol          rol del usuario
	 * @param docenteId    docente vinculado, obligatorio si el rol es DOCENTE
	 * @param estudianteId estudiante vinculado, obligatorio si el rol es ESTUDIANTE
	 */
	public record CrearUsuario(String username, String password, Rol rol, String docenteId, String estudianteId) {
	}

	/**
	 * Crea un usuario.
	 *
	 * @param req datos del usuario
	 * @return identificador y nombre del usuario creado
	 * @throws Errores.Negocio si faltan datos, la contraseña es corta, el nombre está repetido
	 *                         o el docente o estudiante vinculado no existe
	 */
	public Map<String, String> crearUsuario(CrearUsuario req) {
		if (req.username() == null || req.username().isBlank() || req.password() == null || req.password().length() < 12
				|| req.rol() == null)
			throw new Errores.Negocio("Usuario, contraseña (mínimo 12 caracteres) y rol obligatorios");
		return db.cambio(e -> {
			String username = req.username().trim();
			if (e.usuarios().values().stream().anyMatch(u -> u.username().equals(username)))
				throw new Errores.Negocio("Nombre de usuario repetido");
			if (req.rol() == Rol.DOCENTE)
				Soporte.existe(e.docentes().get(req.docenteId()), "Docente vinculado");
			if (req.rol() == Rol.ESTUDIANTE)
				Soporte.existe(e.estudiantes().get(req.estudianteId()), "Estudiante vinculado");
			String id = Soporte.nuevoId();
			e.usuarios().put(id,
					new Usuario(id, username, encoder.encode(req.password()), req.rol(),
							req.rol() == Rol.DOCENTE ? req.docenteId() : null,
							req.rol() == Rol.ESTUDIANTE ? req.estudianteId() : null));
			return Map.of("id", id, "username", username);
		});
	}
}
