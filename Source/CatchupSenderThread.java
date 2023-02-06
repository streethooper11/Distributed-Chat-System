/**
 * CatchupSenderThread.java
 * 
 * Thread responsible for sending a single catch-up snippet through UDP
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 26, 2022
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class CatchupSenderThread implements Runnable {
	private DatagramSocket udpSock;
	private String[] recv_address;
	private Integer timestamp;
	private String snippet_sender;
	private String snippet_content;

	/**
	 * Constructor
	 * 
	 * @param udpSock - UDP socket
	 * @param destination_peer - peer to send the catch-up snippet packet to
	 * @param timestamp - Lamport timestamp of the snippet to be sent
	 * @param snippet_sender - original sender of the snippet
	 * @param snippet_content - snippet content
	 */
	public CatchupSenderThread(DatagramSocket udpSock, String destination_peer, Integer timestamp, String snippet_sender, String snippet_content) {
		this.udpSock = udpSock;
		this.recv_address = destination_peer.split(":", 2);
		this.timestamp = timestamp;
		this.snippet_sender = snippet_sender;
		this.snippet_content = snippet_content;
	}
	
	/**
	 * Use existing udp socket to create a new datagram packet and send a snippet
	 */
	public void run() {
		try {
			byte[] buffer = new byte[100];
			
			buffer = ("ctch" + snippet_sender + " " + timestamp + " " + snippet_content).getBytes(); // follow ctch format
			
			DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, 
					InetAddress.getByName(recv_address[0]), Integer.valueOf(recv_address[1]));
			udpSock.send(sendPacket);
		}
		catch (SocketException se) {
			// socket is closed
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
