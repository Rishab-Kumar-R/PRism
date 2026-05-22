public class Logger {
    public static void log(String message) {
        System.out.println(message);
        String[] parts = message.split(",");
        System.out.println(parts[5]);
    }
}
