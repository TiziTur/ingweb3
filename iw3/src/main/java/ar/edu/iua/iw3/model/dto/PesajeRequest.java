package ar.edu.iua.iw3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo que manda el TMS (balanza) en los puntos 2 y 5: {"peso": 15000}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesajeRequest {
	private Double peso; // kg
}
