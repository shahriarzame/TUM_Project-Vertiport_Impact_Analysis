package net.bhl.matsim.uam.router;

/**
 * Provides flight segment identifiers for MATSim network links. Currently uses two flight segments: horizontal (i.e.
 * cruise flight), which is the default flight segment, and vertical (i.e. take-off and landings). Can be extended to
 * various flight segments in the future.
 *
 * @author RRothfeld (Raoul Rothfeld)
 */
public class UAMFlightSegments_2 {
	//public static final String ATTRIBUTE = "type";

	//public static final String HORIZONTAL = UAMConstants.uam + "_horizontal";
	// static final String VERTICAL = UAMConstants.uam + "_vertical";


public enum FlightSegment {
	TAKE_OFF("Take Off", 60, 0, 5), // Duration: 60 seconds, Horizontal Speed: 0 m/s, Vertical Speed: 5 m/s (ascend)
	CLIMBING("Climbing", 300, 20, 3), // Duration: 300 seconds, Horizontal Speed: 20 m/s, Vertical Speed: 3 m/s (ascend)
	CRUISING("Cruising", 1200, 70, 0), // Duration: 1200 seconds, Horizontal Speed: 70 m/s, Vertical Speed: 0 m/s (steady flight)
	HOVERING("Hovering", 600, 0, 0), // Duration: 600 seconds, Horizontal Speed: 0 m/s, Vertical Speed: 0 m/s (stationary)
	DESCENDING("Descending", 300, 20, -3), // Duration: 300 seconds, Horizontal Speed: 20 m/s, Vertical Speed: -3 m/s (descend)
	LANDING("Landing", 60, 0, -5); // Duration: 60 seconds, Horizontal Speed: 0 m/s, Vertical Speed: -5 m/s (descend)


	private final String name;
	private final double duration; // in seconds
	private final double horizontalSpeed; // in meters per second
	private final double verticalSpeed; // in meters per second

	FlightSegment(String name, double duration, double horizontalSpeed, double verticalSpeed) {
		this.name = name;
		this.duration = duration;
		this.horizontalSpeed = horizontalSpeed;
		this.verticalSpeed = verticalSpeed;
	}

	// Getters
	public String getName() {
		return name;
	}

	public double getDuration() {
		return duration;
	}

	public double getHorizontalSpeed() {
		return horizontalSpeed;
	}

	public double getVerticalSpeed() {
		return verticalSpeed;
	}
}

}