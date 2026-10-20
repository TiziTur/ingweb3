package ar.edu.iua.iw3.model;

/**
 * Los 4 estados de una orden. El orden en que están escritos es el orden del proceso:
 * una orden solo puede avanzar al estado siguiente, nunca retroceder.
 */
public enum EstadoOrden {
	PENDIENTE_PESAJE_INICIAL, // Estado 1: se recibió desde SAP
	CON_PESAJE_INICIAL,       // Estado 2: se registró la tara y se generó la password; acá se carga
	CERRADA,                  // Estado 3: se cerró la carga, no se aceptan más datos de detalle
	FINALIZADA;               // Estado 4: se registró el pesaje final, se puede pedir la conciliación

	// Número de estado como lo nombra la consigna (1 a 4)
	public int getNumero() {
		return this.ordinal() + 1;
	}
}
