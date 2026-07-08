import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class YearWorks {

    private static final int PORT = 8081;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/", YearWorks::handleHome);
        server.createContext("/calendar", YearWorks::handleCalendar);
        server.setExecutor(null);
        server.start();
        System.out.println("Web app started at http://localhost:" + 8081 + "/");
    }

    private static void handleHome(HttpExchange exchange) throws IOException {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        String html = buildPage(currentYear, null);
        sendResponse(exchange, html);
    }

    private static void handleCalendar(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String yearText = params.getOrDefault("year", String.valueOf(LocalDate.now().getYear()));

        String html;
        try {
            int year = Integer.parseInt(yearText.trim());
            html = buildPage(String.valueOf(year), buildCalendarHtml(year));
        } catch (NumberFormatException ex) {
            html = buildPage(yearText, "<p class=\"error\">Please enter a valid year.</p>");
        }

        sendResponse(exchange, html);
    }

    private static void sendResponse(HttpExchange exchange, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static String buildPage(String yearValue, String calendarHtml) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"en\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>Year Calendar</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 0; background: #f4f8ff; color: #1f2937; }");
        html.append(".container { max-width: 1200px; margin: 0 auto; padding: 24px; }");
        html.append(".header { background: linear-gradient(90deg, #1e66cc, #2f80ed); color: white; padding: 24px; border-radius: 12px; margin-bottom: 20px; }");
        html.append("form { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 12px; }");
        html.append("input { padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 15px; min-width: 160px; }");
        html.append("button { padding: 10px 16px; border: none; border-radius: 8px; background: #ff9800; color: white; font-size: 15px; cursor: pointer; }");
        html.append(".calendar { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }");
        html.append(".month { background: white; padding: 16px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }");
        html.append(".month h3 { margin-top: 0; color: #1e66cc; }");
        html.append(".month ul { list-style: none; padding: 0; margin: 0; }");
        html.append(".month li { padding: 4px 0; border-bottom: 1px solid #e5e7eb; font-size: 14px; }");
        html.append(".holiday { color: #dc143c; font-weight: bold; }");
        html.append(".error { color: #b91c1c; font-weight: bold; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\">");
        html.append("<h1>Select Year to View Calendar</h1>");
        html.append("<form method=\"get\" action=\"/calendar\">");
        html.append("<label for=\"year\">Year:</label>");
        html.append("<input id=\"year\" name=\"year\" type=\"number\" value=\"").append(escapeHtml(yearValue)).append("\" required>");
        html.append("<button type=\"submit\">Show Calendar</button>");
        html.append("</form>");
        html.append("</div>");
        if (calendarHtml != null && !calendarHtml.isBlank()) {
            html.append(calendarHtml);
        }
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    private static String buildCalendarHtml(int year) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"calendar\">");
        boolean leapYear = isLeapYear(year);
        int[] daysInMonth = {
            31,
            leapYear ? 29 : 28,
            31, 30, 31, 30,
            31, 31, 30, 31, 30, 31
        };

        String[] monthNames = {
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        };

        String[] dayNames = {
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
        };

        int day = getFirstDayOfYear(year);
        for (int month = 0; month < 12; month++) {
            html.append("<section class=\"month\">");
            html.append("<h3>").append(escapeHtml(monthNames[month])).append("</h3>");
            html.append("<ul>");
            for (int date = 1; date <= daysInMonth[month]; date++) {
                html.append("<li>");
                html.append("<strong>").append(date).append("</strong> (").append(dayNames[day]).append(") : ");

                String holidayName = getHolidayName(year, month + 1, date);
                if (holidayName != null) {
                    html.append("<span class=\"holiday\">HOLIDAY - ").append(escapeHtml(holidayName)).append("</span>");
                } else if (day == 0 || day == 3 || day == 5) {
                    html.append("Works -> 1 3 5 7");
                } else {
                    html.append("Works -> 2 4 6");
                }
                html.append("</li>");
                day = (day + 1) % 7;
            }
            html.append("</ul>");
            html.append("</section>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static boolean isLeapYear(int year) {
        return (year % 400 == 0) || (year % 4 == 0 && year % 100 != 0);
    }

    private static int getFirstDayOfYear(int year) {
        return LocalDate.of(year, 1, 1).getDayOfWeek().getValue() - 1;
    }

    private static String getHolidayName(int year, int month, int day) {
        switch (month) {
            case 1:
                if (day == 14) return "Makar Sankranti";
                if (day == 26) return "Republic Day";
                break;
            case 3:
                if (day == 11) return "Ramzan";
                if (day == 30) return "Ugadi";
                break;
            case 4:
                if (day == 9) return "Ugadi";
                if (day == 14) return "Dr. B.R. Ambedkar Jayanti";
                if (day == 21) return "Eid";
                break;
            case 5:
                if (day == 1) return "Labour Day";
                break;
            case 6:
                if (day == 6) return "Bakrid";
                break;
            case 7:
                if (day == 17) return "Moharram";
                break;
            case 8:
                if (day == 15) return "Independence Day";
                break;
            case 9:
                if (day == 15) return "Ganesh Chaturthi";
                break;
            case 10:
                if (day == 2) return "Gandhi Jayanti";
                if (day == 24) return "Dasara";
                break;
            case 11:
                if (day == 1) return "Karnataka Rajyotsava";
                if (day == 12) return "Deepavali";
                break;
            case 12:
                if (day == 25) return "Christmas";
                break;
        }
        return null;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }
        for (String part : rawQuery.split("&")) {
            int index = part.indexOf('=');
            if (index < 0) {
                params.put(part, "");
            } else {
                String key = URLDecoder.decode(part.substring(0, index), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(part.substring(index + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }
}


