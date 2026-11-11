package co.edu.unbosque.horarios.service.reglas;

/**
 * Incumplimiento de una regla.
 *
 * @param regla   código de la regla, de R1 a R12
 * @param tipo    {@code DURA} o {@code BLANDA}
 * @param mensaje descripción del problema
 */
public record Hallazgo(String regla, String tipo, String mensaje) {
}
