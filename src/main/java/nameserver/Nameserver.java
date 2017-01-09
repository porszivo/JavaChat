package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

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

	private Registry registry;
	private ArrayList<INameserver> serverList;

	private String rmiHost;
	private int rmiPort;
	private String bindingName;
	private boolean root;
	private String domain;

	private Shell shell;

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

		serverList = new ArrayList<>();
		rmiHost = config.getString("registry.host");
		rmiPort = config.getInt("registry.port");
		bindingName = config.getString("root_id");

		if(!config.listKeys().contains("domain")) {
			root = true;
			domain = "root";
		} else {
			root = false;
			domain = config.getString("domain");
		}

	}

	@Override
	public void run() {
		try {
			registry = LocateRegistry.createRegistry(rmiPort);
			INameserver remote = (INameserver) UnicastRemoteObject.exportObject(this, 0);
			registry.bind(bindingName, remote);
		} catch (RemoteException e) {
			System.err.println("Error while starting");
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			System.err.println("Error while binding");
		}

		if(!domain.equals("root")) {
			Registry rootRegistry = null;
			try {
				rootRegistry = LocateRegistry.getRegistry(rmiHost, rmiPort);
				INameserver server = (INameserver) rootRegistry.lookup(bindingName);
			} catch (RemoteException e) {
				System.err.println("Error while starting");
				e.printStackTrace();
			} catch (NotBoundException e) {
				System.err.println("Error while looking for server-remote-object.");
			}
		}

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public String nameservers() throws IOException {
		String ret = "";
		for(INameserver ns : serverList) {
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

	@Override
	public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

	}
}
