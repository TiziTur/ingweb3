package ar.edu.iua.iw3.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una cisterna de un camión (ejemplo: cisterna 1 = 10.000 L).
 * Solo existe dentro de un Camion, por eso no tiene código externo propio.
 */
@Entity
@Table(name = "cisternas")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cisterna {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Número de cisterna dentro del camión: 1, 2, 3...
	private int numero;

	private double capacidadLitros;
}
