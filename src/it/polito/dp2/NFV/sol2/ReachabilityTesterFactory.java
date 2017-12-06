package it.polito.dp2.NFV.sol2;

import it.polito.dp2.NFV.NfvReader;
import it.polito.dp2.NFV.NfvReaderException;
import it.polito.dp2.NFV.lab2.ReachabilityTester;
import it.polito.dp2.NFV.lab2.ReachabilityTesterException;

public class ReachabilityTesterFactory extends it.polito.dp2.NFV.lab2.ReachabilityTesterFactory
{
	private NfvReader monitor;

	@Override
	public ReachabilityTester newReachabilityTester() throws ReachabilityTesterException
	{
		it.polito.dp2.NFV.NfvReaderFactory factory = it.polito.dp2.NFV.NfvReaderFactory.newInstance();
		
		try {
			monitor = factory.newNfvReader();
		}
		catch(NfvReaderException nre) {
			System.err.println("Error while creating monitor");
			nre.printStackTrace();
			System.exit(1);
		}
		
		return new MyReachabilityTester(monitor);
	}

}
