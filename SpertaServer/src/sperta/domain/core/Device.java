package sperta.domain.core;

import java.util.Base64;

import sperta.utils.SectionType;

public class Device  implements java.io.Serializable {
    private Long id;
    private SectionType type;
    private byte [] state;

    /**
     * SectionType t can't be ALL
     * @param id
     * @param t
     */
    Device(Long id, SectionType t){
        this.id = id;
        this.type = t;
        this.state = new byte []{};
    }

    Device(Long id, SectionType type, byte [] state){
        this.id = id;
        this.type = type;
        this.state = state;
    }

    Device(Device original){
        this.id = original.getId();
        this.type = original.getType();
        this.state = original.getState();
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public SectionType getType() {
        return type;
    }

    /**
     * This method should only be used by a an admin user
     */
    public void setType(SectionType type) {
        this.type = type;
    }
    public byte [] getState() {
        return state;
    }
    public void setState(byte [] state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return " Device{" +
                "id=" + id +
                ", type=" + type +
                ", state=" + Base64.getEncoder().encodeToString(state) +
                '}';
    }

    
}
