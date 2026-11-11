package co.edu.unbosque.horarios.api;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Endpoints de publicación, versiones, carga docente y consulta de horarios.
 */
@RestController
public class ControladorHorario {
	private final ServicioHorario horarios;
	private final ServicioMaestros maestros;

	/**
	 * Crea el controlador.
	 *
	 * @param horarios servicio de horarios
	 * @param maestros servicio de datos maestros, para identificar al usuario
	 */
	public ControladorHorario(ServicioHorario horarios, ServicioMaestros maestros) {
		this.horarios = horarios;
		this.maestros = maestros;
	}

	/**
	 * Publica un borrador.
	 *
	 * @param id   identificador del borrador
	 * @param auth autenticación del director
	 * @return horario publicado
	 */
	@PostMapping("/api/director/horarios/{id}/publicar")
	public Horario publicar(@PathVariable String id, Authentication auth) {
		return horarios.publicar(id, auth.getName());
	}

	/**
	 * Crea una nueva versión en borrador a partir de un horario publicado.
	 *
	 * @param id identificador del horario publicado
	 * @return nueva versión
	 */
	@PostMapping("/api/director/horarios/{id}/nueva-version")
	public Horario nuevaVersion(@PathVariable String id) {
		return horarios.nuevaVersion(id);
	}

	/**
	 * Consulta la carga horaria de los docentes en un horario.
	 *
	 * @param id identificador del horario
	 * @return carga de cada docente
	 */
	@GetMapping("/api/director/horarios/{id}/carga")
	public List<ServicioHorario.Carga> carga(@PathVariable String id) {
		return horarios.cargas(id);
	}

	/**
	 * Lista los horarios publicados.
	 *
	 * @return identificador, semestre y versión de cada horario publicado
	 */
	@GetMapping("/api/publico/horarios")
	public List<Map<String, Object>> publicados() {
		return horarios.publicados().stream()
				.map(h -> Map.<String, Object>of("id", h.id(), "semestre", h.semestre(), "version", h.version()))
				.toList();
	}

	/**
	 * Devuelve los nombres de cursos, grupos, aulas y docentes, y la duración de las sesiones,
	 * para mostrar los horarios de forma legible.
	 *
	 * @return catálogo público
	 */
	@GetMapping("/api/publico/catalogo")
	public Map<String, Object> catalogo() {
		Estado e = horarios.estado();
		Map<String, Object> datos = new LinkedHashMap<>();
		datos.put("cursos", e.cursos().values().stream().map(c -> Map.<String, Object>of("id", c.id(), "codigo",
				c.codigo(), "nombre", c.nombre() == null ? "" : c.nombre())).toList());
		datos.put("grupos", e.grupos().values().stream().filter(g -> g.estado() == EstadoGrupo.ABIERTO)
				.map(g -> Map.<String, Object>of("id", g.id(), "cursoId", g.cursoId(), "numero", g.numero())).toList());
		datos.put("aulas",
				e.aulas().values().stream().map(a -> Map.<String, Object>of("id", a.id(), "codigo", a.codigo())).toList());
		datos.put("docentes", e.docentes().values().stream()
				.map(d -> Map.<String, Object>of("id", d.id(), "nombre", d.nombre())).toList());
		datos.put("duracion", e.parametros().duracion());
		return datos;
	}

	/**
	 * Consulta pública de un horario publicado.
	 *
	 * @param id     identificador del horario
	 * @param tipo   filtro: {@code todos}, {@code curso} o {@code grupo}
	 * @param filtro identificador del curso o grupo
	 * @return sesiones que cumplen el filtro
	 * @throws Errores.Negocio si el filtro no está permitido o el horario no está publicado
	 */
	@GetMapping("/api/publico/horarios/{id}")
	public List<Sesion> publico(@PathVariable String id, @RequestParam(defaultValue = "todos") String tipo,
			@RequestParam(defaultValue = "") String filtro) {
		if (!Set.of("todos", "curso", "grupo").contains(tipo))
			throw new Errores.Negocio("Filtro público no permitido");
		return horarios.consultar(id, tipo, filtro, true);
	}

	/**
	 * Devuelve los datos del usuario autenticado. El frontend lo usa al iniciar sesión.
	 *
	 * @param auth autenticación del usuario actual
	 * @return nombre de usuario, rol, docente y estudiante vinculados
	 */
	@GetMapping("/api/consulta/yo")
	public Map<String, Object> yo(Authentication auth) {
		Usuario u = maestros.usuario(auth.getName());
		Map<String, Object> datos = new LinkedHashMap<>();
		datos.put("username", u.username());
		datos.put("rol", u.rol());
		datos.put("docenteId", u.docenteId());
		datos.put("estudianteId", u.estudianteId());
		return datos;
	}

	/**
	 * Consulta de un horario según el rol del usuario.
	 * <p>
	 * Los estudiantes consultan por curso o grupo, los docentes solo su propio horario, el
	 * Comité cualquier filtro sobre horarios publicados y el director cualquier filtro,
	 * incluidos los borradores.
	 * </p>
	 *
	 * @param id     identificador del horario
	 * @param tipo   filtro de la consulta
	 * @param filtro identificador del elemento filtrado
	 * @param auth   autenticación del usuario actual
	 * @return sesiones que cumplen el filtro
	 * @throws Errores.Negocio si el rol no tiene permitida esa consulta
	 */
	@GetMapping("/api/consulta/horarios/{id}")
	public List<Sesion> consulta(@PathVariable String id, @RequestParam String tipo, @RequestParam String filtro,
			Authentication auth) {
		Usuario u = maestros.usuario(auth.getName());
		if (u.rol() == Rol.ESTUDIANTE && !Set.of("curso", "grupo").contains(tipo))
			throw new Errores.Negocio("Consulta no permitida");
		if (u.rol() == Rol.DOCENTE && (!tipo.equals("docente") || !Objects.equals(filtro, u.docenteId())))
			throw new Errores.Negocio("Solo puede consultar su horario");
		return horarios.consultar(id, tipo, filtro, u.rol() != Rol.DIRECTOR);
	}
}
