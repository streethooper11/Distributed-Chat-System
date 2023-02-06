/**
 * DataShared.java
 * 
 * This class contains objects and variables shared by multiple files
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: March 25, 2022
 */

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.time.LocalDateTime;

public class DataShared {
	// key is address of peer, value is its status: alive, silent, missing_ack; includes the client itself as alive
	private ConcurrentHashMap<String, String> all_peers = new ConcurrentHashMap<>();
	
	// keeps track of peers that are alive; key is IP and port address, and value is the last time a peer or a snip message was received from the alive peer
	private ConcurrentHashMap<String, LocalDateTime> alive_peers = new ConcurrentHashMap<>();
	
	// Source: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentSkipListSet.html
	private ConcurrentSkipListSet<String> peer_new = new ConcurrentSkipListSet<>(); 

	private String self_source; // stores IP and port for self
	// keeps track of all peers sent via UDP in <destination, time> format
	private ArrayList<Pair<String, LocalDateTime>> peer_sent = new ArrayList<>();

	// keeps track of all peers received via UDP in <sender, received peer, time> format
	private ArrayList<ThreeTuple<String, String, LocalDateTime>> peer_recv = new ArrayList<>();

	// key is lamport timestamp, and the value is another map with key being snippet sender and value being snippet message
	private ConcurrentHashMap<Integer, ConcurrentHashMap<String, String>> snippets = new ConcurrentHashMap<>();
	
	// Iteration 3 Optional - A set that stores all acks received by this client, in <lamport time, peer address> Pair form
	private ConcurrentSkipListSet<Pair<Integer, String>> acks_recv = new ConcurrentSkipListSet<>();
	
	private AtomicInteger lamport_time = new AtomicInteger();
	
	private AtomicBoolean done = new AtomicBoolean(); // indicates the client should be done with UDP
	private boolean verbose; // indicates if verbose option is on
	
	private long keepalive_time;
	private long peersend_time;
	
	/**
	 * Checks if a peer is in the valid form
	 * A valid peer is in the form of X.X.X.X:Y, in which each X is an integer between 0 and 255, and Y is an integer between 0 and 65535
	 * 
	 * @param peer - a peer received
	 * @param verbose - indicates verbose mode
	 * 
	 * @return Boolean indicating whether the peer is valid or not
	 */
	public boolean isValidPeer(String peer) {
		try {
			String[] peer_ip_port = peer.split(":");
			
			if (peer_ip_port.length == 2) {
				// has the proper number of colons
				int portNum = Integer.parseInt(peer_ip_port[1]);

				if ((portNum >= 0) && (portNum <= 65535)) {
					// valid port number was given
					String[] peer_ip_nums = peer_ip_port[0].split(Pattern.quote("."));
					
					if (peer_ip_nums.length == 4) {
						// has the proper number of periods for IP address
						for (String each_numeric : peer_ip_nums) {
							int each_num = Integer.parseInt(each_numeric);
							
							if ((each_num < 0) || (each_num > 255)) {
								if (verbose) {
									System.out.println(peer + " is not a valid peer.");									
								}
								return false; // invalid numerical value in the address
							}
						}
						
						return true; // is a valid IP address
					}
				}
			}

			return false; // failed one of the tests
		}
		catch (NumberFormatException nfe) {
			return false; // invalid format of port number; not a valid peer
		}
	}
	
	// START of getter for all_peers
	public int get_allpeers_size() {
		return all_peers.size();
	}
	
	public Set<Map.Entry<String, String>> get_allpeers_entries() {
		return all_peers.entrySet();
	}
	
	// START of setter for all_peers
	// this setter will replace already existing peer
	public void add_allpeers(String peer, String status) {
		all_peers.put(peer, status);
	}
	
	// START of getter for alive_peers
	public ConcurrentHashMap.KeySetView<String, LocalDateTime> get_alivepeers_noTime() {
		return alive_peers.keySet();
	}

	public Set<Map.Entry<String, LocalDateTime>> get_alivepeers_withTime() {
		return alive_peers.entrySet();
	}
	
	public ArrayList<String> get_alivepeers_copy_noTime() {
		return new ArrayList<>(alive_peers.keySet());
	}
	
	public boolean check_alivepeer(String peer) {
		return alive_peers.containsKey(peer);
	}
	
	// START of setter for alive_peers
	public void add_alivepeers(String peer, LocalDateTime time) {
		alive_peers.put(peer, time);
	}
	
	public void rem_alivepeers(String peer) {
		alive_peers.remove(peer);
	}
	
	// START of getter for peer_new
	public ArrayList<String> get_peernew_copy() {
		return new ArrayList<>(peer_new);
	}
	
	// START of setter for peer_new
	public void add_peernew(String peer) {
		peer_new.add(peer);
	}
	
	public void rem_peernew(String peer) {
		peer_new.remove(peer);
	}
	
	// getter for self_source
	public String getSelf() {
		return self_source;
	}
	
	// setter for self_source
	public void setSelf(String result) {
		self_source = result;
	}

	// START of getter for peer_sent
	public int get_peersent_size() {
		return peer_sent.size();
	}
	
	public Pair<String, LocalDateTime> get_peersent(int location) {
		return peer_sent.get(location);
	}

	// START of setter for peer_sent
	public void add_peersent(String peer, LocalDateTime time) {
		peer_sent.add(new Pair<>(peer, time));
	}

	// START of getter for peer_recv
	public int get_peerrecv_size() {
		return peer_recv.size();
	}
	
	public ThreeTuple<String, String, LocalDateTime> get_peerrecv(int location) {
		return peer_recv.get(location);
	}
	
	// START of setter for peer_recv
	public void add_peerrecv(String sender, String message, LocalDateTime time) {
		peer_recv.add(new ThreeTuple<>(sender, message, time));
	}

	// START of getter for snippets
	public Collection<ConcurrentHashMap<String, String>> getSnip_all() {
		return snippets.values();
	}
	
	public boolean time_exists_inSnip(Integer timestamp) {
		return snippets.containsKey(timestamp);
	}
	
	public Set<Map.Entry<String, String>> getSnips_inTime(Integer timestamp) {
		return snippets.get(timestamp).entrySet();
	}
	
	public boolean time_has_sender_snip(Integer timestamp, String sender) {
		return snippets.get(timestamp).containsKey(sender);
	}
	
	// START of setter for snippets
	public void add_new_timestamp(Integer timestamp) {
		snippets.put(timestamp, new ConcurrentHashMap<>());
	}
	
	public void add_snippet(Integer timestamp, String sender, String message) {
		snippets.get(timestamp).put(sender, message);
	}

	// START of getter for acks_recv
	public int get_acksrecv_size() {
		return acks_recv.size();
	}
	
	public ConcurrentSkipListSet<Pair<Integer, String>> get_acksrecv() {
		return acks_recv.clone();
	}
	
	public boolean acksrecv_has_peer(Integer timestamp, String peer) {
		return acks_recv.contains(new Pair<>(timestamp, peer));
	}
	
	// START of setter for acks_recv
	public void add_acksrecv(Integer timestamp, String peer) {
		acks_recv.add(new Pair<>(timestamp, peer));
	}
	
	// getter for lamport_time
	public int getLamport() {
		return lamport_time.get();
	}
	
	// setter for lamport_time
	public void setLamport(int newTime) {
		lamport_time.set(newTime);
	}
	
	// increment lamport_time by 1
	public int incrementLamport() {
		return lamport_time.incrementAndGet();
	}
	
	// getter for done
	public boolean getDone() {
		return done.get();
	}
	
	// setter for done
	public void setDone(boolean value) {
		done.set(value);
	}

	// getter for verbose
	public boolean getVerbose() {
		return verbose;
	}
	
	// setter for verbose
	public void setVerbose(boolean value) {
		verbose = value;
	}

	// getter for keepalive_time
	public long getKeepalive() {
		return keepalive_time;
	}
	
	// setter for keepalive_time
	public void setKeepalive(long time) {
		keepalive_time = time;
	}

	// getter for peersend_time
	public long getPeersend() {
		return peersend_time;
	}
	
	// setter for peersend_time
	public void setPeersend(long time) {
		peersend_time = time;
	}
}