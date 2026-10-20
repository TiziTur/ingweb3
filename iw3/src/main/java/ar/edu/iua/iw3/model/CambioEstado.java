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
 * Log de auditoría: cada vez que una orden cambia de estado se guarda una fila acá,
 * con la fecha, el usuario que hizo la acción y una observación.
 */
@Entity
@Table(name = "cambios_estado")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CambioEstado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_orden", nullable = false)
	private Orden orden;

	// Es null cuando la orden recién se crea
	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private EstadoOrden estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(length = 30, nullable = false)
	private EstadoOrden estadoNuevo;

	private Date fecha;

	// Usuario que hizo el cambio (el del token JWT): por ejemplo "sap", "tms" o "carga"
	@Column(length = 100)
	private String usuario;

	private String observacion;
}
