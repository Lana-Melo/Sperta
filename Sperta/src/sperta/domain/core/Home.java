package sperta.domain.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import sperta.utils.SectionType;

public class Home {

    private static long counter; 

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


}
