package communication.Message;

import communication.CommandEnum;

public class MessageCommon {

    String vehicleID;
    CommandEnum command;

    public MessageCommon(CommandEnum command, String vehicleID) {
        this.vehicleID = vehicleID;
        this.command = command;
    }

    public MessageCommon() {
    }

    public CommandEnum getCommand() {
        return command;
    }

    public void setCommand(CommandEnum command) {
        this.command = command;
    }

    public String getVehicleID() {
        return vehicleID;
    }

    public void setVehicleID(String vehicleID) {
        this.vehicleID = vehicleID;
    }
}
