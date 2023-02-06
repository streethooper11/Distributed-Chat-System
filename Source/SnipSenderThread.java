/**
 * SnipSenderThread.java
 * 
 * Thread responsible for sending snippets to other peers and checking ack status
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 26, 2022
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SnipSenderThread implements Runnable {
	private DatagramSocket udpSock;
	private String peer;
	private String snip;
	private int lamport_time;
	private DataShared shared;
	
	/**
	 * Constructor
	 * 
	 * @param udpSock - UDP Socket
	 * @param peer - peer to send snippet to
	 * @param snip - snippet to send
	 * @param shared - Data structure shared
	 */
	public SnipSenderThread(DatagramSocket udpSock, String peer, String snip, int lamport_time, DataShared shared) {
		this.udpSock = udpSock;
		this.peer = peer;
		this.snip = snip;
		this.lamport_time = lamport_time;
		this.shared = shared;
	}
	
	public void run() {
		int counter = 0;
		int timeLimit = 30; // 30 seconds of time limit
		byte[] buffer = ("snip" + lamport_time + " " + snip).getBytes(); // follow snippet format
		DatagramPacket sendPacket;
		String[] peer_ip_port = peer.split(":", 2);

		try {
			do {
				if ((counter % 10) == 0) { // send snip message immediately, then every 10 seconds
					sendPacket = new DatagramPacket(buffer, buffer.length, 
							InetAddress.getByName(peer_ip_port[0]), Integer.parseInt(peer_ip_port[1]));
					udpSock.send(sendPacket);
				}
				
				Thread.sleep(1000);
				
				// check if the snip was acked every second
				if (shared.acksrecv_has_peer(lamport_time, peer)) {
					return; // done here since snip was acked
				}
				
				counter++;
			} while ((counter < timeLimit) && (!shared.getDone()));

			// either time limit is reached or the done flag is set up without ack response
			shared.add_allpeers(peer, "missing_ack");
			shared.rem_alivepeers(peer);
		}
		catch (InterruptedException ie) {
			// thread was interrupted; finish the ack response check immediately
			if (!shared.acksrecv_has_peer(lamport_time, peer)) {
				shared.add_allpeers(peer, "missing_ack");
				shared.rem_alivepeers(peer);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
