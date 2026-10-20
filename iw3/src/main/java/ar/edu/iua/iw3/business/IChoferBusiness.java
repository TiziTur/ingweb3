package ar.edu.iua.iw3.business;

import java.util.List;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Chofer;

public interface IChoferBusiness {
	public List<Chofer> list() throws BusinessException;

	public Chofer load(String codigoExterno) throws NotFoundException, BusinessException;

	// Si existe un chofer con ese código externo lo actualiza, si no lo crea.
	public Chofer sincronizar(Chofer chofer) throws BadRequestException, BusinessException;
}
