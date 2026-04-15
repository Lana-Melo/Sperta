package sperta.domain.core;

public interface IUser extends java.io.Serializable, Cloneable{
    String getUserId();

    void setUserId(String userId);

    byte[] getHash();

    void setHash(byte[] hash);

    byte[] getSalt();

    void setSalt(byte[] salt);
}