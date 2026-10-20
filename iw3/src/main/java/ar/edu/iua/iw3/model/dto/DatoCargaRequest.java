package ar.edu.iua.iw3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dato en tiempo real que manda el sistema de carga en el punto 3:
 * {"masaAcumulada": 1500, "densidad": 0.55, "temperatura": 18.5, "caudal": 3000}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatoCargaRequest {
	private Double masaAcumulada; // kg
	private Double densidad;      // kg/m3
	private Double temperatura;   // °C
	private Double caudal;        // kg/h
}
