package sperta.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import sperta.domain.core.House;
import sperta.domain.core.ISection;
import sperta.domain.core.IUser;
import sperta.utils.LogEntry;
import sperta.utils.Operation;
import sperta.utils.SectionType;

public enum SecurityService {
    INSTANCE;

    private static final Random RANDOM = new SecureRandom();

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
        //System.out.println(section != null);
        //System.out.println(section.hasMantainer(log.getUser()));
        if (section != null && section.hasMantainer(log.getUser())) return true; //check if it's trying to access a section and if has permissions over it

        if (OWNER_OPERATIONS.contains(log.getOperation())){

        } else if(log.getOperation() == Operation.RH){
            section = log.getHouse().getSection(log.getDevice().getType());
            if (section != null && section.hasMantainer(log.getUser())) return true;
        } else if (log.getOperation() == Operation.RT) {
            for (ISection s : log.getHouse().getSections().values()){
                if (s.hasMantainer(log.getUser())) return true;
            }
        }

        return false;
    }

    /**
     * Returns the types of sections that user has access over in the given house
     * @param h
     * @param u
     * @return
     */
    public static List<SectionType> getUserAccessibleSectionTypes(House h, IUser u){
        return h.getSections().values().stream()  //Should check if user is the house's owner //TODO
                .filter(section -> section.hasMantainer(u))
                .map(s -> s.getType())
                .toList();
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
  }

    public static byte[] generateHashedPass(byte[] salt, String passwd) {
        byte [] buf = new byte[salt.length+passwd.getBytes().length];
        System.arraycopy(passwd.getBytes(), 0, buf, 0, passwd.getBytes().length);
        System.arraycopy(salt, 0, buf, passwd.getBytes().length, salt.length);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte [] result = md.digest(buf);
            return result;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }



}
