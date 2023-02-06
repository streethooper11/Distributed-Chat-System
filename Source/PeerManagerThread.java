/**
 * PeerManagerThread.java
 * 
 * Thread responsible for managing peer list
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 26, 2022
 */


import java.util.Map;
import java.time.Duration;
import java.time.LocalDateTime;

public class PeerManagerThread implements Runnable {
	private DataShared shared;
	
	/**
	 * Constructor
	 * 
	 * @param udpSock - UDP Socket
	 * @param shared - Data structure shared
	 */
	public PeerManagerThread(DataShared shared) {
		this.shared = shared;
	}
	
	/**
	 * The thread periodically checks the peers that are alive to see whether they timed out
	 * If a peer did not send a peer or a snip message for designated keepAlive time, the peer is considered dead
	 */
	public void run() {
		LocalDateTime currentTime;

		try {
			while (!shared.getDone()) {
				Thread.sleep(1000); // check every second
				currentTime = LocalDateTime.now();

				// Usage of entrySet from https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html#entrySet--
				// Usage of Duration from https://mkyong.com/java8/java-8-difference-between-two-localdate-or-localdatetime/
				for (Map.Entry<String, LocalDateTime> onePeer : shared.get_alivepeers_withTime()) {
					if (Duration.between(onePeer.getValue(), currentTime).toSeconds() > shared.getKeepalive()) {
						shared.rem_alivepeers(onePeer.getKey());
						shared.add_allpeers(onePeer.getKey(), "silent");
						
						System.out.println(onePeer.getKey() + " is now silent.");
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
