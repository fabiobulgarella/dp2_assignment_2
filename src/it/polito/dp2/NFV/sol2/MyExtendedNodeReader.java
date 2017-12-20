package it.polito.dp2.NFV.sol2;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

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
	private WebTarget target;
	private StringBuilder nffgLoaded;
	private NodeReader node_r;
	private String nodeID;
	
	// Class constructor
	public MyExtendedNodeReader(WebTarget target, StringBuilder nffgLoaded, NodeReader node_r, String nodeID)
	{
		this.target = target;
		this.nffgLoaded = nffgLoaded;
		this.node_r = node_r;
		this.nodeID = nodeID;
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
		// Check if a graph corresponding to node's nffg is currently loaded
		if ( !node_r.getNffg().getName().equals(nffgLoaded.toString()) )
			throw new NoGraphException("No Graph corresponding to this node's nffg is currently loaded");
		
		// Call Neo4JSimpleXML API
		Nodes reachableNodes;
		
		try {
			reachableNodes = target.path("data/node/" + nodeID + "/reachableNodes")
					               .queryParam("relationshipTypes", "ForwardsTo")
					               .queryParam("nodeLabel", "Node")
					               .request()
					               .accept(MediaType.APPLICATION_XML)
					               .get(Nodes.class);
		}
		catch (ProcessingException pe) {
			throw new ServiceException("Error during JAX-RS request processing", pe);
		}
		catch (WebApplicationException wae) {
			throw new ServiceException("Server returned error", wae);
		}
		catch (Exception e) {
			throw new ServiceException("Unexpected exception", e);
		}
		
		// Create reachable hostSet and hostNameSet (to keep track of host already added in the list)
		Set<HostReader> reachableHostSet = new HashSet<HostReader>();
		HashSet<String> reachableHostNameSet = new HashSet<String>();
		
		// Add host where node is allocated on into the set
		HostReader host_r = node_r.getHost();
		if (host_r != null)
		{
			reachableHostSet.add(host_r);
			reachableHostNameSet.add(host_r.getName());
		}
		
		// Search for reachable hosts
		for (Node node: reachableNodes.getNode())
		{
			String nodeName = node.getProperties().getProperty().iterator().next().getValue();
			
			// Add reachable host if present AND if not already loaded inside reachableHostSet
			HostReader newReachableHost = node_r.getNffg().getNode(nodeName).getHost();
			
			if (newReachableHost != null)
			{
				String newReachableHostName = newReachableHost.getName();
				
				if ( !reachableHostNameSet.contains(newReachableHostName) )
				{
					reachableHostSet.add(newReachableHost);
					reachableHostNameSet.add(newReachableHostName);
				}
			}
		}
		
		return reachableHostSet;
	}

}
