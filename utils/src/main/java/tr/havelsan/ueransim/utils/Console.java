package tr.havelsan.ueransim.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Console {

    private static List<Consumer<String>> printHandlers = new ArrayList<>();
    private static Color lastColor;

    public static synchronized void print(Color color, String format, Object... args) {
        if (color == null) color = Color.RESET;

        String string = String.format(format, args);

        String s;

        if (!Objects.equals(lastColor, color)) {
            lastColor = color;
            s = color + string + Color.RESET;
        } else {
            s = string;
        }

        output(s);
    }

    public static synchronized void println(Color color, String format, Object... args) {
        if (color == null) color = Color.RESET;

        String string = String.format(format, args);

        String s;

        if (!Objects.equals(lastColor, color)) {
            lastColor = color;
            s = color + string + Color.RESET;
        } else {
            s = string;
        }

        outputLine(s);
    }

    public static synchronized void printDiv() {
        println(lastColor, "-----------------------------------------------------------------------------");
    }

    public synchronized static void addPrintHandler(Consumer<String> handler) {
        printHandlers.add(handler);
    }

    public static synchronized void println() {
        outputLine();
    }

    private synchronized static void outputLine() {
        outputLine("");
    }

    private synchronized static void outputLine(String string) {
        output(String.format("%s%n", string));
    }

    private synchronized static void output(String string) {
        System.out.print(string);

        for (var handler : printHandlers)
            handler.accept(string);
    }
}
