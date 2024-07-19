package org.oxoo2a.sim4da;

import java.util.Collections;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class Network_Simulation_Fireworks {
    public static void main(String[] args) {
        final int numActors = 5;
        Simulator simulator = Simulator.getInstance();
        LinkedList<String> networkActors = new LinkedList<>();
        for (int i=0; i<numActors; i++) {
            new ActorNode(String.format("Actor %d",i),networkActors);
        }
        simulator.simulate(60);
        simulator.shutdown();
    }


    public static class ActorNode extends Node {
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

        /** Method sentToSubset
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
