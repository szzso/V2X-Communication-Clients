package communication.Message;

import communication.CommandEnum;

import java.util.List;

public class MessageRoute extends MessageCommon {

    private List<String> route;

    public List<String> getRoute() {
        return route;
    }

    public void setRoute(List<String> route) {
        this.route = route;
    }


    public MessageRoute(CommandEnum command, String vehicleID,
                        List<String> route) {
        super(command, vehicleID);
        this.route = route;
        this.command = command;
    }

    public MessageRoute() {
    }
}

