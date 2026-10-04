package mt.edu.um.cps3230.ferry.domain;

/** What the weather means for a sailing. */
public enum SailingStatus {

    /** The crossing goes ahead. */
    SAILS,

    /** The crossing may yet be called off, and every contact has been told so. */
    AT_RISK,

    /** The crossing is called off. */
    CANCELLED
}
