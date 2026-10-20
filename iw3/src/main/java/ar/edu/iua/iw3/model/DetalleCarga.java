package ar.edu.iua.iw3.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
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
 * Registro histórico de la carga (punto 3). No se guardan todos los datos que llegan,
 * sino uno cada "carga.detalle.frecuencia.segundos" (ver application.properties).
 */
@Entity
@Table(name = "detalles_carga")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DetalleCarga {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @JsonIgnore evita que al mostrar el detalle se muestre otra vez la orden completa
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_orden", nullable = false)
	private Orden orden;

	private double masaAcumulada; // kg
	private double densidad;      // kg/m3, entre 0 y 1
	private double temperatura;   // °C
	private double caudal;        // kg/h

	// Momento en que se recibió el dato
	private Date fecha;
}
