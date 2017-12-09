package it.polito.dp2.NFV.sol2;

import java.util.Set;

import it.polito.dp2.NFV.HostReader;
import it.polito.dp2.NFV.LinkReader;
import it.polito.dp2.NFV.NffgReader;
import it.polito.dp2.NFV.NodeReader;
import it.polito.dp2.NFV.VNFTypeReader;
import it.polito.dp2.NFV.lab2.ExtendedNodeReader;
import it.polito.dp2.NFV.lab2.NoGraphException;
import it.polito.dp2.NFV.lab2.ServiceException;

public class MyExtendedNodeReader implements ExtendedNodeReader
{
	private NodeReader node_r;
	private String nffgLoaded;
	private Set<HostReader> hostSet;
	
	// Class constructor
	public MyExtendedNodeReader(NodeReader node_r, String nffgLoaded, Set<HostReader> hostSet)
	{
		this.node_r = node_r;
		this.nffgLoaded = nffgLoaded;
		this.hostSet = hostSet;
	}
	
	@Override
	public String getName()
	{
		return node_r.getName();
	}
	
	@Override
	public VNFTypeReader getFuncType()
	{
		return node_r.getFuncType();
	}

	@Override
	public HostReader getHost()
	{
		return node_r.getHost();
	}

	@Override
	public Set<LinkReader> getLinks()
	{
		return node_r.getLinks();
	}

	@Override
	public NffgReader getNffg()
	{
		return node_r.getNffg();
	}

	@Override
	public Set<HostReader> getReachableHosts() throws NoGraphException, ServiceException
	{	
		// Check if a graph is currently loaded
		if (nffgLoaded == null)
			throw new NoGraphException("No Graph is currently loaded");
		
		return hostSet;
	}

}
