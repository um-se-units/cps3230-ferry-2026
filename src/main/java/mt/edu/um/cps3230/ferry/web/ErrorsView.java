package mt.edu.um.cps3230.ferry.web;

import java.util.List;

/** The body every failed request answers with. */
public record ErrorsView(List<String> errors) {

    public static ErrorsView of(String message) {
        return new ErrorsView(List.of(message == null ? "the request could not be handled" : message));
    }
}
