package facex.facepass.db;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

@Entity
public class User {
    @Id
    @Property
    public Long id = 0l;
    @Property
    public String name = "";
    @Property
    public String faceToken = "";
    @Generated(hash = 922047080)
    public User(Long id, String name, String faceToken) {
        this.id = id;
        this.name = name;
        this.faceToken = faceToken;
    }
    @Generated(hash = 586692638)
    public User() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getFaceToken() {
        return this.faceToken;
    }
    public void setFaceToken(String faceToken) {
        this.faceToken = faceToken;
    }
}
