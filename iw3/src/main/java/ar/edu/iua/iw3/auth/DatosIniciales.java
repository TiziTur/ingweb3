package ar.edu.iua.iw3.auth;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Se ejecuta una vez al arrancar la aplicación (CommandLineRunner).
 * Crea los roles y un usuario por cada sistema, solo si todavía no existen.
 * Así cualquier integrante puede levantar el proyecto con la base vacía y ya tener usuarios.
 *
 * Usuarios creados (la contraseña es "usuarios.password.inicial" de application.properties):
 *   admin    -> ROLE_ADMIN    (puede todo)
 *   sap      -> ROLE_SAP      (punto 1: envía órdenes)
 *   tms      -> ROLE_TMS      (puntos 2 y 5: balanza)
 *   carga    -> ROLE_CARGA    (puntos 3 y 4: sistema de carga)
 *   operador -> ROLE_OPERADOR (consultas; será el usuario del frontend)
 */
@Component
@Slf4j
public class DatosIniciales implements CommandLineRunner {

	@Autowired
	private RoleRepository roleDAO;

	@Autowired
	private UserRepository userDAO;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${usuarios.password.inicial}")
	private String passwordInicial;

	@Override
	public void run(String... args) throws Exception {
		Role admin = crearRol("ROLE_ADMIN", "Administrador");
		Role sap = crearRol("ROLE_SAP", "Sistema externo SAP");
		Role tms = crearRol("ROLE_TMS", "Sistema de balanza TMS");
		Role carga = crearRol("ROLE_CARGA", "Sistema de carga");
		Role operador = crearRol("ROLE_OPERADOR", "Operador de planta");

		crearUsuario("admin", admin);
		crearUsuario("sap", sap);
		crearUsuario("tms", tms);
		crearUsuario("carga", carga);
		crearUsuario("operador", operador);
	}

	private Role crearRol(String nombre, String descripcion) {
		return roleDAO.findByName(nombre).orElseGet(() -> {
			Role r = new Role();
			r.setName(nombre);
			r.setDescription(descripcion);
			log.info("Creando rol {}", nombre);
			return roleDAO.save(r);
		});
	}

	private void crearUsuario(String username, Role rol) {
		if (userDAO.findOneByUsernameOrEmail(username, username).isPresent()) {
			return;
		}
		User u = new User();
		u.setUsername(username);
		u.setEmail(username + "@iw3.local");
		u.setPassword(passwordEncoder.encode(passwordInicial));
		u.setEnabled(true);
		Set<Role> roles = new HashSet<>();
		roles.add(rol);
		u.setRoles(roles);
		userDAO.save(u);
		log.info("Creando usuario {} con rol {}", username, rol.getName());
	}
}
