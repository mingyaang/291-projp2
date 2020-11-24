import org.bson.types.ObjectId;

import java.util.List;

public class Post {

    public ObjectId _id;
    public String Id;
    public String PostTypeId;
    public String AcceptedAnswerId;
    public String ParentId;
    public String CreationDate;
    public Integer Score;
    public Integer ViewCount;
    public String Body;
    public String OwnerUserId;
    public String LastEditorUserId;
    public String LastEditDate;
    public String LastActivityDate;
    public String Title;
    public String Tags;
    public Integer AnswerCount;
    public Integer CommentCount;
    public Integer FavoriteCount;
    public String ContentLicense;
    public List<String> Terms;

    public Post() {}
}
