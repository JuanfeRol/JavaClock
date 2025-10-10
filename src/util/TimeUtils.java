package util;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class TimeUtils {
public static final DateTimeFormatter ISO =
DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
.withZone(ZoneId.systemDefault());


public static String fmt(long millis) {
return ISO.format(Instant.ofEpochMilli(millis));
}


public static String secs(long millis) {
return String.format("%.3f", millis / 1000.0);
}
}