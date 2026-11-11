package co.edu.unbosque.horarios.api;

import co.edu.unbosque.horarios.model.Domain.*;
import co.edu.unbosque.horarios.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Endpoints del Comité Táctico (también disponibles para el director): panel de control,
 * decisión de reasignaciones, cierre de grupos y auditoría.
 */
@RestController
@RequestMapping("/api/comite")
public class ControladorComite {
	private final ServicioInscripcion inscripcion;
	private final ServicioAuditoria auditoria;

	/**
	 * Crea el controlador.
	 *
	 * @param inscripcion servicio de inscripción
	 * @param auditoria   registro de auditoría
	 */
	public ControladorComite(ServicioInscripcion inscripcion, ServicioAuditoria auditoria) {
		this.inscripcion = inscripcion;
		this.auditoria = auditoria;
	}

	/**
	 * Indicadores del panel de control.
	 *
	 * @return panel
	 */
	@GetMapping("/panel")
	public ServicioInscripcion.Panel panel() {
		return inscripcion.panel();
	}

	/**
	 * Lista las propuestas de reasignación.
	 *
	 * @param estado filtro opcional por estado
	 * @return propuestas
	 */
	@GetMapping("/reasignaciones")
	public List<ServicioInscripcion.DetalleReasignacion> reasignaciones(
			@RequestParam(required = false) EstadoReasignacion estado) {
		return inscripcion.reasignaciones(estado);
	}

	/**
	 * Aprueba una propuesta.
	 *
	 * @param id   identificador de la propuesta
	 * @param auth autenticación del miembro del Comité
	 * @return propuesta aprobada
	 */
	@PostMapping("/reasignaciones/{id}/aprobar")
	public Reasignacion aprobar(@PathVariable String id, Authentication auth) {
		return inscripcion.aprobar(id, auth.getName());
	}

	/**
	 * Rechaza una propuesta.
	 *
	 * @param id     identificador de la propuesta
	 * @param motivo motivo opcional del rechazo
	 * @param auth   autenticación del miembro del Comité
	 * @return propuesta rechazada
	 */
	@PostMapping("/reasignaciones/{id}/rechazar")
	public Reasignacion rechazar(@PathVariable String id, @RequestParam(required = false) String motivo,
			Authentication auth) {
		return inscripcion.rechazar(id, motivo, auth.getName());
	}

	/**
	 * Aprueba todas las propuestas pendientes que sigan siendo válidas.
	 *
	 * @param auth autenticación del miembro del Comité
	 * @return resumen de la aprobación
	 */
	@PostMapping("/reasignaciones/aprobar-todas")
	public ServicioInscripcion.ResumenAprobacion aprobarTodas(Authentication auth) {
		return inscripcion.aprobarTodas(auth.getName());
	}

	/**
	 * Cierra un grupo por debajo del mínimo.
	 *
	 * @param id   identificador del grupo
	 * @param auth autenticación del miembro del Comité
	 * @return grupo cerrado
	 */
	@PostMapping("/grupos/{id}/cerrar")
	public Grupo cerrarGrupo(@PathVariable String id, Authentication auth) {
		return inscripcion.cerrarGrupo(id, auth.getName());
	}

	/**
	 * Registro de auditoría completo, del más reciente al más antiguo.
	 *
	 * @return registros de auditoría
	 */
	@GetMapping("/auditoria")
	public List<RegistroAuditoria> auditoria() {
		return auditoria.listar();
	}
}
