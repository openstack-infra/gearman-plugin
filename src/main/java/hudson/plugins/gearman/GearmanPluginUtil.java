package hudson.plugins.gearman;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearmanPluginUtil {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);


    /*
     * This method checks whether a connection can be made to a host:port
     *
     * @param host
     *  the host name
     *
     * @param port
     *  the host port
     *
     * @param timeout
     *  the timeout (milliseconds) to try the connection
     *
     * @return
     *  true if a socket connection can be established otherwise false
     */
    public static boolean connectionIsAvailable(String host, int port, int timeout) {

        InetSocketAddress endPoint = new InetSocketAddress(host, port);
        Socket socket = new Socket();

        if (endPoint.isUnresolved()) {
            System.out.println("Failure " + endPoint);
        } else {
            try {
                socket.connect(endPoint, timeout);
                logger.info("Connection Success:    "+endPoint);
                return true;
            } catch (Exception e) {
                logger.info("Connection Failure:    "+endPoint+" message: "
                        +e.getClass().getSimpleName()+" - "
                        +e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        logger.info(e.getMessage());
                    }
                }
            }
        }
        return false;
    }


}
