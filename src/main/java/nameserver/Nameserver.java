package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

import cli.Command;
import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable, INameserver {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;

	private String bindingName;
	private String rmiHost;
	private int rmiPort;
	private String domain;

	private Registry registry;

	private HashMap<String, INameserver> childNameserver;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Nameserver(String componentName, Config config,
					  InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		// TODO
		this.bindingName = config.getString("root_id");
		this.rmiHost = config.getString("registry.host");
		this.rmiPort = config.getInt("registry.port");
		if(config.listKeys().contains("domain")) {
			this.domain = config.getString("domain");
		} else {
			this.domain = "root";
		}

		childNameserver = new HashMap<>();

	}

	@Override
	public void run() {
		// TODO
		if(domain.equals("root")) {
			try {
				registry = LocateRegistry.createRegistry(rmiPort);
				INameserver remote = (INameserver) UnicastRemoteObject.exportObject(this, 0);
				registry.bind(bindingName, remote);
			} catch (RemoteException e) {
				System.out.println("Error while trying to create Registry");
				e.printStackTrace();
			} catch (AlreadyBoundException e) {
				System.out.println("Error while binding remote object.");
				e.printStackTrace();
			}
		} else {
			try {
				registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
			} catch (RemoteException e) {
				System.out.println("Error while trying to get Registry");
				e.printStackTrace();
			}
		}

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);

		new Thread(shell).start();

	}

	@Command
	@Override
	public String nameservers() throws IOException {
		String ret = "";
		for (String ns : childNameserver.keySet()) {
			ret += ns + "\n";
		}
		return ret;
	}

	@Override
	public String addresses() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		// TODO: start the nameserver
		new Thread(nameserver).start();
	}

	@Override
	public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

	}

	@Override
	public INameserverForChatserver getNameserver(String zone) throws RemoteException {
		return null;
	}

	@Override
	public String lookup(String username) throws RemoteException {
		return null;
	}


	// vienna.at
	// de
	@Override
	public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
		String[] parts = domain.split(".");
		if(parts.length==1) {
			if(!childNameserver.containsKey(domain)) {
				childNameserver.put(domain, nameserver);
			} else {
				throw new AlreadyRegisteredException(domain + " is already registered.");
			}
		} else {
			if(!childNameserver.containsKey(parts[parts.length-1])) {
				throw new InvalidDomainException(domain + " is not a valid domain.");
			}
			INameserver nServer = childNameserver.get(parts[parts.length-1]);
			domain = domain.replace("." + parts[parts.length-1], "");
			nameserver.registerNameserver(domain, nServer, null);
		}

	}
}