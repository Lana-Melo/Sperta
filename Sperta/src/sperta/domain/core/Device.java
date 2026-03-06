package sperta.domain.core;

import sperta.utils.SectionType;

public class Device {
    private Long id;
    private SectionType type;
    private Integer state;

    Device(Long id, SectionType t){
        this.id = id;
        this.type = t;
        this.state = 0;
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
    public Integer getState() {
        return state;
    }
    public void setState(Integer state) {
        this.state = state;
    }


    //@Override
    //public boolean equals(Object other){
    //    if (other == null || !other.getClass().equals(Device.class))
    //        return false;
    //    Device device = (Device) other;
    //    return device.getId() == this.getId() &&
    //        device.getType() == this.getType();
    //}
}
