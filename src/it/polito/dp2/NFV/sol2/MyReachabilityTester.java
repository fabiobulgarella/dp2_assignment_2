package it.polito.dp2.NFV.sol2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import it.polito.dp2.NFV.HostReader;
import it.polito.dp2.NFV.LinkReader;
import it.polito.dp2.NFV.NffgReader;
import it.polito.dp2.NFV.NfvReader;
import it.polito.dp2.NFV.NodeReader;
import it.polito.dp2.NFV.lab2.AlreadyLoadedException;
import it.polito.dp2.NFV.lab2.ExtendedNodeReader;
import it.polito.dp2.NFV.lab2.NoGraphException;
import it.polito.dp2.NFV.lab2.ReachabilityTester;
import it.polito.dp2.NFV.lab2.ReachabilityTesterException;
import it.polito.dp2.NFV.lab2.ServiceException;
import it.polito.dp2.NFV.lab2.UnknownNameException;

public class MyReachabilityTester implements ReachabilityTester
{
	private NfvReader monitor;
	private WebTarget target;
	private ObjectFactory objFactory;
	
	private NffgReader nffg_r;
	
	private HashSet<String> nffgLoadedSet;
	private HashMap<String, String> nodeMap;
	private HashMap<String, String> hostMap;

	public MyReachabilityTester(NfvReader monitor, String url) throws ReachabilityTesterException
	{
		this.monitor = monitor;
		
		// Create JAX-RS Client and WebTarget
		Client client = ClientBuilder.newClient();
		try {
			target = client.target(url);
		}
		catch (IllegalArgumentException iae) {
			throw new ReachabilityTesterException(iae, "Url is not a valid URI");
		}
		
		// Instantiate ObjectFactory
		objFactory = new ObjectFactory();
		
		// Instantiate HashMap and HashSet to track loaded nodes and relationships
		nodeMap = new HashMap<String, String>();
		hostMap = new HashMap<String, String>();
		
		// Instantiate HashSet to track nffgs that have been loaded
		nffgLoadedSet = new HashSet<String>();
	}

	@Override
	public void loadGraph(String nffgName) throws UnknownNameException, AlreadyLoadedException, ServiceException
	{
		// Check if already loaded
		if ( isLoaded(nffgName) )
			throw new AlreadyLoadedException("Nffg \"" + nffgName + "\" already loaded");
		
		// Get nffg-nodes and load them into graph (Type "Node")
		loadNodes("Node");
				
		// Get links and create relationships
		loadRelationships("ForwardsTo");
		
		// Get hosts used by NFFG
		loadNodes("Host");
		
		// Create relationship between nodes and hosts
		loadRelationships("AllocatedOn");
		
		// Set this nffg as successfully loaded
		nffgLoadedSet.add(nffgName);
	}

	@Override
	public Set<ExtendedNodeReader> getExtendedNodes(String nffgName) throws UnknownNameException, NoGraphException, ServiceException
	{
		Set<ExtendedNodeReader> set = new HashSet<ExtendedNodeReader>();
		
		// Check if "nffgName" is null, unknown or not loaded
		if ( !isLoaded(nffgName) )
			throw new NoGraphException("Nffg \"" + nffgName + "\" has not been loaded");
		
		// nffg_r already correctly initialized by "isLoaded" method
		for (NodeReader node_r: nffg_r.getNodes())
		{	
			// Get node id
			String nodeID = nodeMap.get(node_r.getName());
			
			// Create a new ExtendedNodeReader for the relative node
			ExtendedNodeReader newExNode_r = new MyExtendedNodeReader(target, nffgLoadedSet, node_r, nodeID);
			set.add(newExNode_r);
		}
		
		return set;
	}

	@Override
	public boolean isLoaded(String nffgName) throws UnknownNameException
	{
		// Check if valid argument has been passed
		if (nffgName == null)
			throw new UnknownNameException("Argument nffgName can't be null");
		
		// Check if nffg exists
		nffg_r = monitor.getNffg(nffgName);
		if (nffg_r == null)
			throw new UnknownNameException("Unknown nffgName, retreiving nffg failed");
		
		// Check if already loaded
		return nffgLoadedSet.contains(nffgName);
	}
	
	private void loadNodes(String type) throws ServiceException
	{
		for (NodeReader node_r: nffg_r.getNodes())
		{
			String nodeName;
			HostReader host_r;
			
			// Type is Node
			if (type.equals("Node"))
			{
				// Get nodeName (nffg-node)
				nodeName = node_r.getName();
			}
			
			// Type is Host
			else
			{
				host_r = node_r.getHost();
				
				// Check if node is not allocated on a host
				if (host_r == null) continue;
				
				// Get hostName
				nodeName = host_r.getName();
				
				// Check if host has been already uploaded on neo4j
				if (hostMap.get(nodeName) != null) continue;
			}	
			
			// Create a new node object
			Node newNode = objFactory.createNode();
			Properties newProperties = objFactory.createProperties();
			Property newProperty = objFactory.createProperty();
			newProperty.setName("name");
			newProperty.setValue(nodeName);
			newProperties.getProperty().add(newProperty);
			newNode.setProperties(newProperties);
			
			// Create a new labels object
			Labels newLabels = objFactory.createLabels();
			newLabels.getLabel().add(type);
			
			// Call Neo4JSimpleXML API
			try {
				Node res = target.path("data/node")
						         .request(MediaType.APPLICATION_XML)
						         .post(Entity.entity(newNode, MediaType.APPLICATION_XML), Node.class);
				
				Response res2 = target.path("data/node/" + res.id + "/labels")
						              .request(MediaType.APPLICATION_XML)
						              .post(Entity.entity(newLabels, MediaType.APPLICATION_XML));
				
				// Check "res2" response (it doesn't throw exception automatically)
				if (res2.getStatus() != 204)
					throw new WebApplicationException();
				
				if (type.equals("Node"))
					nodeMap.put(nodeName, res.getId());
				else
					hostMap.put(nodeName, res.getId());
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
		}
	}
	
	private void loadRelationships(String type) throws ServiceException
	{
		// Type is ForwardsTo
		if (type.equals("ForwardsTo"))
		{
			for (NodeReader node_r: nffg_r.getNodes())
				for (LinkReader link_r: node_r.getLinks())
					postRelationships(type, node_r, link_r);
		}
		
		// Type is AllocatedOn
		else
		{
			for (NodeReader node_r: nffg_r.getNodes())
				postRelationships(type, node_r, null);
		}
	}
	
	private void postRelationships(String type, NodeReader node_r, LinkReader link_r) throws ServiceException
	{
		HostReader host_r;
		
		// Retrieve source and destination node id from nodeMap
		String srcNodeID = nodeMap.get( node_r.getName() );
		String dstNodeID;
		
		// Type is ForwardsTo
		if (type.equals("ForwardsTo"))
		{
			// Get dstNodeID relatively to a node
			dstNodeID = nodeMap.get( link_r.getDestinationNode().getName() );
		}
		
		// Type is AllocatedOn
		else
		{
			host_r = node_r.getHost();
			
			// Check if there's no host that allocates the node
			if (host_r == null) return;
			
			// Get dstNodeID relatively to an host
			dstNodeID = hostMap.get( host_r.getName() );
		}
		
		// Create a new relationship object
		Relationship newRelationship = objFactory.createRelationship();
		newRelationship.setDstNode(dstNodeID);
		newRelationship.setType(type);
		
		// Call Neo4JSimpleXML API
		try {
			Relationship res = target.path("data/node/" + srcNodeID + "/relationships")
					                 .request(MediaType.APPLICATION_XML)
					                 .post(Entity.entity(newRelationship, MediaType.APPLICATION_XML), Relationship.class);
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
	}

}
