/**
 * MessageManagerThread.java
 * 
 * Thread responsible for receiving packets through UDP and acting accordingly
 * SocketException will be thrown when socket is closed while trying to receive a packet
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 26, 2022
 */

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Map;

import java.time.LocalDateTime;

public class MessageManagerThread implements Runnable {
	private DatagramSocket udpSock;
	private String name;
	private DataShared shared;
	
	/**
	 * Constructor
	 * 
	 * @param udpSock - UDP Socket
	 * @param name - Team name
	 * @param shared - Data structure shared
	 */
	public MessageManagerThread(DatagramSocket udpSock, String name, DataShared shared) {
		this.udpSock = udpSock;
		this.name = name;
		this.shared = shared;
	}
	
	/**
	 * The thread receives a datagram packet
	 * If stop is received, it indicates that the peer should be done
	 * For Iteration 3: stop message sends ack to the registry, and should keep doing it until the registry actually receives ack
	 * If snip is received, it saves it into the list of snippets received
	 * If peer is received, it saves it into the list of peers received
	 * If ctch is received, it saves appropriate information into the list of snippets received if not a duplicate timestamp and sender
	 * If ack is received, it saves the message and the sender to the list of acks received
	 */
	public void run() {
		ExecutorService executor = Executors.newFixedThreadPool(10); // Use of ExecutorService from Registry of the project

		try {
			String command, sender_peer, udp_message, recv_message;
			DatagramPacket recvPacket;
			
			byte[] buffer = new byte[100];
			
			recvPacket = new DatagramPacket(buffer, buffer.length);
			
			while (!shared.getDone()) {
				udpSock.receive(recvPacket);
				udp_message = new String(recvPacket.getData(), 0, recvPacket.getLength());
				
				if (udp_message.length() >= 4) {
					if (udp_message.equalsIgnoreCase("stop")) {
						if (shared.getVerbose()) {
							System.out.println("The registry sent a stop message.");
						}
						sendStopAck(buffer, recvPacket.getAddress().getHostAddress(), recvPacket.getPort(), name); // send ack
						shared.setDone(true); // done if the registry sent a stop message
					}
					else {
						command = udp_message.substring(0, 4);
						sender_peer = recvPacket.getAddress().getHostAddress() + ":" + recvPacket.getPort(); // sender
						
						if (shared.getVerbose()) {
							System.out.println("Command received by " + sender_peer + ": " + command);
						}
						
						recv_message = udp_message.substring(4);
						if (!recv_message.isEmpty()) { // ignore empty peer/snip/ctch message as they are not valid
							if (command.equalsIgnoreCase("snip")) {
								if (shared.check_alivepeer(sender_peer)) {
									// Iteration 4 change - Only add snippets from peers considered alive
									deliverSnippet(buffer, sender_peer, recv_message);									
								}
							}
							else if (command.equalsIgnoreCase("peer")) {
								addPeer(sender_peer, recv_message, executor);
							}
							else if (command.equalsIgnoreCase("ctch")) { // Iteration 3 Optional
								deliverCatchUp(recv_message);
							}
							else if (command.equalsIgnoreCase("ack ")) { // Iteration 3 Optional
								addAckMessage(sender_peer, recv_message);
							}
						}
					}

					recvPacket.setLength(buffer.length);						
				}
			}
			
			while (true) {
				// Iteration 3 Required - Handle possible future stop messages
				udpSock.receive(recvPacket);
				udp_message = new String(recvPacket.getData(), 0, recvPacket.getLength());
				
				if (udp_message.equals("stop")) { // the registry should be sending only the stop message
					if (shared.getVerbose()) {
						System.out.println("The registry sent a stop message.");
					}

					shared.setDone(true); // set the done flag again
					sendStopAck(buffer, recvPacket.getAddress().getHostAddress(), recvPacket.getPort(), name);
					recvPacket.setLength(buffer.length);						

					udpSock.receive(recvPacket); // wait for possible future stop messages	
					udp_message = new String(recvPacket.getData(), 0, recvPacket.getLength());
				}
			}
		}
		catch (SocketException se) {
			// Iteration 3 Required - the socket is closed because it had TCP connection with the registry; the thread should quit now
			try {
				executor.shutdownNow();
			}
			catch (Exception e) {
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace(); // IO exception except for SocketException
		}
	}
	
	/**
	 * Iteration 3 Required - send ack message in response to stop
	 * 
	 * @param buffer - buffer
	 * @param recv_IP - IP of the recipient
	 * @param recv_port - port number of the recipient
	 * @param name - the team name
	 * @throws SocketException
	 * @throws IOException
	 */
	private void sendStopAck(byte[] buffer, String recv_IP, int recv_port, String name) throws SocketException, IOException {
		buffer = ("ack" + name).getBytes(); // follow ack format
		DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(recv_IP), recv_port);
		udpSock.send(sendPacket);
	}
	
	/**
	 * Deliver snippet received
	 * Iteration 3 Optional - check duplicate snippet, and send ack message if not duplicate
	 * 
	 * @param buffer - buffer
	 * @param sender_peer - address of the sender of the snippet
	 * @param recv_message - received message
	 */
	private void deliverSnippet(byte[] buffer, String sender_peer, String recv_message) throws IOException {
		Integer timestamp;
		String[] string_parts = recv_message.split(" ", 2); // follow snip format to split into timestamp and content
		
		if (string_parts.length == 2) { // ignore bad snip message
			try {
				timestamp = Integer.valueOf(string_parts[0]);											
			}
			catch (NumberFormatException nfe) {
				return; // wrong timestamp format; skip this iteration
			}
			
			if (!shared.time_exists_inSnip(timestamp)) {
				shared.add_new_timestamp(timestamp);
			}
			
			if (!shared.time_has_sender_snip(timestamp, sender_peer)) { // ignore if the client already has a snippet sent by the sender peer in the indicated timestamp
				shared.add_snippet(timestamp, sender_peer, string_parts[1]);

				// follow algorithm given in lecture slides for lamport timestamp
				if (shared.getLamport() < timestamp.intValue()) {
					shared.setLamport(timestamp.intValue());
				}
				
				sendSnippetAck(buffer, timestamp, sender_peer); // Iteration 3 Optional - send ack
				shared.incrementLamport(); // increment lamport time upon delivery
				
				// print out all snippets received for the client if verbose
				if (shared.getVerbose()) {
					Integer backup_lamport = shared.getLamport(); // set value so it doesn't print out indefinitely if timestamp keeps increasing
					for (Integer i = 0; i.intValue() <= backup_lamport.intValue(); i++) {
						if (shared.time_exists_inSnip(i)) {
							// Usage of entrySet from https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html#entrySet--
							for (Map.Entry<String, String> eachSnip : shared.getSnips_inTime(i)) {
								System.out.println(i + " " + eachSnip.getValue() + " " + eachSnip.getKey() + "\n");										
							}
						}
					}			
				}
				else {
					System.out.println(timestamp + " " + string_parts[1] + " " + sender_peer); // only print the latest snip if not verbose
				}

				System.out.println(""); // put an empty line to separate next printout
			}
		}
	}
	
	/**
	 * Iteration 3 Optional
	 * Send Ack to the sender of the snippet
	 * 
	 * @param buffer - buffer
	 * @param timestamp - timestamp of the snippet
	 * @param sender_peer - address of the snippet sender
	 */
	private void sendSnippetAck(byte[] buffer, Integer timestamp, String sender_peer) throws IOException {
		String[] string_parts = sender_peer.split(":", 2);
		
		buffer = ("ack " + timestamp).getBytes(); // follow snippet ack format
		DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, 
				InetAddress.getByName(string_parts[0]), Integer.valueOf(string_parts[1]));
		udpSock.send(sendPacket);				
	}
	
	/**
	 * Add peer received and mark sender as active
	 * Iteration 3 Optional - send catch-up snippets appropriately
	 * 
	 * @param sender_peer - sender of the peer message
	 * @param recv_message - address of the received peer
	 * @param executor - executor service for sending catch-up snippets
	 */
	private void addPeer(String sender_peer, String recv_message, ExecutorService executor) {
		LocalDateTime currentTime = LocalDateTime.now();
		System.out.println("Received peer");
		if (!sender_peer.equals(shared.getSelf())) {
			if (!shared.check_alivepeer(sender_peer)) { // sender wasn't alive before
				if (shared.getVerbose()) {
					System.out.println(sender_peer + " was not alive before. Sending catchup snippets...");
				}
				sendCatchUp(sender_peer, executor); // Iteration 3 Optional
			}
		}

		if (recv_message.charAt(0) == '/') {
			recv_message = recv_message.substring(1);
		}
		
		shared.add_allpeers(sender_peer, "alive"); // sender is alive
		shared.add_alivepeers(sender_peer, currentTime); // update time

		if (shared.isValidPeer(recv_message)) {
			shared.add_peerrecv(sender_peer, recv_message, currentTime);

			if (!recv_message.equals(shared.getSelf())) {
				if (!shared.check_alivepeer(recv_message)) {
					 // Iteration 3 Optional - Only consider a new peer silent if it doesn't exist in the list of peers alive
					shared.add_allpeers(recv_message, "silent"); // assume received peer to be silent at first
					shared.add_peernew(recv_message); // it is in the list of new peers so a peer message will be sent
				}
			}

			if (shared.getVerbose()) {
				System.out.println("Peer received: " + recv_message); // print out which peer was received								
			}
		}
	}
	
	/**
	 * Iteration 3 Optional
	 * Send all catch-up snippets to a peer newly marked alive
	 * 
	 * @param sender_peer - sender of the peer message
	 * @param executor - executor service for sending catch-up snippets
	 */
	private void sendCatchUp(String sender_peer, ExecutorService executor) {
		Integer backup_lamport = shared.getLamport(); // set value so it doesn't send snippets indefinitely if timestamp keeps increasing
		for (Integer i = 0; i.intValue() <= backup_lamport.intValue(); i++) { // go through each timestamp, from 0 to the set value, and send all snippets
			if (shared.time_exists_inSnip(i)) {
				// Usage of entrySet from https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html#entrySet--
				for (Map.Entry<String, String> eachSnip : shared.getSnips_inTime(i)) {
					executor.execute(new CatchupSenderThread(udpSock, sender_peer, i, eachSnip.getKey(), eachSnip.getValue())); // new thread to send each catch-up snippets
				}
			}
		}
	}

	/**
	 * Iteration 3 Optional
	 * Deliver catch-up snippets received as long as it's not a duplicate
	 * 
	 * @param recv_message - information of received catch-up snippets
	 */
	private void deliverCatchUp(String recv_message) {
		try {
			String[] catchUpMsg = recv_message.split(" ", 3); // <sender> <timestamp> <content> in that order
			Integer timestamp = Integer.valueOf(catchUpMsg[1]);
			
			if (shared.isValidPeer(catchUpMsg[0])) { // make sure the original sender is in a valid peer format
				if (!shared.time_exists_inSnip(timestamp)) {
					shared.add_new_timestamp(timestamp);
				}
				
				if (!shared.time_has_sender_snip(timestamp, catchUpMsg[0])) { // ignore if the client already has a snippet sent by the sender peer in the indicated timestamp
					shared.add_snippet(timestamp, catchUpMsg[0], catchUpMsg[2]);

					if (shared.getLamport() < timestamp.intValue()) {
						shared.setLamport(timestamp.intValue()); // update lamport time
					}

					shared.incrementLamport(); // increment lamport time upon delivery

					if (shared.getVerbose()) {
						System.out.println("Added catch-up snippet");
					}
				}
				else {
					if (shared.getVerbose()) {
						System.out.println("Catch-up snippet already exists");
					}	
				}				
			}
		}
		catch (NumberFormatException nfe) {
			// wrong timestamp format
		}
	}

	/**
	 * Iteration 3 Optional
	 * Add received response ack of the snippet sent to the recent list of acks received
	 * 
	 * @param sender_peer - sender of the response ack
	 * @param recv_message - message; should only contain the timestamp
	 */
	private void addAckMessage(String sender_peer, String recv_message) {
		try {
			Integer timestamp = Integer.valueOf(recv_message);
			shared.add_acksrecv(timestamp, sender_peer);
		}
		catch (NumberFormatException nfe) {
			// wrong timestamp format
		}
	}
}
