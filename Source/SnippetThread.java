/**
 * SnippetThread.java
 * 
 * Thread responsible for entering and sending snippets to other peers
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 25, 2022
 */

import java.io.*;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnippetThread implements Runnable {
	private DatagramSocket udpSock;
	private DataShared shared;
	
	/**
	 * Constructor
	 * 
	 * @param udpSock - UDP Socket
	 * @param shared - Data structure shared
	 */
	public SnippetThread(DatagramSocket udpSock, DataShared shared) {
		this.udpSock = udpSock;
		this.shared = shared;
	}
	
	public void run() {
		try {
			ExecutorService executor = Executors.newFixedThreadPool(20); // Use of ExecutorService from Registry of the project
			
			// modified from Registry.java
			String command;
			
			// Using BufferedReader instead of Scanner for ready() method
			// Source: https://docs.oracle.com/javase/7/docs/api/java/io/BufferedReader.html#ready()
			BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
			Integer backup_lamport;
			
			while (!shared.getDone()) {
				while (!keyboard.ready()) {
					if (shared.getDone()) {
						keyboard.close();
						return; // done here; thread should end
					}
					Thread.sleep(100);
				}

				command = keyboard.readLine();
				backup_lamport = shared.incrementLamport(); // increments lamport by 1 and assigns the incremented value
				
				// create packet and send to peers that are alive
				for (String eachPeer : shared.get_alivepeers_noTime()) {
					executor.execute(new SnipSenderThread(udpSock, eachPeer, command, backup_lamport.intValue(), shared)); // make a thread to check for acks and resend snips
				}
				
				if (!shared.time_exists_inSnip(backup_lamport)) {
					shared.add_new_timestamp(backup_lamport);
				}
				
				shared.add_snippet(backup_lamport, shared.getSelf(), command); // add snippet to itself
				System.out.println(backup_lamport + " " + command + " " + shared.getSelf() + "\n"); // print the snip sent
			}
			
			keyboard.close();
			
			// Iteration 3 Optional - Shut down executor
			try {
				executor.shutdownNow();
			}
			catch (Exception e) {
				// exception happened; simply quit the thread
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
