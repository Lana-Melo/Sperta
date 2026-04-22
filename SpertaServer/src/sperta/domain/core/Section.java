package sperta.domain.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sperta.utils.SectionType;

public class Section implements ISection {

    private Long contador;
    private SectionType type;
    private List<Device> devices;
    private Map<String, IUser> mantainers;

    /**
     * Contador is correspondent to the next available number. If there are no houses, contador should equal to 0
     * @param contador
     */
    public Section(
        SectionType type,
        Long contador,
        Map<String, IUser> mantainers
    ) {
        this.type = type;
        this.contador = contador;
        this.devices = new ArrayList<>();
        this.mantainers = mantainers;
    }

    public Section(ISection original) {
        this.type = original.getType();
        this.contador = original.getContador();
        this.devices = new ArrayList<>();
        this.mantainers = new HashMap<>();
        for (Device d : original.getDevices()) {
            devices.add(new Device(d));
        }
        for (String uId : original.getMantainers().keySet()) {
            this.mantainers.put(uId, new User(this.mantainers.get(uId)));
        }
    }

    public Long getContador() {
        return contador;
    }

    public void setContador(Long contador) {
        this.contador = contador;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public Device addDevice() {
        Device d = new Device(contador++, this.type);
        this.devices.add(d);
        return d;
    }

    public void addDevices(int n) {
        for (int i = 0; i < n; i++) {
            this.addDevice();
        }
    }

    public List<Device> getDevices() {
        return this.devices;
    }

    /**
     * Gets the Device of a certain Type with a given id
     * @param type
     * @param id
     * @return Device if found, else null
     */
    public Device getDevicebyId(Long id) {
        return this.getDevices()
            .stream()
            .filter(device -> device.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public void removeDevice(Device d) {
        this.devices.removeIf(device -> device.getId().equals(d.getId()));
    }

    public void setMantainers(Map<String, IUser> mantainers) {
        this.mantainers = mantainers;
    }

    public Map<String, IUser> getMantainers() {
        return this.mantainers;
    }

    public void addMantainer(IUser user) {
        this.mantainers.put(user.getUserId(), user);
    }

    public void removeMantainer(IUser user) {
        this.mantainers.remove(user.getUserId());
    }

    public SectionType getType() {
        return type;
    }

    public void setType(SectionType t) {
        this.type = t;
    }

    public boolean hasMantainer(IUser user) {
        return this.mantainers.containsKey(user.getUserId());
    }
}
