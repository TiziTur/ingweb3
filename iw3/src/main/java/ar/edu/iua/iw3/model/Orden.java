package ar.edu.iua.iw3.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Orden de carga de gas. Es el centro del sistema: guarda los datos que manda SAP (punto 1),
 * los pesajes del TMS (puntos 2 y 5), las fechas de cada evento y los últimos valores
 * recibidos durante la carga (punto 3).
 */
@Entity
@Table(name = "ordenes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Orden {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Número de orden: es el código que conocen los sistemas externos. Único.
	@Column(nullable = false, unique = true)
	private Long numeroOrden;

	@ManyToOne
	@JoinColumn(name = "id_camion", nullable = false)
	private Camion camion;

	@ManyToOne
	@JoinColumn(name = "id_chofer", nullable = false)
	private Chofer chofer;

	@ManyToOne
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	// Se guarda como texto ("CERRADA") para que en la base se entienda sin buscar el número
	@Enumerated(EnumType.STRING)
	@Column(length = 30, nullable = false)
	private EstadoOrden estado;

	// Turno de carga previsto
	private Date fechaPrevistaCarga;

	// Kilogramos a cargar
	private Double preset;

	// ----- Pesajes (kg) -----
	private Double tara;          // pesaje inicial, camión vacío
	private Double pesajeFinal;   // pesaje final, camión cargado

	// Contraseña de activación de 5 dígitos. Nunca se muestra en el JSON de la orden:
	// solo se devuelve una vez, en la respuesta del pesaje inicial.
	@JsonIgnore
	private Integer password;

	// ----- Fechas de cada evento -----
	private Date fechaRecepcion;      // punto 1
	private Date fechaPesajeInicial;  // punto 2
	private Date fechaInicioCarga;    // primer dato válido de detalle
	private Date fechaFinCarga;       // último dato válido de detalle
	private Date fechaPesajeFinal;    // punto 5

	// ----- Últimos valores recibidos durante la carga (se pisan con cada dato válido) -----
	private Double ultimaMasaAcumulada;
	private Double ultimaDensidad;
	private Double ultimaTemperatura;
	private Double ultimoCaudal;
	private Date fechaUltimoDato;

	// Número de estado (1 a 4) para mostrarlo junto con el nombre
	public int getNumeroEstado() {
		return estado == null ? 0 : estado.getNumero();
	}
}
