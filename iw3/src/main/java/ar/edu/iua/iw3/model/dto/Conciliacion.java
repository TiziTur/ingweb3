package ar.edu.iua.iw3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conciliación de una orden finalizada (punto 5). No se guarda en la base:
 * se calcula cada vez que se pide, a partir de la orden y sus detalles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conciliacion {
	private Long numeroOrden;
	private Double pesajeInicial;                  // tara
	private Double pesajeFinal;
	private Double productoCargado;                // última masa acumulada
	private Double netoPorBalanza;                 // pesajeFinal - pesajeInicial
	private Double diferenciaBalanzaCaudalimetro;  // netoPorBalanza - productoCargado
	private Double promedioTemperatura;            // de los detalles guardados
	private Double promedioDensidad;
	private Double promedioCaudal;
}
