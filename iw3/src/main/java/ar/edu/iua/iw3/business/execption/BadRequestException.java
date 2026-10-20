package ar.edu.iua.iw3.business.execption;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Los datos recibidos son inválidos (faltan datos, password incorrecta, dato de carga descartado). El controlador responde 400 (BAD_REQUEST).
 * Mismo formato que NotFoundException y FoundException del proyecto base.
 */
@NoArgsConstructor
public class BadRequestException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public BadRequestException(String message, Throwable ex) {
		super(message, ex);
	}

	@Builder
	public BadRequestException(String message) {
		super(message);
	}

	@Builder
	public BadRequestException(Throwable ex) {
		super(ex);
	}
}
