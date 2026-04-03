package sperta.domain.core;

public interface IUser extends java.io.Serializable, Cloneable{
    String getUserId();

    void setUserId(String userId);

    String getPassword();

    void setPassword(String password);
}
