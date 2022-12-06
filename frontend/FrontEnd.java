import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class FrontEnd implements Auction {
    private ArrayList<Integer> replicaIDs = new ArrayList<>();
    private static int primaryID;
    private Polling poll = new Polling();
    private Server primaryServer;

    public FrontEnd(){
        poll.start();
    }

    public void pollReplicas(){
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            String[] registryServices = r.list();

            for (int i = 0; i < registryServices.length; i++) {
                if (registryServices[i].contains("Replica")) {
                    Server current = (Server) r.lookup(registryServices[i]);
                    try {
                        int currentID = current.alive();

                        if (primaryID == -1) {
                            primaryID = currentID;
                            primaryServer = (Server)r.lookup("Replica"+primaryID);
                        }
                        if (!replicaIDs.contains(currentID)) {
                            replicaIDs.add(currentID);
                            Integer[] replicaIDArray = new Integer[replicaIDs.size()];
                            for (int j = 0; j < replicaIDs.size(); j++) {
                                replicaIDArray[j] = replicaIDs.get(j);
                            }
                            primaryServer.receiveReplicaList(replicaIDArray, primaryID);
                            primaryServer.updateOtherReplicas();
                        }

                        Integer[] replicaIDArray = new Integer[replicaIDs.size()];
                        for (int j = 0; j < replicaIDs.size(); j++) {
                            replicaIDArray[j] = replicaIDs.get(j);
                        }

                        current.receiveReplicaList(replicaIDArray, primaryID);

                    } catch (ConnectException e) {
                        System.out.println(registryServices[i] + " dead");
                        r.unbind(registryServices[i]);
                        String[] splString = registryServices[i].split("Replica");
                        int deadID = Integer.parseInt(splString[1]);
 
                        int deadIDpos = replicaIDs.indexOf(deadID);
                        replicaIDs.remove(deadIDpos);

                        if (!replicaIDs.contains(primaryID)) {
                            if (replicaIDs.size() == 0) {
                                System.out.println("No bitches? 0_o");
                                primaryID = -1;
                            }
                            else{
                                primaryID = replicaIDs.get(0);
                                primaryServer = (Server) r.lookup("Replica"+primaryID);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Integer[] listAllReplicaIDs() throws RemoteException {
        return (Integer[]) replicaIDs.toArray();
    }

    public NewUserInfo newUser(String email) throws RemoteException {
        NewUserInfo newUser = null;
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s = (Server) r.lookup("Replica"+primaryID);
            newUser = s.newUser(email);
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return newUser;
    }

    public byte[] challenge(int userID) throws RemoteException {
        byte[] signature = null;
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s = (Server) r.lookup("Replica"+primaryID);
            signature = s.challenge(userID);
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return signature;
    }

   
    public boolean authenticate(int userID, byte[] signature) throws RemoteException {
        boolean authd = false;
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s = (Server) r.lookup("Replica"+primaryID);
            authd = s.authenticate(userID, signature);
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return authd;
    }

    public AuctionItem getSpec(int itemID) throws RemoteException {
        AuctionItem requestedItem = null;
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s = (Server) r.lookup("Replica"+primaryID);
            requestedItem = s.getSpec(itemID);
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return requestedItem;
    }

    public int newAuction(int userID, AuctionSaleItem item) throws RemoteException {
        int itemID = 0;
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s = (Server) r.lookup("Replica"+primaryID);
            itemID = s.newAuction(userID, item);
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemID;
    }

    public AuctionItem[] listItems() throws RemoteException {
        
        AuctionItem[] list = {};
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s = (Server) r.lookup("Replica"+primaryID);
            list = s.listItems();
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();

        }
        return list;
    }

    public AuctionCloseInfo closeAuction(int userID, int itemID) throws RemoteException {
        
        AuctionCloseInfo closeInfo = new AuctionCloseInfo();
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s  = (Server) r.lookup("Replica"+primaryID);
            closeInfo = s.closeAuction(userID, itemID);
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return closeInfo;
    }

    public boolean bid(int userID, int itemID, int price) throws RemoteException {
        boolean accepted = false;
        try {
            Registry r = LocateRegistry.getRegistry("localhost");
            Server s = (Server) r.lookup("Replica"+primaryID);
            accepted = s.bid(userID, itemID, price);
            s.updateOtherReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return accepted;
    }

    public int getPrimaryReplicaID(){

        return primaryID;
    }

    public static void main(String[] args) {
        FrontEnd fa = new FrontEnd();
        primaryID = -1;
        try {
            Auction stub = (Auction) UnicastRemoteObject.exportObject(fa, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("Auction", stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class Polling extends Thread{
        public void run(){
            while(true)
                pollReplicas();
        }
    }
}
