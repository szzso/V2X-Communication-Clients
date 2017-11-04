package communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import communication.Message.MessageCommon;
import communication.Message.MessageRoute;

import java.io.IOException;
import java.util.Optional;

public class FactoryMessage {

    public static <M extends MessageCommon> Optional<M> createMessage(CommandEnum command, String messageJSON) throws IOException {
        switch (command){
            case ROUTE:
                return Optional.of((M) parseMessage(messageJSON, MessageRoute.class));

        }
        System.out.println("Message create - Empty ");
        return Optional.empty();
    }

    public static CommandEnum getCommandType(String messageJSON) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(messageJSON.getBytes());
            JsonNode commandNode = rootNode.path("command");
            return CommandEnum.getNameByValue(commandNode.asText());

        } catch (IOException e) {
            return CommandEnum.UNDEFINED;
        }
    }

    private static  <M extends MessageCommon> M parseMessage(String messageJSON, Class<M> messageClass) throws IOException {
        return new ObjectMapper().readValue(messageJSON, messageClass);
    }

    public static <M extends MessageCommon> String convertToJson(M message){
        ObjectMapper objectMapper = new ObjectMapper();
        String messageJSON = "";
        try {
            messageJSON = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return messageJSON;
    }
}
