package sperta.domain.core;

public class User extends AUser{
    /**
     * Id should be unique, but is never enforced in Domain Logic
     */
    public User(String id, byte[] hash, byte[] salt){
        super();
        this.setHash(hash);
        this.setSalt(salt);
        this.setUserId(id);
    }

    public User(IUser original){
        this.hash = original.getHash();
        this.salt = original.getSalt();
        this.userId = original.getUserId();
    }

    @Override
    public String toString() {
        return "User [userId=" + this.getUserId() + "]";

    }

    @Override
    public boolean equals(Object o){
        if (o == null || !o.getClass().equals(this.getClass())) return false;
        User other = (User) o;
        return other.getUserId().equals(this.getUserId());
    }
}
