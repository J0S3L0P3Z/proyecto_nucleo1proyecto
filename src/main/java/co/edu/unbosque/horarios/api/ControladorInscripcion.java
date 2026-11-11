package co.edu.unbosque.horarios.api;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Endpoints de inscripción: los del estudiante sobre sus propios grupos y los del director
 * para inscribir, consultar inscritos y abrir o cerrar el periodo.
 */
@RestController
public class ControladorInscripcion {
	private final ServicioInscripcion inscripcion;
	private final ServicioMaestros maestros;

	/**
	 * Crea el controlador.
	 *
	 * @param inscripcion servicio de inscripción
	 * @param maestros    servicio de datos maestros, para identificar al usuario
	 */
	public ControladorInscripcion(ServicioInscripcion inscripcion, ServicioMaestros maestros) {
		this.inscripcion = inscripcion;
		this.maestros = maestros;
	}

	/**
	 * Identificador del estudiante vinculado al usuario autenticado.
	 *
	 * @param auth autenticación del usuario actual
	 * @return identificador del estudiante
	 * @throws Errores.Negocio si el usuario no es un estudiante vinculado
	 */
	private String estudiante(Authentication auth) {
		Usuario u = maestros.usuario(auth.getName());
		if (u.rol() != Rol.ESTUDIANTE || u.estudianteId() == null)
			throw new Errores.Negocio("Esta opción es solo para estudiantes");
		return u.estudianteId();
	}

	/**
	 * Oferta de grupos del horario vigente. Para un estudiante, marca los grupos en que está inscrito.
	 *
	 * @param auth autenticación del usuario actual
	 * @return oferta de grupos
	 */
	@GetMapping("/api/inscripcion/oferta")
	public ServicioInscripcion.Oferta oferta(Authentication auth) {
		Usuario u = maestros.usuario(auth.getName());
		return inscripcion.oferta(u.rol() == Rol.ESTUDIANTE ? u.estudianteId() : null);
	}

	/**
	 * Grupos en los que está inscrito el estudiante autenticado.
	 *
	 * @param auth autenticación del estudiante
	 * @return grupos del estudiante
	 */
	@GetMapping("/api/inscripcion/mis-grupos")
	public List<ServicioInscripcion.GrupoOferta> misGrupos(Authentication auth) {
		return inscripcion.gruposDelEstudiante(estudiante(auth));
	}

	/**
	 * Inscribe al estudiante autenticado en un grupo.
	 *
	 * @param grupoId identificador del grupo
	 * @param auth    autenticación del estudiante
	 * @return inscripción creada
	 */
	@PostMapping("/api/inscripcion/mis-grupos/{grupoId}")
	public Inscripcion inscribirme(@PathVariable String grupoId, Authentication auth) {
		return inscripcion.inscribir(estudiante(auth), grupoId, auth.getName());
	}

	/**
	 * Cancela la inscripción del estudiante autenticado en un grupo.
	 *
	 * @param grupoId identificador del grupo
	 * @param auth    autenticación del estudiante
	 */
	@DeleteMapping("/api/inscripcion/mis-grupos/{grupoId}")
	public void salirme(@PathVariable String grupoId, Authentication auth) {
		inscripcion.cancelar(estudiante(auth), grupoId, auth.getName());
	}

	/**
	 * Datos para que el director inscriba a un estudiante.
	 *
	 * @param estudianteId identificador del estudiante
	 * @param grupoId      identificador del grupo
	 */
	public record SolicitudInscripcion(String estudianteId, String grupoId) {
	}

	/**
	 * Inscribe a un estudiante en un grupo, en nombre del director.
	 *
	 * @param req  estudiante y grupo
	 * @param auth autenticación del director
	 * @return inscripción creada
	 */
	@PostMapping("/api/director/inscripciones")
	public Inscripcion inscribir(@RequestBody SolicitudInscripcion req, Authentication auth) {
		return inscripcion.inscribir(req.estudianteId(), req.grupoId(), auth.getName());
	}

	/**
	 * Cancela la inscripción de un estudiante, en nombre del director.
	 *
	 * @param estudianteId identificador del estudiante
	 * @param grupoId      identificador del grupo
	 * @param auth         autenticación del director
	 */
	@DeleteMapping("/api/director/inscripciones")
	public void cancelar(@RequestParam String estudianteId, @RequestParam String grupoId, Authentication auth) {
		inscripcion.cancelar(estudianteId, grupoId, auth.getName());
	}

	/**
	 * Estudiantes inscritos en un grupo.
	 *
	 * @param id identificador del grupo
	 * @return inscritos
	 */
	@GetMapping("/api/director/grupos/{id}/inscritos")
	public List<ServicioInscripcion.Inscrito> inscritos(@PathVariable String id) {
		return inscripcion.inscritos(id);
	}

	/**
	 * Cierra el periodo de inscripción y genera las propuestas de reasignación.
	 *
	 * @param auth autenticación del director
	 * @return resumen del cierre
	 */
	@PostMapping("/api/director/inscripciones/cerrar")
	public ServicioInscripcion.ResumenCierre cerrar(Authentication auth) {
		return inscripcion.cerrar(auth.getName());
	}

	/**
	 * Reabre el periodo de inscripción.
	 *
	 * @param auth autenticación del director
	 * @return estado del periodo
	 */
	@PostMapping("/api/director/inscripciones/abrir")
	public EstadoInscripcion abrir(Authentication auth) {
		return inscripcion.abrir(auth.getName());
	}
}
