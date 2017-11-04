package communication;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum CommandEnum {

    ROUTE("route"),
    REROUTE("reroute"),
    UNDEFINED("undefined"),
    QUIT("quit");

    private String command;

    CommandEnum(String command) {
        this.command = command;
    }

    public static boolean contains(String command) {
        return Arrays.asList(CommandEnum.values()).stream()
                .filter(value -> value.command.equalsIgnoreCase(command))
                .collect(Collectors.toList()).size() > 0;
    }

    public String getCommand() {
        return this.command;
    }

    public static CommandEnum getNameByValue(String command) {
        return Arrays.stream(CommandEnum.values())
                .filter(c -> c.command.equalsIgnoreCase(command)).findFirst()
                .get();
    }
}
