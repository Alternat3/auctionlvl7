import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable{
    String email;
    int userID;
    ArrayList<Integer> auctionsCreated;
    byte[] publicKey;
    int authd = 0;

    public User(String email, int userID, byte[] publicKey){
        this.email = email;
        this.userID = userID;
        auctionsCreated = new ArrayList<>();
        this.publicKey = publicKey;
    }

    public void addAuction(int itemID){
        auctionsCreated.add(itemID);
    }
}
