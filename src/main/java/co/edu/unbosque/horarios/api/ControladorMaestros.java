package co.edu.unbosque.horarios.api;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Endpoints de datos maestros, exclusivos del rol DIRECTOR: parámetros, docentes, cursos,
 * aulas, estudiantes y usuarios.
 */
@RestController
@RequestMapping("/api/director")
public class ControladorMaestros {
	private final ServicioMaestros maestros;

	/**
	 * Crea el controlador.
	 *
	 * @param maestros servicio de datos maestros
	 */
	public ControladorMaestros(ServicioMaestros maestros) {
		this.maestros = maestros;
	}

	/**
	 * Devuelve los catálogos, grupos, horarios, parámetros y el estado de la inscripción.
	 *
	 * @return estado del sistema sin usuarios, inscripciones ni auditoría
	 */
	@GetMapping("/estado")
	public Map<String, Object> estado() {
		Estado e = maestros.estado();
		Map<String, Object> datos = new LinkedHashMap<>();
		datos.put("docentes", e.docentes().values());
		datos.put("cursos", e.cursos().values());
		datos.put("aulas", e.aulas().values());
		datos.put("grupos", e.grupos().values());
		datos.put("horarios", e.horarios().values());
		datos.put("parametros", e.parametros());
		datos.put("estudiantes", e.estudiantes().values());
		datos.put("inscripcion", e.inscripcion());
		return datos;
	}

	/**
	 * Consulta los parámetros generales.
	 *
	 * @return parámetros actuales
	 */
	@GetMapping("/parametros")
	public Parametros parametros() {
		return maestros.estado().parametros();
	}

	/**
	 * Reemplaza los parámetros generales.
	 *
	 * @param p nuevos parámetros
	 * @return parámetros guardados
	 */
	@PutMapping("/parametros")
	public Parametros parametros(@RequestBody Parametros p) {
		return maestros.parametros(p);
	}

	/**
	 * Lista los docentes.
	 *
	 * @return docentes registrados
	 */
	@GetMapping("/docentes")
	public Collection<Docente> docentes() {
		return maestros.estado().docentes().values();
	}

	/**
	 * Crea un docente.
	 *
	 * @param d datos del docente
	 * @return docente creado
	 */
	@PostMapping("/docentes")
	public Docente docente(@RequestBody Docente d) {
		return maestros.docente(d);
	}

	/**
	 * Actualiza un docente.
	 *
	 * @param id identificador del docente
	 * @param d  nuevos datos
	 * @return docente actualizado
	 */
	@PutMapping("/docentes/{id}")
	public Docente docente(@PathVariable String id, @RequestBody Docente d) {
		return maestros.actualizarDocente(id, d);
	}

	/**
	 * Elimina un docente que no esté asignado a sesiones.
	 *
	 * @param id identificador del docente
	 */
	@DeleteMapping("/docentes/{id}")
	public void borrarDocente(@PathVariable String id) {
		maestros.eliminar("docentes", id);
	}

	/**
	 * Lista los cursos.
	 *
	 * @return cursos registrados
	 */
	@GetMapping("/cursos")
	public Collection<Curso> cursos() {
		return maestros.estado().cursos().values();
	}

	/**
	 * Crea un curso.
	 *
	 * @param c datos del curso
	 * @return curso creado
	 */
	@PostMapping("/cursos")
	public Curso curso(@RequestBody Curso c) {
		return maestros.curso(c);
	}

	/**
	 * Actualiza un curso.
	 *
	 * @param id identificador del curso
	 * @param c  nuevos datos
	 * @return curso actualizado
	 */
	@PutMapping("/cursos/{id}")
	public Curso curso(@PathVariable String id, @RequestBody Curso c) {
		return maestros.actualizarCurso(id, c);
	}

	/**
	 * Elimina un curso que no tenga grupos.
	 *
	 * @param id identificador del curso
	 */
	@DeleteMapping("/cursos/{id}")
	public void borrarCurso(@PathVariable String id) {
		maestros.eliminar("cursos", id);
	}

	/**
	 * Lista las aulas.
	 *
	 * @return aulas registradas
	 */
	@GetMapping("/aulas")
	public Collection<Aula> aulas() {
		return maestros.estado().aulas().values();
	}

	/**
	 * Crea un aula.
	 *
	 * @param a datos del aula
	 * @return aula creada
	 */
	@PostMapping("/aulas")
	public Aula aula(@RequestBody Aula a) {
		return maestros.aula(a);
	}

	/**
	 * Actualiza un aula.
	 *
	 * @param id identificador del aula
	 * @param a  nuevos datos
	 * @return aula actualizada
	 */
	@PutMapping("/aulas/{id}")
	public Aula aula(@PathVariable String id, @RequestBody Aula a) {
		return maestros.actualizarAula(id, a);
	}

	/**
	 * Elimina un aula que no esté asignada a sesiones.
	 *
	 * @param id identificador del aula
	 */
	@DeleteMapping("/aulas/{id}")
	public void borrarAula(@PathVariable String id) {
		maestros.eliminar("aulas", id);
	}

	/**
	 * Lista los estudiantes.
	 *
	 * @return estudiantes registrados
	 */
	@GetMapping("/estudiantes")
	public Collection<Estudiante> estudiantes() {
		return maestros.estado().estudiantes().values();
	}

	/**
	 * Crea un estudiante.
	 *
	 * @param est datos del estudiante
	 * @return estudiante creado
	 */
	@PostMapping("/estudiantes")
	public Estudiante estudiante(@RequestBody Estudiante est) {
		return maestros.estudiante(est);
	}

	/**
	 * Actualiza un estudiante.
	 *
	 * @param id  identificador del estudiante
	 * @param est nuevos datos
	 * @return estudiante actualizado
	 */
	@PutMapping("/estudiantes/{id}")
	public Estudiante estudiante(@PathVariable String id, @RequestBody Estudiante est) {
		return maestros.actualizarEstudiante(id, est);
	}

	/**
	 * Elimina un estudiante sin inscripciones.
	 *
	 * @param id identificador del estudiante
	 */
	@DeleteMapping("/estudiantes/{id}")
	public void borrarEstudiante(@PathVariable String id) {
		maestros.eliminar("estudiantes", id);
	}

	/**
	 * Crea un usuario.
	 *
	 * @param req datos del usuario
	 * @return identificador y nombre del usuario creado
	 */
	@PostMapping("/usuarios")
	public Map<String, String> usuario(@RequestBody ServicioMaestros.CrearUsuario req) {
		return maestros.crearUsuario(req);
	}
}
