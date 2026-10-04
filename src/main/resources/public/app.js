/*
 * The whole front end. No framework and no build step: the pages load this
 * file and call the one function they need.
 */
var ferry = (function () {
    "use strict";

    function byId(id) {
        return document.getElementById(id);
    }

    function show(element, text) {
        element.textContent = text === null || text === undefined ? "" : String(text);
    }

    function showErrors(element, body) {
        var errors = body && body.errors ? body.errors : ["Something went wrong."];
        show(element, errors.join(" "));
    }

    /** Sends a request and hands back {ok, body}. */
    function call(method, url, payload) {
        var options = { method: method, headers: { Accept: "application/json" } };
        if (payload !== undefined) {
            options.headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(payload);
        }
        return fetch(url, options).then(function (response) {
            return response.json()
                .catch(function () { return null; })
                .then(function (body) { return { ok: response.ok, body: body }; });
        });
    }

    function departureText(sailing) {
        return sailing.departsAt.replace("T", " ").slice(0, 16);
    }

    function queryValue(name) {
        return new URLSearchParams(window.location.search).get(name);
    }

    /* ----------------------------------------------------------- index.html */

    function startSearchPage() {
        var form = byId("search-form");
        var date = byId("search-date");
        var from = byId("search-from");
        var errors = byId("search-errors");
        var summary = byId("results-summary");
        var body = byId("results-body");

        date.value = new Date().toISOString().slice(0, 10);

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            show(errors, "");
            var url = "/api/sailings?date=" + encodeURIComponent(date.value)
                + "&from=" + encodeURIComponent(from.value);
            call("GET", url).then(function (answer) {
                body.replaceChildren();
                if (!answer.ok) {
                    showErrors(errors, answer.body);
                    show(summary, "No sailings were found.");
                    return;
                }
                answer.body.forEach(function (sailing) {
                    body.appendChild(sailingRow(sailing));
                });
                show(summary, answer.body.length + " sailings found.");
            });
        });
    }

    function sailingRow(sailing) {
        var row = document.createElement("tr");
        row.setAttribute("data-testid", "result-row");
        row.setAttribute("data-sailing-id", sailing.id);

        [departureText(sailing), sailing.fromName + " to " + sailing.toName,
            sailing.passengerPlacesLeft, sailing.vehiclePlacesLeft].forEach(function (value, index) {
            var cell = document.createElement("td");
            cell.setAttribute("data-testid", ["row-departs", "row-route", "row-passenger-places",
                "row-vehicle-places"][index]);
            show(cell, value);
            row.appendChild(cell);
        });

        var action = document.createElement("td");
        if (sailing.full) {
            var full = document.createElement("span");
            full.setAttribute("data-testid", "row-full");
            show(full, "Full");
            action.appendChild(full);
        } else {
            var link = document.createElement("a");
            link.href = "book.html?sailing=" + encodeURIComponent(sailing.id);
            link.setAttribute("data-testid", "row-book-link");
            show(link, "Book this sailing");
            action.appendChild(link);
        }
        row.appendChild(action);
        return row;
    }

    /* ------------------------------------------------------------ book.html */

    function startBookingPage() {
        var sailingId = queryValue("sailing");
        var list = byId("passenger-list");
        var quote = byId("quote-amount");
        var errors = byId("book-errors");
        var vehicleToggle = byId("vehicle-toggle");
        var vehicleFields = byId("vehicle-fields");
        var nextRow = 0;

        function addPassenger() {
            list.appendChild(passengerRow(nextRow));
            nextRow += 1;
            refreshQuote();
        }

        function passengerRow(index) {
            var row = document.createElement("div");
            row.className = "passenger";
            row.setAttribute("data-testid", "passenger-row");

            row.appendChild(field("Name", "text", "passenger-name-" + index, "passenger-name", ""));
            row.appendChild(field("Date of birth", "date", "passenger-dob-" + index,
                "passenger-dob", ""));

            var residentWrap = document.createElement("div");
            residentWrap.className = "checkbox";
            var resident = document.createElement("input");
            resident.type = "checkbox";
            resident.id = "passenger-resident-" + index;
            resident.setAttribute("data-testid", "passenger-resident");
            var residentLabel = document.createElement("label");
            residentLabel.htmlFor = resident.id;
            show(residentLabel, "Gozo resident");
            residentWrap.appendChild(resident);
            residentWrap.appendChild(residentLabel);
            row.appendChild(residentWrap);

            var remove = document.createElement("button");
            remove.type = "button";
            remove.className = "secondary";
            remove.setAttribute("data-testid", "remove-passenger");
            show(remove, "Remove");
            remove.addEventListener("click", function () {
                row.remove();
                refreshQuote();
            });
            row.appendChild(remove);

            row.addEventListener("change", refreshQuote);
            return row;
        }

        function field(labelText, type, id, testId, value) {
            var wrap = document.createElement("div");
            var label = document.createElement("label");
            label.htmlFor = id;
            show(label, labelText);
            var input = document.createElement("input");
            input.type = type;
            input.id = id;
            input.value = value;
            input.setAttribute("data-testid", testId);
            wrap.appendChild(label);
            wrap.appendChild(input);
            return wrap;
        }

        function passengers() {
            return Array.prototype.map.call(list.children, function (row) {
                return {
                    name: row.querySelector("[data-testid='passenger-name']").value,
                    dateOfBirth: row.querySelector("[data-testid='passenger-dob']").value,
                    gozoResident: row.querySelector("[data-testid='passenger-resident']").checked
                };
            });
        }

        function refreshQuote() {
            var travelling = passengers().filter(function (passenger) {
                return passenger.dateOfBirth;
            });
            if (travelling.length === 0) {
                show(quote, "€0.00");
                return;
            }
            var url = "/api/quote?sailingId=" + encodeURIComponent(sailingId)
                + "&vehicle=" + (vehicleToggle.checked ? "true" : "false")
                + travelling.map(function (passenger) {
                    return "&passenger=" + encodeURIComponent(
                        passenger.dateOfBirth + (passenger.gozoResident ? ":resident" : ""));
                }).join("");
            call("GET", url).then(function (answer) {
                show(quote, answer.ok ? answer.body.price : "€0.00");
            });
        }

        vehicleToggle.addEventListener("change", function () {
            vehicleFields.hidden = !vehicleToggle.checked;
            refreshQuote();
        });

        byId("add-passenger").addEventListener("click", addPassenger);

        byId("reserve-button").addEventListener("click", function () {
            show(errors, "");
            call("POST", "/api/bookings", {
                sailingId: sailingId,
                contactEmail: byId("contact-email").value,
                vehicleRegistration: vehicleToggle.checked ? byId("vehicle-registration").value : null,
                passengers: passengers()
            }).then(function (answer) {
                if (!answer.ok) {
                    showErrors(errors, answer.body);
                    return;
                }
                show(byId("reserved-reference"), answer.body.reference);
                byId("payment-section").hidden = false;
            });
        });

        byId("confirm-button").addEventListener("click", function () {
            show(errors, "");
            var reference = byId("reserved-reference").textContent;
            call("POST", "/api/bookings/" + encodeURIComponent(reference) + "/confirm",
                { cardNumber: byId("card-number").value }).then(function (answer) {
                if (!answer.ok) {
                    showErrors(errors, answer.body);
                    return;
                }
                window.location.href = "booking.html?ref=" + encodeURIComponent(reference);
            });
        });

        call("GET", "/api/sailings/" + encodeURIComponent(sailingId)).then(function (answer) {
            var summary = byId("sailing-summary");
            if (!answer.ok) {
                showErrors(byId("book-errors"), answer.body);
                show(summary, "That sailing could not be found.");
                return;
            }
            show(summary, answer.body.fromName + " to " + answer.body.toName + ", leaving "
                + departureText(answer.body) + ". " + answer.body.passengerPlacesLeft
                + " passenger places left.");
        });

        addPassenger();
    }

    /* --------------------------------------------------------- booking.html */

    function startConfirmationPage() {
        var reference = queryValue("ref");
        var errors = byId("booking-errors");

        function render(booking) {
            show(byId("booking-reference"), booking.reference);
            show(byId("booking-status"), booking.status);
            show(byId("booking-sailing"), booking.sailing.fromName + " to "
                + booking.sailing.toName + ", leaving " + departureText(booking.sailing));
            show(byId("booking-passengers"), booking.passengers.map(function (passenger) {
                return passenger.name;
            }).join(", "));
            show(byId("booking-vehicle"), booking.vehicleRegistration || "None");
            show(byId("booking-price"), booking.price);
        }

        call("GET", "/api/bookings/" + encodeURIComponent(reference)).then(function (answer) {
            if (!answer.ok) {
                showErrors(errors, answer.body);
                return;
            }
            render(answer.body);
        });

        byId("cancel-button").addEventListener("click", function () {
            show(errors, "");
            call("POST", "/api/bookings/" + encodeURIComponent(reference) + "/cancel")
                .then(function (answer) {
                    if (!answer.ok) {
                        showErrors(errors, answer.body);
                        return;
                    }
                    render(answer.body);
                    show(byId("cancel-result"), "This booking has been called off.");
                });
        });

        byId("send-copy").addEventListener("click", function () {
            show(byId("copy-result"),
                "A copy would be sent to " + byId("copy-to").value + ".");
        });
    }

    return {
        startSearchPage: startSearchPage,
        startBookingPage: startBookingPage,
        startConfirmationPage: startConfirmationPage
    };
}());
