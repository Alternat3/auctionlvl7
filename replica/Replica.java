import java.io.File;
import java.io.FileOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

public class Replica implements Server{
    private static int replicaID;
    private ArrayList<Integer> otherReplicas = new ArrayList<>();
    private ArrayList<User> users = new ArrayList<>();
    private ArrayList<AuctionItem> listedItems = new ArrayList<>();
    private ArrayList<AuctionWinner> currentWinners = new ArrayList<>();
    private static Key publicKey;
    private static Key privateKey;
    private int primaryID;
    private boolean isPrimary = false;

    public NewUserInfo newUser(String email) throws RemoteException {
        System.out.println("Called");
        KeyPairGenerator gen;
        NewUserInfo newUser = new NewUserInfo();
        try {
            gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.genKeyPair();
            Key publicKey = kp.getPublic();
            Key privKey = kp.getPrivate();
            byte[] userPublicKey = publicKey.getEncoded();
            byte[] userPrivKey = privKey.getEncoded();

            newUser.privateKey = userPrivKey; newUser.publicKey = userPublicKey;
            int userID = users.size() + 1;
            newUser.userID = userID;
            User newUser2 = new User(email, userID, userPublicKey);
            users.add(newUser2);

            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        updateOtherReplicas();

        return newUser;
    }

    public byte[] challenge(int userID) throws RemoteException {
        String str = "auction";
        byte[] bytearr = str.getBytes();
        byte[] finalsig = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign((PrivateKey) privateKey);
            sig.update(bytearr);
            finalsig = sig.sign();
        } catch (Exception e) {
        }
        
        users.get(userID - 1).authd++;

        return finalsig;
    }

    public boolean authenticate(int userID, byte[] signature) throws RemoteException {
        byte[] userPublicKey = users.get(userID - 1).publicKey;
        String userEmail = users.get(userID - 1).email;
        boolean result = false;

        try {
            Signature userSig = Signature.getInstance("SHA256withRSA");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(userPublicKey);
            PublicKey pub = kf.generatePublic(pubKeySpec);
            userSig.initVerify(pub);
            userSig.update(userEmail.getBytes());
            result = userSig.verify(signature);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(result == true){
            users.get(userID - 1).authd++;
        }
        
        return result;
    }

    public boolean isAuth(int userID){
        boolean auth = false;

        for(int i = 0; i < users.size(); i++){
            User current = users.get(i);
            if(current.userID == userID){
                if(current.authd >= 2){
                    auth = true;
                }
            }
        }
        return auth;
    }

    public AuctionItem getSpec(int itemID) throws RemoteException {
        AuctionItem result = null;
        for (int i = 0; i < listedItems.size(); i++) {
            AuctionItem current = listedItems.get(i);
            if (current.itemID == itemID) {
                result = current;
            }
        }
        return result;
    }

    public int newAuction(int userID, AuctionSaleItem item) throws RemoteException {
        int accepted = 0;
        if(users.get(userID-1).authd >=2){
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).userID == userID) {
                    AuctionItem newItem = new AuctionItem(); newItem.name = item.name; newItem.description = item.description; newItem.highestBid = item.reservePrice;
                    newItem.itemID = listedItems.size() + 1;
                    listedItems.add(newItem);
                    users.get(i).addAuction(newItem.itemID);
                    accepted = 1;
                }
            }
        }
        
        return accepted;
    }

    public AuctionItem[] listItems() throws RemoteException {
        AuctionItem[] list;
        if (listedItems.size() > 0) {
            list = new AuctionItem[listedItems.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = listedItems.get(i);
            }
        }
        else{
            list = null;
        }
        return list;
    }

    public AuctionCloseInfo closeAuction(int userID, int itemID) throws RemoteException {
        AuctionCloseInfo temp = null;

        if(users.get(userID-1).authd >=2){
            int winnerID = 0;
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).userID == userID) {
                    if (users.get(i).auctionsCreated.contains(itemID)) {
                        for (int j = 0; j < currentWinners.size(); j++) {
                            if (currentWinners.get(j).itemID == itemID) {
                                winnerID = currentWinners.get(j).userID;
                        }
                    }
                }
            }
        }

            if (winnerID != 0) {
                temp = new AuctionCloseInfo();
            for (int i = 0; i < listedItems.size(); i++) {
                    if(listedItems.get(i).itemID == itemID)
                        temp.winningPrice = listedItems.get(i).highestBid;
                }
                temp.winningEmail = users.get(winnerID - 1).email;
            }
        }

        return temp;
    }

    public boolean bid(int userID, int itemID, int price) throws RemoteException {
        boolean bidAccepted = false;
        if(users.get(userID-1).authd >=2){
            int itemIndex = -1;
            for (int i = 0; i < listedItems.size(); i++) {
            AuctionItem current = listedItems.get(i);
            if (current.itemID == itemID) {
                itemIndex = i;
            }
            }

            if (itemIndex != -1) {
                if (listedItems.get(itemIndex).highestBid < price) {
                    listedItems.get(itemIndex).highestBid = price;
                    bidAccepted = true;
                    AuctionWinner temp = new AuctionWinner(userID, itemID);
                if (currentWinners.contains(temp)) {
                    int index = currentWinners.indexOf(temp);
                    currentWinners.get(index).userID = userID;
                }
                else{
                    currentWinners.add(temp);
                }
                }
            }
        }
        return bidAccepted;
    }

    public void updateReplicaList(){
        otherReplicas = null;
        try {
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateOtherReplicas(){
        if(isPrimary == true){
            AuctionItem[] itemsArray;
            User[] userArray;
            AuctionWinner[] winnersArray;

            try {
                if (users != null) {
                    userArray = new User[users.size()];
                    for (int i = 0; i < userArray.length; i++) {
                        userArray[i] = users.get(i);
                    }
                }
                else{
                    userArray = new User[0];
                }
                
                if (listedItems != null) {
                    itemsArray = new AuctionItem[listedItems.size()];
                    for (int i = 0; i < itemsArray.length; i++) {
                        itemsArray[i] = listedItems.get(i);
                    }
                }
                else{
                    itemsArray = new AuctionItem[0];
                }
                
                if (currentWinners != null) {
                    winnersArray = new AuctionWinner[currentWinners.size()];
                    for (int i = 0; i < winnersArray.length; i++) {
                        winnersArray[i] = currentWinners.get(i);
                    }
                }
                else{
                    winnersArray = new AuctionWinner[0];
                }
                

                Registry registry = LocateRegistry.getRegistry("localhost");
                Server current;
                for (int i = 0; i < otherReplicas.size(); i++) {
                    System.out.println("Replica ID: " + replicaID + "   otherReplicaID: " + otherReplicas.get(i));
                    if (otherReplicas.get(i) != replicaID) {
                        System.out.println("Updating Replica"+otherReplicas.get(i));
                        current = (Server) registry.lookup("Replica"+otherReplicas.get(i));
                        current.receiveUpdate(userArray, winnersArray, itemsArray);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int receiveUpdate(User[] receivedUsers, AuctionWinner[] receivedWinners, AuctionItem[] receivedItems){
        System.out.println("Receiving update");
        int updated = 0;
        users = new ArrayList<>();
        for (int i = 0; i < receivedUsers.length; i++) {
            users.add(receivedUsers[i]);
        }

        listedItems = new ArrayList<>();
        for (int i = 0; i < receivedItems.length; i++) {
            listedItems.add(receivedItems[i]);
        }

        currentWinners = new ArrayList<>();
        for (int i = 0; i < receivedWinners.length; i++) {
            currentWinners.add(receivedWinners[i]);
        }

        updated = 1;
        return updated;
    }

    public void receiveReplicaList(Integer[] replicaIDs, int primaryReplicaID) {
        otherReplicas = new ArrayList<>();
        for (int i = 0; i < replicaIDs.length; i++) {
            otherReplicas.add(replicaIDs[i]);
        }

        primaryID = primaryReplicaID;

        if (primaryID == replicaID) {
            isPrimary = true;
        }
        else{
            isPrimary = false;
        }
    }

    public int alive(){
        return replicaID;
    }
    
    public static void main(String[] args) {
        replicaID = Integer.parseInt(args[0]);

        Replica r = new Replica();
        String serviceName = ("Replica" + replicaID);
        System.out.println(serviceName);
        try {
            Registry registry = LocateRegistry.getRegistry();
            Server stub = (Server) UnicastRemoteObject.exportObject(r, 0);
            registry.rebind(serviceName, stub);

            KeyPairGenerator kp = KeyPairGenerator.getInstance("RSA");
            kp.initialize(2048);
            KeyPair keyPair = kp.genKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            byte[] currentKey = privateKey.getEncoded();
            File privateFile = new File("..\\keys\\server_private.key");
            File publicFile = new File("..\\keys\\server_public.key");
            try (FileOutputStream stream = new FileOutputStream(privateFile,false)){
                stream.write(currentKey);
            }

            currentKey = publicKey.getEncoded();

            try (FileOutputStream stream = new FileOutputStream(publicFile,false)){
                stream.write(currentKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}