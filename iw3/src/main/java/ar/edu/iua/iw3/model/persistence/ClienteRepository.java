package ar.edu.iua.iw3.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.iua.iw3.model.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	// Spring arma la consulta solo a partir del nombre del método:
	// SELECT * FROM ... WHERE codigo_externo = ?
	Optional<Cliente> findByCodigoExterno(String codigoExterno);
}
