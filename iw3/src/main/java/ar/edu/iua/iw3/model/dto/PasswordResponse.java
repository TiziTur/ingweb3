package ar.edu.iua.iw3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta del punto 2: el TMS recibe la contraseña de activación generada.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResponse {
	private Long numeroOrden;
	private Integer password;
}
