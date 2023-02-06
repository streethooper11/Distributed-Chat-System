/**
 * CPSC 559 Project Iteration - Required + Optional
 * 
 * P3_opt.java: main file
 * 
 * Changes from Iteration 2
 * - Send ack on stop message and deal with multiple stop messages
 * - Send all snippets that the client has saved to new (alive) / re-activated peers
 * - Send ack on receiving a snip message
 * - Add received acks to the report
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 26, 2022
 */

import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class P3_opt {
	public static final String DEFAULT_IP_ADDRESS = "136.159.5.22";
	public static final int DEFAULT_PORT_NUMBER = 12955;
	public static final String DEFAULT_TEAM_NAME = "Sehwa Kim";
	public static final long KEEPALIVE_TIME = 180; // 180 seconds for keepalive
	public static final long PEERSEND_TIME = 60; // interval of sending each peer message in seconds

	private String registry_address;
	private LocalDateTime registry_recv_time;
	private HashSet<String> registry_peers; // keeps peer list sent by registry; can include the client itself
	private DatagramSocket udpSock; // socket for UDP communication
	private DataShared shared; // contains shared objects and variables
	
	/**
	 * Connects to the server and sends response based on the request of the server
	 * 
	 * @param ip_address IP address of the server
	 * @param port Port number of the server
	 * @param name Team name
	 * @param udpPort port address of the udp socket
	 * 
	 * @throws IOException in case of problems during creation of the socket or connection to server
	 * 
	 * Modified from Quiz02Server's start function
	 */
	private void connect_Registry(String server_ip_address, int server_port, String name) throws IOException {
		System.out.println("Connecting to " + server_ip_address + ":" + server_port + "...");
		Socket sock = new Socket(server_ip_address, server_port); // tcp connect to server
		
		if (!shared.getDone()) { // this indicates this connection is at the start of the client
			shared.setSelf(sock.getLocalAddress().getHostAddress() + ":" + shared.getSelf()); // prepend IP to udp port number to save self address
			registry_address = server_ip_address + ":" + server_port; // save registry address
		}
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		
		// Modified from DictionaryClient.java example
		String message = in.readLine(); // get message from server
		
		while (!message.equals("close")) {
			switch (message) {
			case "get team name":
				System.out.println("Sending the team name...");
				out.write(name + "\n");
				out.flush();
				System.out.println("Successfully sent the team name");
				break;
			case "get code":
				sendCode(out);
				break;
			case "receive peers":
				receivePeers(in);
				break;
			case "get report":
				sendReport(out);
				break;
			case "get location":
				System.out.println("Sending the location...");
				if (shared.getVerbose()) {
					System.out.println("Sending location: " + shared.getSelf());
				}
				out.write(shared.getSelf() + "\n");
				out.flush();
				System.out.println("Successfully sent the location");
				break;
			}
			
			message = in.readLine(); // get the next message
		}
		
		shared.add_allpeers(shared.getSelf(), "alive"); // add the client itself to the list of peers

		System.out.println("Closing connection with the Registry...");
		try {
			Thread.sleep(1000);
		}
		catch (Exception e) {
		}
		finally {
			out.close();
			in.close();
			sock.close();
		}
	}
	
	/**
	 * Sends source code to the server
	 * 
	 * @param out stream to send message to the server
	 * 
	 * @throws IOException in case of problems while communicating with the server
	 * @throws FileNotFoundException if the source file is not found; should not happen normally
	 */
	private void sendCode(BufferedWriter out) throws IOException, FileNotFoundException {
		System.out.println("Sending the source code...");

		BufferedReader source_file;
		String codeLine;
		
		out.write("Java\n"); // written using Java

		source_file = new BufferedReader(new FileReader("P3_opt.java"));
		codeLine = source_file.readLine();
		// null is found when the EOF is reached, so read and send lines until then
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("MessageManagerThread.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("SnippetThread.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("SnipSenderThread.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("CatchupSenderThread.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("PeerSenderThread.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("PeerManagerThread.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("DataShared.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n"); // add newline character as readLine removes it
			codeLine = source_file.readLine(); // read the next line
		}
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("Pair.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n");
			codeLine = source_file.readLine();
		}		
		out.write("\n");
		source_file.close();
		
		source_file = new BufferedReader(new FileReader("ThreeTuple.java"));
		codeLine = source_file.readLine();
		while (codeLine != null) {
			out.write(codeLine + "\n");
			codeLine = source_file.readLine();
		}		
		source_file.close();
		
		out.write("...\n"); // send end_of_code
		out.flush();		
		
		System.out.println("Successfully sent the source code");
	}
	
	/**
	 * Receives peer information from the server
	 * 
	 * @param ip_address IP address of current server
	 * @param port port number of current server
	 * @param in input stream to receive message from the server
	 * 
	 * @throws IOException in case of problems while communicating with the server
	 */
	private void receivePeers(BufferedReader in) throws IOException {
		System.out.println("Receiving a peer list...");
		
		registry_peers = new HashSet<>();
		registry_recv_time = LocalDateTime.now();
		
		String message = in.readLine(); // get number of peers
		int numPeers = Integer.parseInt(message);
		
		System.out.println("Number of peers: " + numPeers);
		
		for (int i = 0; i < numPeers; i++) {
			// get IP:port of a peer and add to the registry peer list and all peer list
			message = in.readLine();
			
			if (message.charAt(0) == '/') {
				message = message.substring(1); // try filtering out forward slash in case InetAddress was given
			}
			
			if (shared.isValidPeer(message)) {
				registry_peers.add(message);
				
				if (!message.equals(shared.getSelf())) {
					shared.add_allpeers(message, "alive"); // assume peers from the registry to be alive at first
					shared.add_alivepeers(message, registry_recv_time);
				}
			}
		}
		
		System.out.println("Successfully received the peer list");

		if (shared.getVerbose()) {
			// print received peers
			System.out.println("Received peers from the registry:");
			for (String eachPeer : registry_peers) {
				System.out.println(eachPeer);
			}		
		}
	}

	/**
	 * Sends report to the server
	 * 
	 * @param out output stream to send report to the server
	 * 
	 * @throws IOException in case of problems while communicating with the server
	 */
	private void sendReport(BufferedWriter out) throws IOException {
		System.out.println("Sending the report...");

		// Usage of DateTimeFormatter from https://www.javatpoint.com/java-get-current-date
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		
		out.write(String.valueOf(shared.get_allpeers_size()) + "\n");
		for (Map.Entry<String, String> eachPeer : shared.get_allpeers_entries()) {
			out.write(eachPeer.getKey() + " " + eachPeer.getValue() + "\n"); // Iteration 3 Optional: State peer's address with the state			
		}

		if (registry_peers == null || registry_peers.isEmpty()) {
			out.write("0\n"); // peer list was not received by the registry; shouldn't happen normally
		}
		else {
			out.write("1" + "\n"); // 1 registry
			out.write(registry_address + "\n" + dtf.format(registry_recv_time) + "\n");
			
			// send the list of all peers from the registry with its numbers
			out.write(String.valueOf(registry_peers.size()) + "\n" );
			for (String reg_peer : registry_peers) {
				out.write(reg_peer + "\n");
			}			
		}
		
		int size;
		/**
		 * New section of report for Iteration 2
		 */
		size = shared.get_peerrecv_size();
		out.write(String.valueOf(size) + "\n");
		
		ThreeTuple<String, String, LocalDateTime> eachPeer_recv;
		for (int i = 0; i < size; i++) {
			eachPeer_recv = shared.get_peerrecv(i);
			out.write(eachPeer_recv.first() + " " + eachPeer_recv.second() + " " + dtf.format(eachPeer_recv.third()) + "\n");
		}
		
		size = shared.get_peersent_size();
		out.write(String.valueOf(size) + "\n");
		
		Pair<String, LocalDateTime> eachPeer_sent;
		for (int i = 0; i < size; i++) {
			eachPeer_sent = shared.get_peersent(i);
			out.write(eachPeer_sent.first() + " " + shared.getSelf() + dtf.format(eachPeer_sent.second()) + "\n");
		}
		
		int total_num_snips = 0;
		for (ConcurrentHashMap<String, String> eachTimestamp : shared.getSnip_all()) {
			total_num_snips += eachTimestamp.size();
		}
		out.write(String.valueOf(total_num_snips) + "\n");
		
		for (Integer i = 0; i.intValue() <= shared.getLamport(); i++) {
			if (shared.time_exists_inSnip(i)) {
				// Usage of entrySet from https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html#entrySet--
				for (Map.Entry<String, String> eachSnip : shared.getSnips_inTime(i)) {
					out.write(i + " " + eachSnip.getValue() + " " + eachSnip.getKey() + "\n");										
				}
			}
		}
		
		/**
		 * New section of report for Iteration 3 - Optional
		 */
		size = shared.get_acksrecv_size();
		out.write(String.valueOf(size) + "\n");
		
		System.out.println("Sent size: " + size);
		
		for (Pair<Integer, String> eachAck : shared.get_acksrecv()) {
			out.write(String.valueOf(eachAck.first()) + " " + eachAck.second() + "\n");
			System.out.println("Sent Ack: " + eachAck.first() + " " + eachAck.second());
		}

		out.flush();
		
		System.out.println("Successfully sent the report");
	}
	
	/**
	 * Runs the client program. IP address, port number and team name can be given as arguments
	 * Arguments are accepted as IP address, port number, and team name in that order
	 * 
	 * @param args - IP address and port number, respectively
	 */
	public static void main(String[] args) {
		P3_opt client = new P3_opt();
		client.shared = new DataShared();

		String server_ip_address = DEFAULT_IP_ADDRESS; // Default IP address of server
		int server_port = DEFAULT_PORT_NUMBER; // Default port number of server
		String name = DEFAULT_TEAM_NAME; // Default team name
		
		client.shared = new DataShared();

		client.shared.setKeepalive(KEEPALIVE_TIME);
		client.shared.setPeersend(PEERSEND_TIME);
		client.shared.setVerbose(false); // not verbose initially
		
		boolean ipSet = false;
		boolean portSet = false;
		boolean nameSet = false;
		boolean sendTimeSet = false;
		boolean keepAliveTimeSet = false;
		String argument;
		// break down arguments
		for (int index = 0; index < args.length; index++) {
			argument = args[index];
			
			switch(argument) {
			case "--ip":
				if (!ipSet) {
					ipSet = true;
					index++;
					if (index < args.length) {
						server_ip_address = args[index];
					}
				}
				break;
			case "--port":
				if (!portSet) {
					portSet = true;
					index++;
					if (index < args.length) {
						try {
							int portNum = Integer.parseInt(args[index]);
							if ((portNum >= 0) && (portNum <= 65535)) {
								server_port = portNum; // only copy if port number is in range
							}
						}
						catch (NumberFormatException nfe) {
							// port number is not a valid number
							System.out.println("Invalid port number. Using default port...");
						}
					}
				}
				break;
			case "--name":
				if (!nameSet) {
					nameSet = true;
					index++;
					if (index < args.length) {
						name = args[index];
					}
				}
				break;
			case "--v":
				if (!client.shared.getVerbose()) {
					client.shared.setVerbose(true);
				}
				break;
			case "--psend":
				if (!sendTimeSet) {
					sendTimeSet = true;
					index++;
					if (index < args.length) {
						try {
							client.shared.setPeersend(Integer.parseInt(args[index]));
						}
						catch (NumberFormatException nfe) {
							// peer send time is not a valid number
							System.out.println("Invalid peer send time. Using default time...");
						}
					}
				}
				break;
			case "--alive":
				if (!keepAliveTimeSet) {
					keepAliveTimeSet = true;
					index++;
					if (index < args.length) {
						try {
							client.shared.setKeepalive(Integer.parseInt(args[index]));
						}
						catch (NumberFormatException nfe) {
							// keepalive time is not a valid number
							System.out.println("Invalid keepAlive time. Using default time...");
						}
					}
				}
				break;
			default:
				break;
			}					
		}
		
		try {
			client.udpSock = new DatagramSocket();
			client.shared.setSelf(String.valueOf(client.udpSock.getLocalPort())); // save port number of the udp socket
			client.shared.setLamport(0);
			client.shared.setDone(false);
			
			client.connect_Registry(server_ip_address, server_port, name); // for starting
			
			if (client.shared.getVerbose()) {
				System.out.println("Successfully connected as: " + client.shared.getSelf());				
			}
			
			Thread thread_m = new Thread(new MessageManagerThread(client.udpSock, name, client.shared));
			thread_m.start();
			Thread thread_s = new Thread(new SnippetThread(client.udpSock, client.shared));
			thread_s.start();
			Thread thread_ps = new Thread(new PeerSenderThread(client.udpSock, client.shared));
			thread_ps.start();
			Thread thread_pm = new Thread(new PeerManagerThread(client.shared));
			thread_pm.start();

			thread_ps.join();
			thread_pm.join();

			client.runStopTimer(); // Iteration 3 - Required
			thread_s.join();
			thread_m.join();
			
			client.shared.setDone(true); // really done now
			client.connect_Registry(server_ip_address, server_port, name); // for ending
			System.exit(0); // quit program
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Iteration 3 - Required
	 * Creates a thread that wait for stop message from registry
	 * If a stop message from registry is not received for 15 seconds, this thread will close the socket and end itself
	 */
	private void runStopTimer() {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			private int timeCount = 0;
			
			public void run() {
				if (shared.getDone()) { // RecvSendMsgThread set this true after receiving stop from registry
					shared.setDone(false);
					timeCount = 0; // reset count
				}
				else {
					timeCount++; // increase count
					
					if (timeCount == 3) { // 15 seconds passed since the last stop message, so assume registry received ack
						timer.cancel();
						udpSock.close();
					}
				}
			}
		};
		
		timer.schedule(task, 0, 5000);
	}
}
