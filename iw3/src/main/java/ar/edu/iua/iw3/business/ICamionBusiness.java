package ar.edu.iua.iw3.business;

import java.util.List;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Camion;

public interface ICamionBusiness {
	public List<Camion> list() throws BusinessException;

	public Camion load(String codigoExterno) throws NotFoundException, BusinessException;

	// Si existe un camion con ese código externo lo actualiza, si no lo crea.
	public Camion sincronizar(Camion camion) throws BadRequestException, BusinessException;
}
