package sperta.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import sperta.domain.core.House;
import sperta.domain.core.ISection;
import sperta.domain.core.IUser;
import sperta.utils.LogEntry;
import sperta.utils.Operation;
import sperta.utils.SectionType;

public class SecurityService {

    private static final Set<Operation> OWNER_OPERATIONS = 
        Collections.unmodifiableSet(EnumSet.of(
            Operation.CREATE,
            Operation.ADD,
            Operation.RD
        ));

    public static boolean checkUserActionPermissions(LogEntry log){
        //create, add and register device
        if (log.getHouse().getOwner().equals(log.getUser())) return true; //checks if it's owner
        if (OWNER_OPERATIONS.contains(log.getOperation())) return false; //checks if a non-user is trying to do a owner action

        //enviar valor
        ISection section = log.getHouse().getSection(log.getSectionType());
        if (section != null && section.hasMantainer(log.getUser())) return true; //check if it's trying to access a section and if has permissions over it

        //check RH -> checking if user has access to the device's section in that house
        section = log.getHouse().getSection(log.getDevice().getType());
        if (section != null && section.hasMantainer(log.getUser())) return true;
        
        //check RT
        if(log.getOperation().equals(Operation.RT)) return false; //it's the only operation available, if this is not the one we're verifying, something's wrong
        for (ISection s : log.getHouse().getSections().values()){
            if (s.hasMantainer(log.getUser())) return true;
        }

        return false;
    }

    /**
     * Returns the types of sections that user has access over in the given house
     * @param h
     * @param u
     * @return
     */
    public List<SectionType> getUserAccessibleSectionTypes(House h, IUser u){
        return h.getSections().values().stream()
                .filter(section -> section.hasMantainer(u))
                .map(s -> s.getType())
                .toList();
    }

}
