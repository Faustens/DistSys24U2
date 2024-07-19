package msc_info_distsys24_u2;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.Simulator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class Network_Simulation_Fireworks {
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
        simulator.simulate(60);
        simulator.shutdown();
    }

    //==========================================================================================
    // Class ActorNode
    //==========================================================================================
    /** Class ActorNode
     * Defines an actor according to '2024S Ubung DS02.pdf', 'Aufgabe 2'.
     */
    public static class ActorNode extends Node {
        /** Class Variables */
        boolean active;
        double sendProbability;
        LinkedList<String> networkActors;

        /** Class constructor.
         * @param name Name of Node.
         * @param networkActors Reference to List of ActorNode names in Network.
         *                      Instantiated NodeActor gets automatically added to
         *                      networkActors.
         */
        public ActorNode(String name, LinkedList<String> networkActors) {
            super(name);
            this.active = true;
            this.sendProbability = .5;
            this.networkActors = networkActors;
            networkActors.add(name);
        }

        /** Method engage
         * Engage method of ActorNode.
         */
        @Override
        public void engage() {
            Message receiveMsg;
            Message firework = new Message().add("sender", this.NodeName());
            double roll;
            sendToSubset(firework);
            active = false;
            while (true) {
                receiveMsg = receive();
                if (receiveMsg == null) continue;
                active = true;
                roll = Math.random();
                if (roll <= sendProbability) sendToSubset(firework);
                sendProbability /= 2;
                active = false;
            }
        }

        /** Method sendToSubset
         * Sends a message to a random subset of 'networkActors'.
         * @param message the message to be sent
         */
        private void sendToSubset(Message message) {
            int rangeValue = (int) (Math.random() * (networkActors.size()-2))+2;
            LinkedList<String> list = networkActors.stream()
                    .filter(s -> !(s.equals(NodeName())))
                    .collect(Collectors.toCollection(LinkedList::new));
            Collections.shuffle(list);
            list.subList(1, rangeValue).forEach(s -> sendBlindly(message, s));
        }
    }
}
