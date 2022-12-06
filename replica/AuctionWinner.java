import java.io.Serializable;

public class AuctionWinner implements Serializable{
    public int userID;
    public int itemID;

    public AuctionWinner(int userID, int itemID){
        this.userID = userID;
        this.itemID = itemID;
    }
}

