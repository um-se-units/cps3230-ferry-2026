package mt.edu.um.cps3230.ferry.domain;

/** The two ferry terminals. */
public enum Terminal {

    CIRKEWWA("Ċirkewwa"),
    MGARR("Mġarr");

    private final String displayName;

    Terminal(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** The terminal at the other end of the crossing. */
    public Terminal opposite() {
        return this == CIRKEWWA ? MGARR : CIRKEWWA;
    }
}
