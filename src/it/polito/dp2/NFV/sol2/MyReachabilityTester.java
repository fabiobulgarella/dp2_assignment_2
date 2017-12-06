package it.polito.dp2.NFV.sol2;

import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import it.polito.dp2.NFV.NfvReader;
import it.polito.dp2.NFV.lab2.AlreadyLoadedException;
import it.polito.dp2.NFV.lab2.ExtendedNodeReader;
import it.polito.dp2.NFV.lab2.NoGraphException;
import it.polito.dp2.NFV.lab2.ReachabilityTester;
import it.polito.dp2.NFV.lab2.ServiceException;
import it.polito.dp2.NFV.lab2.UnknownNameException;

public class MyReachabilityTester implements ReachabilityTester
{
	private NfvReader monitor;
	private String url;
	private WebTarget target;

	public MyReachabilityTester(NfvReader monitor)
	{
		this.monitor = monitor;
		this.url = System.getProperty("it.polito.dp2.NFV.lab2.URL");
		
		// check if System Property has been read correctly
		if (url == null)
        {
        	System.err.println("System property \"it.polito.dp2.NFV.lab2.URL\" not found");
        	System.exit(1);
        }
		
		// create JAX-RS client and WebTarget
		Client clientObject = ClientBuilder.newClient();
		target = clientObject.target( UriBuilder.fromUri(url).build() );
	}

	@Override
	public void loadGraph(String nffgName) throws UnknownNameException, AlreadyLoadedException, ServiceException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public Set<ExtendedNodeReader> getExtendedNodes(String nffgName) throws UnknownNameException, NoGraphException, ServiceException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLoaded(String nffgName) throws UnknownNameException
	{
		// TODO Auto-generated method stub
		return false;
	}

}
