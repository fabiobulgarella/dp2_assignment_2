package it.polito.dp2.NFV.sol2;

import java.util.Set;

import it.polito.dp2.NFV.HostReader;
import it.polito.dp2.NFV.LinkReader;
import it.polito.dp2.NFV.NffgReader;
import it.polito.dp2.NFV.VNFTypeReader;
import it.polito.dp2.NFV.lab2.ExtendedNodeReader;
import it.polito.dp2.NFV.lab2.NoGraphException;
import it.polito.dp2.NFV.lab2.ServiceException;

public class MyExtendedNodeReader implements ExtendedNodeReader {

	@Override
	public VNFTypeReader getFuncType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HostReader getHost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<LinkReader> getLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NffgReader getNffg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<HostReader> getReachableHosts() throws NoGraphException, ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

}
