package sperta.domain.core;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sperta.utils.SectionType;

public class House implements java.io.Serializable{

    private String name;
    private IUser owner;
    private Map<SectionType, ISection> sections;

    public House(IUser owner, String name){
        this.owner = owner;
        this.name = name;
        this.sections = makeSections();
    }

    public House(House original){
        this.owner = original.owner;
        this.name = original.name;
        this.sections = makeSections();
        for (ISection s : original.getSections().values()){
            this.sections.put(s.getType(), new Section(s));
        }
    }

    private Map<SectionType, ISection> makeSections(){
        Map<SectionType, ISection> map = new EnumMap<>(SectionType.class);
        for (SectionType type: SectionType.values()){
            //if (type == SectionType.ALL) continue; //skip "all" //We're not skipping anymore
            ISection s = new Section(type, 1L , new HashMap<>());
            map.put(type, s);
        }
        return map;
    }

    public String getId() {
        return getName(); //At this time, the house's id is it's name
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

    public ISection getSection(SectionType type){
        return sections.get(type);
    }

    public void setSections(Map<SectionType, ISection> sections) {
        this.sections = sections;
    }

    public Device addDevice(SectionType type){
        return this.getSections().get(type).addDevice();
    }

    public void addDevicesOfType(int n, SectionType type){
        this.getSections().get(type).addDevices(n);
    }

    public List<Device> getSectionDevices(SectionType type){
        return this.getSections().get(type).getDevices();
    }

    /**
     * This function inserts a mantainer in the section of given type. 
     * It does have some domain rules:    PS:(it shouldn't though, but since this is a short term project....)
     *  - When a mantainer is inserted and type = ALL -> the user is added to all sections and section ALL
     *  - When type != ALL:
     *      - If user already in ALL -> User is removed from all sections (including ALL) 
     *                                  and is inserted 
     *      - Else ->  User is added to said section
     * @param mantainer
     * @param type
     */
    public void addMantainerToSection(IUser mantainer, SectionType type){
        if(type == SectionType.ALL) {addMantainerToAllSections(mantainer); return;}
        if(sections.get(SectionType.ALL).hasMantainer(mantainer))
            removeMantainerFromAllSections(mantainer);
        this.getSections().get(type).addMantainer(mantainer);
    }

    public void addMantainerToAllSections(IUser mantainer){
        this.getSections().forEach((k,v) -> { v.addMantainer(mantainer);});
    }

    public void removeMantainerFromAllSections(IUser mantainer){
        for (SectionType type : SectionType.values()){removeMantainerFromSection(mantainer, type);}
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

    @Override
    public String toString() {
        StringBuilder houseString = new StringBuilder()
            .append("House [name=").append(name)
            .append(", owner=") .append(owner.getUserId())
            .append("]");
        for (SectionType type : this.getSections().keySet()) {
            if (type == SectionType.ALL) continue;
            houseString.append("\n  Section ").append(type).append(":");
            for (Device d : this.getSections().get(type).getDevices()) {
                houseString.append("\n    ").append(d);
            }
        }
        return houseString.toString();
    }

}