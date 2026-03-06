package sperta.domain.core;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sperta.utils.SectionType;

public class Home {

    private static Long counter; 

    private Long id;
    private String name;
    private IUser owner;
    private Map<SectionType, ISection> sections;

    public Home(IUser owner){
        this.owner = owner;
        this.id = counter;
        this.name = "Casa";
        this.sections = makeSections();

        counter++;
    }

    public Home(IUser owner, String name, Long id, Map<SectionType, ISection> sections){
        this.owner = owner;
        this.id = id;
        this.name = name;
        this.sections = sections;
    }


    private Map<SectionType, ISection> makeSections(){
        Map<SectionType, ISection> map = new EnumMap<>(SectionType.class);
        for (SectionType type: SectionType.values()){
            if (type == SectionType.ALL) continue; //skip "all"
            ISection s = new Section(type, 0L , new HashMap<>());
            map.put(type, s);
        }
        return map;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IUser getOwner() {
        return owner;
    }

    public void setOwner(IUser owner) {
        this.owner = owner;
    }

    public Map<SectionType, ISection> getSections() {
        return sections;
    }

    public void setSections(Map<SectionType, ISection> sections) {
        this.sections = sections;
    }

    public void addDevice(SectionType type){
        this.getSections().get(type).addDevice();
    }

    public void addDevicesOfType(int n, SectionType type){
        this.getSections().get(type).addDevices(n);
    }

    public List<Device> getSectionDevices(SectionType type){
        return this.getSections().get(type).getDevices();
    }

    public void addMantainerToSection(IUser mantainer, SectionType type){
        this.getSections().get(type).addMantainer(mantainer);
    }

    public void removeMantainerFromSection(IUser mantainer, SectionType type){
        this.getSections().get(type).removeMantainer(mantainer);
    }

    public void removeDeviceFromSection(Device d, SectionType type){
        this.getSections().get(type).removeDevice(d);
    }

    public Device getDeviceFromSectionById(Long id, SectionType type){
        return this.getSections().get(type).getDevicebyId(id);
    }

}
