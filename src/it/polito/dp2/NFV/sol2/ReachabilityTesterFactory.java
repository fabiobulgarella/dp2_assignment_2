package it.polito.dp2.NFV.sol2;

import it.polito.dp2.NFV.NfvReader;
import it.polito.dp2.NFV.NfvReaderException;
import it.polito.dp2.NFV.NfvReaderFactory;
import it.polito.dp2.NFV.lab2.ReachabilityTester;
import it.polito.dp2.NFV.lab2.ReachabilityTesterException;

public class ReachabilityTesterFactory extends it.polito.dp2.NFV.lab2.ReachabilityTesterFactory
{
	@Override
	public ReachabilityTester newReachabilityTester() throws ReachabilityTesterException
	{
		NfvReaderFactory factory = NfvReaderFactory.newInstance();
		String url = System.getProperty("it.polito.dp2.NFV.lab2.URL");
		NfvReader monitor;
		
		// Check if System Property has been read correctly
		if (url == null)
        {
			throw new ReachabilityTesterException("System property \"it.polito.dp2.NFV.lab2.URL\" not found");
        }
		
		try {
			monitor = factory.newNfvReader();
		}
		catch(NfvReaderException nre) {
			throw new ReachabilityTesterException(nre, "Error while creating monitor");
		}
		
		return new MyReachabilityTester(monitor, url);
	}

}
