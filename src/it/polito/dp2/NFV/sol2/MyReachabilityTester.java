package it.polito.dp2.NFV.sol2;

import java.util.HashMap;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import it.polito.dp2.NFV.HostReader;
import it.polito.dp2.NFV.LinkReader;
import it.polito.dp2.NFV.NffgReader;
import it.polito.dp2.NFV.NfvReader;
import it.polito.dp2.NFV.NodeReader;
import it.polito.dp2.NFV.VNFTypeReader;
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
	private ObjectFactory objFactory;
	
	private String nffgLoaded;
	private NffgReader nffg_r;
	
	private HashMap<String, String> nodeMap;
	private HashMap<String, String> hostMap;

	public MyReachabilityTester(NfvReader monitor)
	{
		this.monitor = monitor;
		this.url = System.getProperty("it.polito.dp2.NFV.lab2.URL");
		
		// Check if System Property has been read correctly
		if (url == null)
        {
        	System.err.println("System property \"it.polito.dp2.NFV.lab2.URL\" not found");
        	System.exit(1);
        }
		
		// Create JAX-RS Client and WebTarget
		Client client = ClientBuilder.newClient();
		target = client.target( UriBuilder.fromUri(url).build() );
		
		// Instantiate ObjectFactory
		objFactory = new ObjectFactory();
		
		// Instantiate HashMap to track loaded nodes and hosts
		nodeMap = new HashMap<String, String>();
		hostMap = new HashMap<String, String>();
	}

	@Override
	public void loadGraph(String nffgName) throws UnknownNameException, AlreadyLoadedException, ServiceException
	{
		// Check if already loaded
		if ( isLoaded(nffgName) )
			throw new AlreadyLoadedException();
		
		// Delete all previous loaded nodes
		deleteAllNodes();
		
		// Get nffg-nodes and load them into graph (Type "Node")
		loadNodes("Nodes");
				
		// Get links and create relationships
		for (NodeReader node_r: nffg_r.getNodes()) {
			for (LinkReader link_r: node_r.getLinks())
			{
				// Retrieve source and destination node id from nodeMap
				String srcNodeID = nodeMap.get( node_r.getName() );
				String dstNodeID = nodeMap.get( link_r.getDestinationNode().getName() );
				
				// Create a new relationship object
				Relationship newRelationship = objFactory.createRelationship();
				newRelationship.setDstNode(dstNodeID);
				newRelationship.setType("ForwardsTo");
				
				// Call Neo4JSimpleXML API
				try {
					Relationship res = target.path("data/node/" + srcNodeID + "/relationships")
							                 .request(MediaType.APPLICATION_XML)
							                 .post(Entity.entity(newRelationship, MediaType.APPLICATION_XML), Relationship.class);
				}
				catch (ProcessingException pe) {
					System.err.println("Error during JAX-RS request processing");
					throw new ServiceException(pe);
				}
				catch (WebApplicationException wae) {
					System.err.println("Server returned error: links");
					throw new ServiceException(wae);
				}
				catch (Exception e) {
					System.err.println("Unexpected exception");
					throw new ServiceException(e);
				}
			}
		}
		
		// Get hosts used by NFFG
		for (NodeReader node_r: nffg_r.getNodes())
		{
			HostReader host_r = node_r.getHost();
			
			// Check if node is not allocated on a host
			if (host_r == null) continue;
			
			String hostName = host_r.getName();
			
			// Check if host has been already uploaded on neo4j
			if (hostMap.get(hostName) != null) continue;
			
			// Create a new node object
			Node newNode = objFactory.createNode();
			Properties newProperties = objFactory.createProperties();
			Property newProperty = objFactory.createProperty();
			newProperty.setName("name");
			newProperty.setValue(hostName);
			newProperties.getProperty().add(newProperty);
			newNode.setProperties(newProperties);
			
			// Create a new labels object
			Labels newLabels = objFactory.createLabels();
			newLabels.getLabel().add("Host");
			
			// Call Neo4JSimpleXML API
			try {
				Node res = target.path("data/node")
						         .request(MediaType.APPLICATION_XML)
						         .post(Entity.entity(newNode, MediaType.APPLICATION_XML), Node.class);
				
				hostMap.put(hostName, res.id);
				
				Response res2 = target.path("data/node/" + res.id + "/labels")
						              .request(MediaType.APPLICATION_XML)
						              .post(Entity.entity(newLabels, MediaType.APPLICATION_XML));
			}
			catch (ProcessingException pe) {
				System.err.println("Error during JAX-RS request processing");
				throw new ServiceException(pe);
			}
			catch (WebApplicationException wae) {
				System.err.println("Server returned error: hosts");
				throw new ServiceException(wae);
			}
			catch (Exception e) {
				System.err.println("Unexpected exception");
				throw new ServiceException(e);
			}			
		}
		
		// Create relationship between nodes and hosts
		for (NodeReader node_r: nffg_r.getNodes())
		{
			if (node_r.getHost() == null) continue;
			
			// Retrieve source and destination node id from nodeMap
			String srcNodeID = nodeMap.get( node_r.getName() );
			String dstNodeID = hostMap.get( node_r.getHost().getName() );
			
			// Create a new relationship object
			Relationship newRelationship = objFactory.createRelationship();
			newRelationship.setDstNode(dstNodeID);
			newRelationship.setType("AllocatedOn");
			
			// Call Neo4JSimpleXML API
			try {
				Relationship res = target.path("data/node/" + srcNodeID + "/relationships")
						                 .request(MediaType.APPLICATION_XML)
						                 .post(Entity.entity(newRelationship, MediaType.APPLICATION_XML), Relationship.class);
			}
			catch (ProcessingException pe) {
				System.err.println("Error during JAX-RS request processing");
				throw new ServiceException(pe);
			}
			catch (WebApplicationException wae) {
				System.err.println("Server returned error: allocation");
				throw new ServiceException(wae);
			}
			catch (Exception e) {
				System.err.println("Unexpected exception");
				throw new ServiceException(e);
			}
		}
		
		// Set this nffg as successfully loaded
		this.nffgLoaded = nffgName;
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
		// Check if valid argument has been passed
		if (nffgName == null)
			throw new UnknownNameException("Argument nffgName can't be null");
		
		// Check if nffg exists
		nffg_r = monitor.getNffg(nffgName);
		if (nffg_r == null)
			throw new UnknownNameException("Unknown nffgName, retreiving nffg failed");
		
		// Check if already loaded
		return (nffgName == nffgLoaded) ? true : false;
	}
	
	private void deleteAllNodes() throws ServiceException
	{
		// Delete all nffg-nodes
		for (String value: nodeMap.values())
			deleteNode(value);
		
		// Delete all hosts
		for (String value: hostMap.values())
			deleteNode(value);
	}
	
	private void deleteNode(String nodeID) throws ServiceException
	{
		// Call Neo4JSimpleXML API
		try {
			Response res = target.path("data/node/" + nodeID)
					             .request().delete();
			
			System.out.println("Codice HTTP: " + res.getStatus());
		}
		catch (ProcessingException pe) {
			System.err.println("Error during JAX-RS request processing");
			throw new ServiceException(pe);
		}
		catch (WebApplicationException wae) {
			System.err.println("Server returned error");
			throw new ServiceException(wae);
		}
		catch (Exception e) {
			System.err.println("Unexpected exception");
			throw new ServiceException(e);
		}
	}
	
	private void loadNodes(String type) throws ServiceException
	{
		for (NodeReader node_r: nffg_r.getNodes())
		{
			String nodeName = node_r.getName();
			
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
				
				nodeMap.put(nodeName, res.id);
				
				Response res2 = target.path("data/node/" + res.id + "/labels")
						              .request(MediaType.APPLICATION_XML)
						              .post(Entity.entity(newLabels, MediaType.APPLICATION_XML));
			}
			catch (ProcessingException pe) {
				System.err.println("Error during JAX-RS request processing");
				throw new ServiceException(pe);
			}
			catch (WebApplicationException wae) {
				System.err.println("Server returned error");
				throw new ServiceException(wae);
			}
			catch (Exception e) {
				System.err.println("Unexpected exception");
				throw new ServiceException(e);
			}
		}
	}
	
	private void loadRelationships(String type) throws ServiceException
	{
		
	}

}
