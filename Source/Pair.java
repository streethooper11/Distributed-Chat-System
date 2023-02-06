/**
 * Pair.java
 * 
 * Pair class used to store a pair of values
 * Created by Sehwa Kim for CPSC559 Project
 */

package cpsc501.A1;

// https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html
public class Pair<F extends Comparable<F>, S> implements Comparable<Pair<F, S>> {
	private final F firstElement;
	private final S secondElement;
	
	// constructor for pair
	public Pair(F firstElement, S secondElement) {
		this.firstElement = firstElement;
		this.secondElement = secondElement;
	}

	// get the first element
	public F first() {
		return firstElement;
	}

	// get the second element
	public S second() {
		return secondElement;
	}
	
	@Override
	public int compareTo(Pair<F, S> otherPair) {
		return firstElement.compareTo(otherPair.first());
	}
}
