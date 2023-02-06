/**
 * PeerSenderThread.java
 * 
 * Thread responsible for sending out peer messages
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 26, 2022
 */

import java.util.ArrayList;
import java.util.Random;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;

public class PeerSenderThread implements Runnable {
	private DatagramSocket udpSock;
	private DataShared shared;
	
	/**
	 * Constructor
	 * 
	 * @param udpSock - UDP Socket
	 * @param shared - Data structure shared
	 */
	public PeerSenderThread(DatagramSocket udpSock, DataShared shared) {
		this.udpSock = udpSock;
		this.shared = shared;
	}
	
	/**
	 * The thread periodically sends peer messages
	 */
	public void run() {
		String[] peer_ip_port;
		byte[] buffer = new byte[100];
		DatagramPacket sendPacket;
		LocalDateTime currentTime;
		Random rand = new Random();
		int randomIndex;
		ArrayList<String> alive_peers_string, new_peers_string;

		try {
			while (!shared.getDone()) {
				alive_peers_string = shared.get_alivepeers_copy_noTime();
				new_peers_string = shared.get_peernew_copy();
				currentTime = LocalDateTime.now();

				if (!alive_peers_string.isEmpty()) {
					// usage of Random from Registry.java
					do {
						randomIndex = rand.nextInt(alive_peers_string.size());
					} while (alive_peers_string.get(randomIndex).equals(shared.getSelf())); // pick something that's not itself

					if (shared.getVerbose()) {
						for (String eachPeer : alive_peers_string) {
							System.out.println("Peer in list: " + eachPeer);
						}							
					}
					
					buffer = ("peer" + alive_peers_string.get(randomIndex)).getBytes(); // follow peer format

					if (shared.getVerbose()) {
						System.out.println("Sending peer: " + alive_peers_string.get(randomIndex));							
					}
				}
				else {
					// send one from a new peer list as there are no peers alive
					if (!new_peers_string.isEmpty()) {
						randomIndex = rand.nextInt(new_peers_string.size());

						buffer = ("peer" + new_peers_string.get(randomIndex)).getBytes(); // follow peer format

						if (shared.getVerbose()) {
							System.out.println("Sending peer: " + new_peers_string.get(randomIndex));							
						}
					}
				}
				
				// create packet and send to every alive peer
				for (String eachPeer : alive_peers_string) {
					peer_ip_port = eachPeer.split(":");
					sendPacket = new DatagramPacket(buffer, buffer.length, 
							InetAddress.getByName(peer_ip_port[0]), Integer.parseInt(peer_ip_port[1]));
					udpSock.send(sendPacket);
					shared.add_peersent(eachPeer, currentTime);
				}

				// send to peers recently received by other peers and not considered alive yet
				for (String new_peer : new_peers_string) {
					if (!alive_peers_string.contains(new_peer)) {
						peer_ip_port = new_peer.split(":");
						sendPacket = new DatagramPacket(buffer, buffer.length, 
								InetAddress.getByName(peer_ip_port[0]), Integer.parseInt(peer_ip_port[1]));
						udpSock.send(sendPacket);

						shared.add_peersent(new_peer, currentTime);
					}
					shared.rem_peernew(new_peer);
				}

				Thread.sleep(shared.getPeersend()*1000);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
