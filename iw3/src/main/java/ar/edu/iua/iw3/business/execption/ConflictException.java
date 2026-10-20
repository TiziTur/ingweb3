package ar.edu.iua.iw3.business.execption;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * La acción no corresponde al estado actual de la orden. El controlador responde 409 (CONFLICT).
 * Mismo formato que NotFoundException y FoundException del proyecto base.
 */
@NoArgsConstructor
public class ConflictException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public ConflictException(String message, Throwable ex) {
		super(message, ex);
	}

	@Builder
	public ConflictException(String message) {
		super(message);
	}

	@Builder
	public ConflictException(Throwable ex) {
		super(ex);
	}
}
