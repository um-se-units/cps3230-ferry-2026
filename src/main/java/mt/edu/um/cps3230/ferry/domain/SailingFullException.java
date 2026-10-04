package mt.edu.um.cps3230.ferry.domain;

/** Thrown when a sailing has no room left for the places a booking asks for. */
public class SailingFullException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public SailingFullException(String message) {
        super(message);
    }
}
