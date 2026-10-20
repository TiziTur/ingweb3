package ar.edu.iua.iw3.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Camión que viene del sistema externo (SAP).
 * Se identifica entre sistemas por su codigoExterno.
 */
@Entity
@Table(name = "camiones")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Camion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Identificador del camión en el sistema externo. Es el que se usa para sincronizar.
	@Column(length = 50, nullable = false, unique = true)
	private String codigoExterno;

	@Column(length = 20, nullable = false)
	private String patente;

	// Opcional según la consigna
	private String descripcion;

	// Cisternado: el volumen de cada cisterna. Se guarda en la tabla "cisternas"
	// con una columna id_camion. Al borrar o reemplazar la lista, se borran las cisternas viejas.
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id_camion")
	@OrderBy("numero")
	private List<Cisterna> cisternas = new ArrayList<>();

	// Volumen total del camión: no se guarda en la base, se calcula sumando las cisternas.
	public double getTotalLitros() {
		double total = 0;
		if (cisternas != null) {
			for (Cisterna c : cisternas) {
				total += c.getCapacidadLitros();
			}
		}
		return total;
	}
}
