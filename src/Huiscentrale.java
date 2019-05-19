import com.fazecast.jSerialComm.SerialPort;
import com.onsdomein.proxy.ProxyOnsDomein;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class Huiscentrale {
    private String client_id = "5678";
    private ProxyOnsDomein proxy = new ProxyOnsDomein();
    private OutputStream output;
    private InputStream input;
    private String portLinks = "COM3";
    private SerialPort port = SerialPort.getCommPort(portLinks);

    Huiscentrale() {
        // After first boot of app connection is made with both arduino and server, then passes on to listeningForMessage
        setupArduinoConnection();

        try {
            proxy.connectClientToServer(client_id);
            System.out.println("Connected to server");
            listenForMessageFromServer();
        } catch (IOException e) {
            System.out.println("HC cannot connect to server: " + e);
        }
    }


    private void listenForMessageFromServer() {
        // get messages from server
        while (true) {
            String request;
            try {
                request = proxy.receiveRequest();
            } catch (Exception e) {
                System.out.println("Connection with server lost. " + e);
                break;
            }
            System.out.println("received from server: " + request);
            sendToArduino(request);
        }
    }

    private void setupArduinoConnection() {
        // setup of the serial connection to the Arduino

        try {
            // open the serialport, serialport can only be opened if there is no other program using (Arduino IDE for example).
            port.openPort();
            System.out.println("Succesfully opened the port.");
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
            // Create an outputstream.
            output = port.getOutputStream();
            //create an inputstream.
            input = port.getInputStream();
            //set the baudrate to the same as the arduino
            port.setBaudRate(57600);
            System.out.println("OutputStream established.");
            System.out.println("InputStream established.");
        } catch (Exception e) {
            System.out.println("Comport niet beschikbaar");
            System.exit(1);
        }
    }

    private void sendToArduino(String message) {
        String reactionFromArduino;
        String[] messageSplit = message.split(";", 0);
        //checks if the message has the correct format
        if (messageSplit.length == 3) {

            String outputToArduino = messageSplit[2];

            System.out.println("Sending to Arduino: " + outputToArduino);
            try {
                output.write(outputToArduino.getBytes());
                System.out.println("sending serial message to arduino succesfull");
            } catch (IOException e) {
                System.out.println("sending serial message to arduino failed");
            }

            //TODO: handle response from Arduino (is nu nog dezelfde booschap als ontvangen is gewoon terug gestuurd)

            reactionFromArduino = messageSplit[2];

        } else {
            reactionFromArduino = "res;FAIL: No message send to Arduino";
        }

        sendToProxy(messageSplit[1], reactionFromArduino);

    }

    private void sendToProxy(String reactionFor, String reactionFromArduino) {
        try {
            //TODO: make sure you always respond, the server will if HC is offline, GA waits for a reply from either server or HC
            proxy.sendResponse("setHc", client_id, reactionFor, reactionFromArduino);
        } catch (Exception e) {
            System.out.println("HC kan geen contact maken met de server. " + e);
        }
    }

}