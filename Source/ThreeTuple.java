/**
 * ThreeTuple.java
 * 
 * ThreeTuple class defines a 3-tuple
 * 
 * Name: Sehwa Kim
 * UCID: 10044558
 * Date: February 17, 2022
 */

public class ThreeTuple<F, S, T> {
	private final F firstElement;
	private final S secondElement;
	private final T thirdElement;
	
	// constructor for ThreeTuple
	public ThreeTuple(F firstElement, S secondElement, T thirdElement) {
		this.firstElement = firstElement;
		this.secondElement = secondElement;
		this.thirdElement = thirdElement;
	}

	// get the first element
	public F first() {
		return firstElement;
	}

	// get the second element
	public S second() {
		return secondElement;
	}

	// get the third element
	public T third() {
		return thirdElement;
	}
}
