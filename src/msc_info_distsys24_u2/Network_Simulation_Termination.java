package msc_info_distsys24_u2;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.Network;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.Simulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

public class Network_Simulation_Termination {
    //==========================================================================================
    // Main method
    //==========================================================================================
    public static void main(String[] args) {
        final int numActors = 10;
        Simulator simulator = Simulator.getInstance();
        LinkedList<String> networkActors = new LinkedList<>();
        for (int i = 0; i < numActors; i++) {
            new ActorNode(String.format("Actor-%d", i), networkActors);
        }
        simulator.simulate();
    }

    //==========================================================================================
    // Class ActorNode
    //==========================================================================================
    /** Class ActorNode
     * Defines an actor according to '2024S Ubung DS02.pdf', 'Aufgabe 2'.
     */
    public static class ActorNode extends Node {
        /** Class constructor
         * @param name Name of Node.
         * @param networkActors Reference to List of ActorNode names in Network.
         *                      Instantiated NodeActor gets automatically added to
         *                      networkActors.
         */
        public ActorNode(String name, LinkedList<String> networkActors) {
            super(name);
            this.active = true;
            this.sendProbability = 1;
            this.networkActors = networkActors;
            sentMsgCnt = 0;
            receivedMsgCnt = 0;
            firework = new Message()
                    .add("sender", this.NodeName())
                    .add("type", "firework");
            networkActors.add(name);
        }

        /** Method engage
         * Engage method of ActorNode.
         */
        @Override
        public void engage() {
            Message receiveMsg;
            sendToSubset(firework);
            active = false;
            while (true) {
                receiveMsg = receive();
                if (receiveMsg == null) continue;
                //System.out.println(receiveMsg.getPayload().get("type"));
                if (receiveMsg.getPayload().get("type").equals("firework")) handleFireworkMessage();
                else if (receiveMsg.getPayload().get("type").equals("control")) handleControlMessage();
                else if (receiveMsg.getPayload().get("type").equals("termination")) break;
            }
        }

        private void handleFireworkMessage() {
            receivedMsgCnt += 1;
            active = true;
            double roll = Math.random();
            if (roll <= sendProbability) {
                sendToSubset(firework);
            }
            sendProbability /= 2;
            active = false;
        }

        private void handleControlMessage() {
            Message controlMsg = new Message()
                    .add("sender",this.NodeName())
                    .add("type", "control")
                    .add("status",String.valueOf(active))
                    .add("sent",String.valueOf(sentMsgCnt))
                    .add("received",String.valueOf(receivedMsgCnt));
            sendBlindly(controlMsg, observer.getName());
        }

        /** Method sendToSubset
         * Sends a message to a random subset of 'networkActors'.
         * Arrival of messages is not guarantied.
         * @param message the message to be sent
         */
        private void sendToSubset(Message message) {
            int rangeValue = (int) (Math.random() * (networkActors.size()-2))+2;
            LinkedList<String> list = networkActors.stream()
                    .filter(s -> !(s.equals(NodeName())))
                    .collect(Collectors.toCollection(LinkedList::new));
            Collections.shuffle(list);
            list.subList(1, rangeValue).forEach(s -> {
                sendBlindly(message, s);
                sentMsgCnt += 1;
            });
        }

        /** Class Variables */
        boolean active;
        double sendProbability;
        int sentMsgCnt, receivedMsgCnt;
        LinkedList<String> networkActors;
        Message firework;
        ObserverNode observer = ObserverNode.getInstance();
    }

    //==========================================================================================
    // Class ObserverNode
    //==========================================================================================

    /** Class ObserverNode
     * Defines an observer as singleton that monitors the network and
     * checks if the network has terminated in regular, predetermined intervals
     */
    public static class ObserverNode extends Node {
        /** Class constructor
         * @param name Name of observer
         *             Because of the singleton nature the name will always be predetermined,
         *             in this case: "Observer"
         */
        private ObserverNode(String name) {
            super(name);
        }

        /** Method getInstance
         * Creates and returns a new ObserverNode instance if none has been created yet.
         * Else just returns the already existing instance
         * @return Instance of ObserverNode
         */
        public static ObserverNode getInstance() {
            if (instance == null) {
                synchronized (Simulator.class) {
                    if (instance == null) {
                        instance = new ObserverNode("Observer");
                    }
                }
            }
            return instance;
        }

        /** Method engage
         * Broadcasts a control message every SLEEP_TIME seconds requesting information about the
         * current activity status and sent and received messages of all other network nodes.
         * Determines the network as terminated, if no node is active and the number of
         * received Messages equals the number of sent messages, in which case the simulator gets shut down.
         */
        public void engage() {
            boolean networkActive;
            while(true) {
                try {
                    Thread.sleep((int) SLEEP_TIME*1000);
                } catch (InterruptedException e) {
                    break;
                }
                networkActive = getNetworkActivityStatus();
                if (networkActive) continue;
                try {
                    Thread.sleep((int) SLEEP_TIME*1000);
                } catch (InterruptedException e) {
                    break;
                }
                networkActive = getNetworkActivityStatus();
                if (networkActive) continue;
                break;
            }
            logger.debug("[INFO] System Terminated");
            broadcast(terminationMessage);
            simulator.shutdown();
        }

        public String getName() {
            return this.NodeName();
        }

        /** Method getNetworkActivityStatus
         * Sends control messages to every other actor in the network expecting a message, containing
         * the activity-status and sent and received messages.
         * @return boolean value representing the networks activity status
         *          true - Network is still active
         *          false - network has terminated
         */
        private boolean getNetworkActivityStatus() {
            boolean activeNetwork;
            int sentMsgCnt, receivedMsgCnt, answerCnt;
            int actionNodeCnt = Network.getInstance().numberOfNodes()-1;

            sentMsgCnt = 0;
            receivedMsgCnt = 0;
            answerCnt = 0;
            activeNetwork = false;
            broadcast(controlMessage);
            while (answerCnt < actionNodeCnt) {
                receiveMessage = receive();
                answerCnt += 1;
                Map<String, String> payload = receiveMessage.getPayload();
                boolean active = Boolean.parseBoolean(payload.get("status"));
                if (active) activeNetwork = true;
                sentMsgCnt += Integer.parseInt(payload.get("sent"));
                receivedMsgCnt += Integer.parseInt(payload.get("received"));
            }
            if (activeNetwork || sentMsgCnt != receivedMsgCnt) {
                logger.debug("[INFO] Network still active");
                return true;
            }
            return false;
        }

        /** Class Variables */
        Simulator simulator = Simulator.getInstance();
        static ObserverNode instance = null;
        Message controlMessage = new Message()
                .add("type", "control")
                .add("sender", this.NodeName());
        Message terminationMessage = new Message()
                .add("type","termination")
                .add("sender",this.NodeName());
        Message receiveMessage;
        final double SLEEP_TIME = 1;
        Logger logger = LoggerFactory.getLogger(this.NodeName());
    }
}