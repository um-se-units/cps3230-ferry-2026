package mt.edu.um.cps3230.ferry.web;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Every time the API prints is ISO 8601 in Malta local time. */
public final class MaltaTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Malta");

    private MaltaTime() {
    }

    /**
     * @param moment a local time in Malta
     * @return the same moment as an ISO 8601 string with its offset
     */
    public static String format(LocalDateTime moment) {
        return moment.atZone(ZONE).toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
