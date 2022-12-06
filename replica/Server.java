import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Server extends Remote{
    public NewUserInfo newUser(String email) throws 
    RemoteException;
    public byte[] challenge(int userID) throws RemoteException;
    public boolean authenticate(int userID, byte signature[]) 
    throws RemoteException;
    public AuctionItem getSpec(int itemID) throws RemoteException;
    public int newAuction(int userID, AuctionSaleItem item) throws 
    RemoteException;
    public AuctionItem[] listItems() throws RemoteException;
    public AuctionCloseInfo closeAuction(int userID, int itemID) 
    throws RemoteException;
    public boolean bid(int userID, int itemID, int price) throws 
    RemoteException;
    public int alive() throws RemoteException;
    public int receiveUpdate(User[] receivedUsers, AuctionWinner[] receivedWinners, AuctionItem[] receivedItems) throws RemoteException;
    public void updateOtherReplicas() throws RemoteException;
    public void receiveReplicaList(Integer[] replicaIDs, int primaryReplicaID) throws RemoteException;
}
