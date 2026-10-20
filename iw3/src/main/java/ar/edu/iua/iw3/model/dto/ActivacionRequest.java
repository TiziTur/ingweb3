package ar.edu.iua.iw3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo que manda el sistema de carga en el punto 3 para activar la carga: {"password": 12345}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivacionRequest {
	private Integer password;
}
