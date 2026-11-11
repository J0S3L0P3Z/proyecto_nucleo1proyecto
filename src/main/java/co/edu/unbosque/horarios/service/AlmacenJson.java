package co.edu.unbosque.horarios.service;

import co.edu.unbosque.horarios.model.Domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.function.Function;

/**
 * Persistencia del estado del sistema en un archivo JSON.
 * <p>
 * Mantiene una copia en memoria y escribe el archivo de forma atómica para evitar
 * dejarlo corrupto si la aplicación se detiene a mitad de una escritura. Todas las
 * modificaciones pasan por {@link #transformar(Function)}, que las serializa.
 * </p>
 */
@Service
public class AlmacenJson {
	private final ObjectMapper mapper;
	private final Path archivo;
	private Estado estado;

	/**
	 * Resultado de una modificación: el estado que se debe guardar y el valor que se devuelve.
	 *
	 * @param <T>       tipo del valor devuelto
	 * @param estado    estado que se va a persistir
	 * @param resultado valor que se entrega a quien pidió el cambio
	 */
	public record Cambio<T>(Estado estado, T resultado) {
	}

	/**
	 * Carga el estado desde el archivo {@code estado.json}. Si el archivo no existe,
	 * usa los datos precargados de {@code datos-iniciales.json} y, si tampoco existen,
	 * crea un estado vacío.
	 *
	 * @param mapper     conversor JSON de Jackson
	 * @param directorio carpeta donde se guarda el archivo (propiedad {@code app.data-dir})
	 * @throws IllegalStateException si el archivo existe pero no se puede leer
	 */
	public AlmacenJson(ObjectMapper mapper, @Value("${app.data-dir:./data}") String directorio) {
		this.mapper = mapper;
		archivo = Path.of(directorio).resolve("estado.json");
		try {
			Files.createDirectories(archivo.toAbsolutePath().getParent());
			estado = Files.exists(archivo) ? mapper.readValue(archivo.toFile(), Estado.class) : datosIniciales();
		} catch (IOException e) {
			throw new IllegalStateException("No se puede leer el estado JSON", e);
		}
	}

	/**
	 * Lee los datos precargados desde {@code src/main/resources/datos-iniciales.json}.
	 *
	 * @return estado precargado, o un estado vacío si el recurso no existe
	 * @throws IOException si el recurso existe pero no se puede leer
	 */
	private Estado datosIniciales() throws IOException {
		ClassPathResource recurso = new ClassPathResource("datos-iniciales.json");
		if (!recurso.exists())
			return Estado.vacio();
		try (InputStream entrada = recurso.getInputStream()) {
			return mapper.readValue(entrada, Estado.class);
		}
	}

	/**
	 * Devuelve una copia profunda del estado actual.
	 * <p>
	 * Al ser una copia, modificarla no afecta el estado guardado hasta llamar a {@link #guardar(Estado)}.
	 * </p>
	 *
	 * @return copia del estado
	 */
	public synchronized Estado leer() {
		return mapper.convertValue(mapper.valueToTree(estado), Estado.class);
	}

	/**
	 * Escribe el estado en disco y actualiza la copia en memoria.
	 *
	 * @param nuevo estado que se va a persistir
	 * @throws IllegalStateException si ocurre un error de escritura
	 */
	public synchronized void guardar(Estado nuevo) {
		try {
			Path tmp = Files.createTempFile(archivo.toAbsolutePath().getParent(), "estado-", ".tmp");
			try {
				mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), nuevo);
				try {
					Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException ex) {
					Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
				}
			} finally {
				Files.deleteIfExists(tmp);
			}
			estado = mapper.convertValue(mapper.valueToTree(nuevo), Estado.class);
		} catch (IOException e) {
			throw new IllegalStateException("Error al persistir JSON", e);
		}
	}

	/**
	 * Aplica una modificación que puede reemplazar el estado completo y la guarda.
	 * <p>
	 * Si la operación lanza una excepción, no se guarda nada.
	 * </p>
	 *
	 * @param <T> tipo del valor devuelto
	 * @param op  operación que recibe una copia del estado y devuelve el estado nuevo y un resultado
	 * @return resultado de la operación
	 */
	public synchronized <T> T transformar(Function<Estado, Cambio<T>> op) {
		Cambio<T> c = op.apply(leer());
		guardar(c.estado());
		return c.resultado();
	}

	/**
	 * Aplica una modificación sobre las colecciones del estado y la guarda.
	 * <p>
	 * Si la operación lanza una excepción, no se guarda nada.
	 * </p>
	 *
	 * @param <T> tipo del valor devuelto
	 * @param op  operación que modifica el estado y devuelve un resultado
	 * @return resultado de la operación
	 */
	public <T> T cambio(Function<Estado, T> op) {
		return transformar(e -> new Cambio<>(e, op.apply(e)));
	}
}
