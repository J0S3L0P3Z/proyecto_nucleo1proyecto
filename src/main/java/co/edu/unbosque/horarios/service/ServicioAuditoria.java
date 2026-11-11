package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Registro de auditoría de solo lectura.
 * <p>
 * Los registros solo se agregan; no existe ninguna operación para modificarlos ni eliminarlos.
 * </p>
 */
@Service
public class ServicioAuditoria {
	private final AlmacenJson db;

	/**
	 * Crea el servicio.
	 *
	 * @param db almacenamiento del estado
	 */
	public ServicioAuditoria(AlmacenJson db) {
		this.db = db;
	}

	/**
	 * Agrega un registro al estado que se está modificando.
	 *
	 * @param e           estado dentro de una operación de cambio
	 * @param tipo        {@code EXITO}, {@code ERROR}, {@code ALERTA} o {@code INFO}
	 * @param accion      nombre corto de la acción
	 * @param descripcion detalle de la acción
	 * @param usuario     usuario que realizó la acción
	 */
	public void registrar(Estado e, String tipo, String accion, String descripcion, String usuario) {
		e.auditoria().add(new RegistroAuditoria(e.auditoria().size() + 1, Soporte.ahora(), tipo, accion, descripcion,
				usuario));
	}

	/**
	 * Lista los registros, del más reciente al más antiguo.
	 *
	 * @return registros de auditoría
	 */
	public List<RegistroAuditoria> listar() {
		List<RegistroAuditoria> lista = new ArrayList<>(db.leer().auditoria());
		Collections.reverse(lista);
		return lista;
	}
}
