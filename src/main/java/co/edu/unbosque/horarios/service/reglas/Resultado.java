package co.edu.unbosque.horarios.service.reglas;

import java.util.List;

/**
 * Resultado de evaluar una sesión.
 *
 * @param valido       {@code true} si no se incumple ninguna regla dura
 * @param errores      reglas duras incumplidas
 * @param advertencias reglas blandas incumplidas
 */
public record Resultado(boolean valido, List<Hallazgo> errores, List<Hallazgo> advertencias) {
}
