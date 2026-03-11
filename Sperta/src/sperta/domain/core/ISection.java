package sperta.domain.core;

import java.util.List;
import java.util.Map;
import sperta.utils.SectionType;

public interface ISection extends java.io.Serializable {

    Long getContador();

    void setContador(Long contador);

    void setDevices(List<Device> devices);

    Device addDevice();

    void addDevices(int n);

    List<Device> getDevices();

    Device getDevicebyId(Long id);

    void removeDevice(Device d);

    void setMantainers(Map<String, IUser> mantainers);

    Map<String, IUser> getMantainers();

    void addMantainer(IUser user);

    void removeMantainer(IUser user);

    SectionType getType();

    void setType(SectionType t);

    boolean hasMantainer(IUser user);
}