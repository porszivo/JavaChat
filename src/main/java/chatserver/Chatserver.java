package chatserver;

import cli.Command;
import cli.Shell;
import listener.ServerListenerTCP;
import listener.ServerListenerUDP;
import model.UserMap;
import nameserver.INameserverForChatserver;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;

	private Config config;
	private Config userConfig;
	private Config nsConfig;
	private UserMap userMap;

	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private int tcpPortNumber;
	private int udpPortNumber;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
    private ServerListenerTCP serverListenerTCP;
	private ServerListenerUDP serverListenerUDP;


	private ExecutorService executorService;
	private Shell shell;

	/** Add for Lab2 */
	private INameserverForChatserver rootNameserver;

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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userConfig = new Config("user");
		this.nsConfig = new Config("ns-root");
		userMap = new UserMap(userConfig);

		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.tcpPortNumber = this.config.getInt("tcp.port");
		this.udpPortNumber = this.config.getInt("udp.port");

		executorService = Executors.newCachedThreadPool();

	}

	@Override
	public void run() {

		try {
			serverSocket = new ServerSocket(tcpPortNumber);
			datagramSocket = new DatagramSocket(udpPortNumber);

			serverListenerTCP = new ServerListenerTCP(serverSocket, executorService, userMap);
			serverListenerUDP = new ServerListenerUDP(datagramSocket, executorService, userMap);
			executorService.execute(serverListenerTCP);
			executorService.execute(serverListenerUDP);

			shell = new Shell(componentName, userRequestStream, userResponseStream);
			shell.register(this);
			executorService.execute(shell);

		} catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port "
					+ " or listening for a connection");
			System.out.println(e.getMessage());
		}

		try {
			Registry registry = LocateRegistry.getRegistry(nsConfig.getString("registry.host"),
					nsConfig.getInt("registry.port"));

			rootNameserver = (INameserverForChatserver) registry.lookup(nsConfig.getString("root_id"));

		} catch(RemoteException e) {
			System.err.println("Error while obtaining registry/server-remote-object.");
		} catch(NotBoundException e) {
			System.err.println("Error while looking for server-remote-object.");
		}

		System.out.println("Server is running");

	}

	@Command
	@Override
	public String users() throws IOException {

		return userMap.users();

	}

	@Command
	@Override
	public String exit() throws IOException {

		try {


			if(serverSocket != null) serverSocket.close();
			serverListenerUDP.exit();
			if(datagramSocket != null) datagramSocket.close();

			userRequestStream.close();
			userResponseStream.close();

			userMap = new UserMap(userConfig);

			executorService.shutdown();

			shell.close();

			try {
				if(!executorService.awaitTermination(100, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}



		} catch (IOException e) {

			System.out.println(e.getMessage());

		}

		return null;

	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);

		chatserver.run();

	}

}
