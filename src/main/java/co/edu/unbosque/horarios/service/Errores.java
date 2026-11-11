package co.edu.unbosque.horarios.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Manejo centralizado de errores de la API, que responde con HTTP 400 y un mensaje en el campo {@code error}.
 */
@RestControllerAdvice
public class Errores {
	/**
	 * Excepción para errores de reglas de negocio o de datos inválidos enviados por el cliente.
	 */
	public static class Negocio extends RuntimeException {
		/**
		 * Crea la excepción con un mensaje para el usuario.
		 *
		 * @param m mensaje descriptivo del error
		 */
		public Negocio(String m) {
			super(m);
		}
	}

	/**
	 * Convierte una excepción de negocio en una respuesta HTTP 400.
	 *
	 * @param e excepción lanzada
	 * @return respuesta con el mensaje de error
	 */
	@ExceptionHandler(Negocio.class)
	public ResponseEntity<?> negocio(Negocio e) {
		return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
	}

	/**
	 * Responde HTTP 400 cuando el cuerpo de la petición no es un JSON válido o trae un enumerado desconocido.
	 *
	 * @param e excepción de lectura del mensaje
	 * @return respuesta con el mensaje de error
	 */
	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	public ResponseEntity<?> json(Exception e) {
		return ResponseEntity.badRequest().body(Map.of("error", "JSON inválido o valor de enumeración incorrecto"));
	}

	/**
	 * Responde HTTP 400 cuando los datos de entrada no pasan la validación.
	 *
	 * @param e excepción de validación
	 * @return respuesta con el mensaje de error
	 */
	@ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
	public ResponseEntity<?> validar(Exception e) {
		return ResponseEntity.badRequest().body(Map.of("error", "Datos de entrada inválidos"));
	}

	/**
	 * Responde HTTP 400 cuando un parámetro de la URL tiene un tipo o un valor incorrecto.
	 *
	 * @param e excepción de conversión del parámetro
	 * @return respuesta con el mensaje de error
	 */
	@ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
	public ResponseEntity<?> parametro(Exception e) {
		return ResponseEntity.badRequest().body(Map.of("error", "Parámetro con valor inválido"));
	}
}
