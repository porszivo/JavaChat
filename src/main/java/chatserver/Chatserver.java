package chatserver;

import cli.Shell;
import listener.UserListenerTCP;
import listener.UserListenerUDP;
import model.UserMap;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;

	private Config config;
	private Config userConfig;
	private UserMap userMap;

	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private int tcpPortNumber;
	private int udpPortNumber;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
    private UserListenerTCP userListenerTCP;
	private UserListenerUDP userListenerUDP;


	private ExecutorService executorService;
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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userConfig = new Config("user");
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

			userListenerTCP = new UserListenerTCP(serverSocket, executorService, userMap);
			userListenerUDP = new UserListenerUDP(datagramSocket, executorService, userMap);
			executorService.execute(userListenerTCP);
			executorService.execute(userListenerUDP);

			shell = new Shell(componentName, userRequestStream, userResponseStream);
			shell.register(this);
			executorService.execute(shell);

		} catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port "
					+ " or listening for a connection");
			System.out.println(e.getMessage());
		}

		System.out.println("Server is running");


	}

	@Override
	public String users() throws IOException {

		return userMap.users();

	}

	@Override
	public String exit() throws IOException {

		try {

			if(serverSocket != null) serverSocket.close();
			if(datagramSocket != null) datagramSocket.close();

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

		// TODO: start the chatserver
		chatserver.run();

	}

}
