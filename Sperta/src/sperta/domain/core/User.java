package sperta.domain.core;

public class User extends AUser{
    /**
     * Id should be unique, but is never enforced in Domain Logic
     */
    public User(String id, String pass){
        super();
        this.setPassword(pass);
        this.setUserId(id);
    }

    @Override
    public String toString() {
        return "User [userId=" + this.getUserId() + "]";

    }
}
