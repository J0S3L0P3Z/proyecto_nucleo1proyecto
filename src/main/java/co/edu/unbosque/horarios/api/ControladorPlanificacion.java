package co.edu.unbosque.horarios.api;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.*;
import co.edu.unbosque.horarios.service.reglas.Resultado;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Endpoints de planificación, exclusivos del rol DIRECTOR: grupos, borradores y asignación
 * asistida de sesiones. Solo coordinan; las reglas están en el validador.
 */
@RestController
@RequestMapping("/api/director")
public class ControladorPlanificacion {
	private final ServicioPlanificacion planificacion;

	/**
	 * Crea el controlador.
	 *
	 * @param planificacion servicio de planificación
	 */
	public ControladorPlanificacion(ServicioPlanificacion planificacion) {
		this.planificacion = planificacion;
	}

	/**
	 * Calcula el plan de grupos de un curso.
	 *
	 * @param id identificador del curso
	 * @return plan con grupos mínimos, capacidad y demanda
	 */
	@GetMapping("/cursos/{id}/plan")
	public ServicioPlanificacion.Plan plan(@PathVariable String id) {
		return planificacion.plan(id);
	}

	/**
	 * Abre los grupos de un curso.
	 *
	 * @param id       identificador del curso
	 * @param cantidad número de grupos
	 * @return grupos creados
	 */
	@PostMapping("/cursos/{id}/grupos")
	public List<Grupo> grupos(@PathVariable String id, @RequestParam int cantidad) {
		return planificacion.grupos(id, cantidad);
	}

	/**
	 * Crea un borrador de horario para un semestre.
	 *
	 * @param semestre semestre académico
	 * @return borrador creado
	 */
	@PostMapping("/horarios")
	public Horario horario(@RequestParam String semestre) {
		return planificacion.crearHorario(semestre);
	}

	/**
	 * Valida una ubicación de sesión sin guardarla.
	 *
	 * @param id     identificador del horario
	 * @param sesion identificador de la sesión
	 * @param m      ubicación propuesta
	 * @return resultado de la validación
	 */
	@PostMapping("/horarios/{id}/sesiones/{sesion}/validar")
	public Resultado validar(@PathVariable String id, @PathVariable String sesion,
			@RequestBody ServicioPlanificacion.Movimiento m) {
		return planificacion.validar(id, sesion, m);
	}

	/**
	 * Ubica una sesión en el horario.
	 *
	 * @param id     identificador del horario
	 * @param sesion identificador de la sesión
	 * @param m      ubicación
	 * @return sesión asignada y validación
	 */
	@PutMapping("/horarios/{id}/sesiones/{sesion}")
	public ServicioPlanificacion.Asignacion asignar(@PathVariable String id, @PathVariable String sesion,
			@RequestBody ServicioPlanificacion.Movimiento m) {
		return planificacion.asignar(id, sesion, m);
	}

	/**
	 * Quita la ubicación de una sesión.
	 *
	 * @param id     identificador del horario
	 * @param sesion identificador de la sesión
	 * @return sesión pendiente
	 */
	@DeleteMapping("/horarios/{id}/sesiones/{sesion}")
	public Sesion retirar(@PathVariable String id, @PathVariable String sesion) {
		return planificacion.retirar(id, sesion);
	}

	/**
	 * Evalúa todas las franjas de la semana para una sesión.
	 *
	 * @param id        identificador del horario
	 * @param sesion    identificador de la sesión
	 * @param docenteId identificador del docente
	 * @param aulaId    identificador del aula
	 * @return franjas evaluadas
	 */
	@GetMapping("/horarios/{id}/sesiones/{sesion}/celdas")
	public List<ServicioPlanificacion.Celda> celdas(@PathVariable String id, @PathVariable String sesion,
			@RequestParam String docenteId, @RequestParam String aulaId) {
		return planificacion.celdas(id, sesion, docenteId, aulaId);
	}
}
