package ar.edu.iua.iw3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta del punto 3 cuando la password es correcta: el sistema de carga recibe el preset.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresetResponse {
	private Long numeroOrden;
	private Double preset; // kg a cargar
}
