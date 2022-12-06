import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Client {
    public static String userEmail;
    public static void main(String[] args) {
        NewUserInfo userInfo = null;
        Auction server = null;
        try {
                String name = "Auction";
                Registry registry = LocateRegistry.getRegistry("localhost");
                server = (Auction) registry.lookup(name);
               }
               catch (Exception e) {
                System.err.println("Exception:");
                e.printStackTrace();
            }
            
        while (true) {
            int menu = mainMenu();
            switch (menu) {
                case 1:
                    getItemSpec(server);
                    break;
                case 2:
                    bid(server, userInfo.userID);
                    break;
                case 3:
                    listItemForSale(server, userInfo.userID);
                    break;
                case 4:
                    closeAuction(server, userInfo.userID);
                    break;
                case 5:
                    userInfo = createUser(server);
                    break;
                case 6:
                    listAvailableAuctions(server);
                    break;
                case 7:
                authenticate(server, userInfo.userID, userInfo.privateKey);
                break;
                case 8:
                challenge(server, userInfo.userID);
                break;
                default:
                    break;
            }
        }
        
    }

    public static void getItemSpec(Auction server){
        int itemID = 0;
        System.out.println("Input itemID: ");
        String read = System.console().readLine();
        try {
            itemID = Integer.parseInt(read);
        } catch (Exception e) {
            System.out.println("Invalid Entry");
        }

        if (itemID != 0) {
            try {
                AuctionItem result = server.getSpec(itemID);
                if (result != null) {
                    System.out.println(result.itemID + ": " + result.name + ", " + result.description + ", " + result.highestBid);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void bid(Auction server, int userID){
        int itemID = 0;
        int bid = 0;
        System.out.println("Item ID: ");
        String IDString = System.console().readLine();
        System.out.println("Enter bid: ");
        String bidString = System.console().readLine();
        try {
            itemID = Integer.parseInt(IDString);
            bid = Integer.parseInt(bidString);
        } catch (Exception e) {
            System.out.println("Invalid entry.");
        }

        if (itemID <= 0 || bid <= 0) {
            System.out.println("Invalid entry");
        }
        else{
            boolean result = false;
            try {
                result = server.bid(userID, itemID, bid);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if (result) {
                System.out.println("Bid accepted");
            }
            else{
                System.out.println("Bid rejected");
            }
        }
    }

    public static void listItemForSale(Auction server, int userID){
        System.out.println("Item name: ");
        String name = System.console().readLine();
        System.out.println("Enter product description: ");
        String description = System.console().readLine();
        System.out.println("Enter reserve price: ");
        String result = System.console().readLine();
        int reservePrice = -1;
        try {
            reservePrice = Integer.parseInt(result);
        } catch (Exception e) {
            System.out.println("Invalid entry.");
        }

        if (reservePrice < 0) {
            System.out.println("Invalid reserve price.");
        }
        else{
            AuctionSaleItem newItem = new AuctionSaleItem(); newItem.name = name; newItem.description = description; newItem.reservePrice = reservePrice;
            try {
                server.newAuction(userID, newItem);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeAuction(Auction server, int userID){
        System.out.println("Enter item ID to close auction.");
        String itemIDString = System.console().readLine();
        int itemID = 0;
        try {
            itemID = Integer.parseInt(itemIDString);
        } catch (Exception e) {
            System.out.println("Invalid entry");
        }

        if (itemID > 0) {
            try {
                AuctionCloseInfo closeInfo = server.closeAuction(userID, itemID);
                if (closeInfo != null) {
                    System.out.println("Winning email: " + closeInfo.winningEmail + ", Winning Price: " + closeInfo.winningPrice);
                }
                else{
                    System.out.println("Invalid entry.");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static NewUserInfo createUser(Auction server){
        NewUserInfo userInfo = null;
        System.out.println("Please enter email: ");
        userEmail = System.console().readLine();
        try {
            userInfo = server.newUser(userEmail);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println("Your userID is: " + userInfo.userID);
        return userInfo;
    }

    public static void listAvailableAuctions(Auction server){
        try {
            AuctionItem[] auctions = server.listItems();
            if (auctions != null) {
                for (AuctionItem auctionItem : auctions) {
                    System.out.println(auctionItem.itemID + ": " + auctionItem.name + ", " + auctionItem.description + ", " + auctionItem.highestBid);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void authenticate(Auction server, int userID, byte[] userPrivKey){

        String str = userEmail;
        byte[] bytearr = str.getBytes();
        byte[] finalsig = null;

        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(userPrivKey);
            PrivateKey priv = kf.generatePrivate(privKeySpec);
            sig.initSign(priv);
            sig.update(bytearr);
            finalsig = sig.sign();
            boolean result = server.authenticate(userID, finalsig);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void challenge(Auction server, int userID){
        try {
            byte[] strResult = server.challenge(userID);
            Signature sig = Signature.getInstance("SHA256withRSA");
            Path keyPath = Paths.get("../keys/server_public.key");
            byte[] server_public = Files.readAllBytes(keyPath);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            EncodedKeySpec pubKey = new X509EncodedKeySpec(server_public);
            PublicKey pub = kf.generatePublic(pubKey);
            sig.initVerify(pub);
            sig.update("auction".getBytes());
            boolean result = sig.verify(strResult);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int mainMenu(){
        System.out.println("Please choose an option.");
        System.out.println("1. Get Item Spec");
        System.out.println("2. Bid on item.");
        System.out.println("3. Create Auction");
        System.out.println("4. Close auction");
        System.out.println("5. Create new user");
        System.out.println("6. List All Auctions");
        System.out.println("7. Authenticate");
        System.out.println("8. Challenge");

        String result = System.console().readLine();
        int numResult = 0;
        try {
            numResult = Integer.parseInt(result);
        } catch (Exception e) {
            System.out.println("Invalid entry.");
        }

        return numResult;
    }
    
    
}

