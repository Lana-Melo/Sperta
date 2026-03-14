package sperta.utils;

import java.time.LocalDateTime;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.domain.core.Device;

/**
 * <p>This class represents a log entry, whenever calling LoggerService, you should create a LogEntry.
 * There are four mandatory attributes for all types of operations (they are in  the LogEntry's constructor).</p>
 * <p><b> Make sure that you allways add every attribute for each respective operation! </b></p>
 * 
 * <div> 
    <ul>
        <li>CREATE _hm_ # Criar casa _hm_ - utilizador é Owner</li>
        <li> ADD _user1_ _hm_ _s_ # Adicionar utilizador <user1> à casa <hm>, seção <s>.</li>
        <li>RD _hm_ _s_ # Registar um Dispositivo na casa <hm>, na seção <s></li>
        <li>EC _hm_ _d_ _int_ # Enviar valor <int> de estado/temporização, do dispositivo <d> da casa <hm>, para o servidor.</li>
        <li> RT _hm_ # Receber a informação sobre o último comando (estados/temporizações) enviado a cada dispositivo da casa <hm>, desde que o utilizador tenha permissões.</li>
        <li>RH _hm_ _d_ # Receber o Histórico (ficheiro de log .csv) de comandos enviados ao dispositivo <d> da casa <hm>, desde que o utilizador tenha permissões.</li>
 * </ul>
 * </div>
 */
public class LogEntry {
    private LocalDateTime timestamp;
    private IUser user;
    private Operation operation;
    private House house;

    private IUser addedUser;
    private SectionType sectionType;
    private Device device;
    private Integer devicesValue;

    public LogEntry(LocalDateTime timestamp, IUser user, Operation op, House house){
        this.timestamp = timestamp;
        this.user = user;
        this.operation = op;
        this.house = house;

        this.addedUser = null;
        this.sectionType = null;
        this.device = null;
        this.devicesValue = null;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public IUser getUser() {
        return user;
    }

    public void setUser(IUser user) {
        this.user = user;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public House getHouse() {
        return house;
    }

    public void setHouse(House home) {
        this.house = home;
    }

    public IUser getAddedUser() {
        return addedUser;
    }

    public void setAddedUser(IUser addedUser) {
        this.addedUser = addedUser;
    }

    public SectionType getSectionType() {
        return sectionType;
    }

    public void setSectionType(SectionType sectionType) {
        this.sectionType = sectionType;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Integer getDevicesValue() {
        return devicesValue;
    }

    public void setDevicesValue(int devicesValue) {
        this.devicesValue = devicesValue;
    }

    public String getCsvString() {
        return new StringBuilder()
            .append(timestamp.toString()).append(",")
            .append(user.toString()).append(",")
            .append(operation.toString()).append(",")
            .append(house.getName()).append(",")
            .append(addedUser).append(",")
            .append(sectionType).append(",")
            .append(device).append(",")
            .append(devicesValue)
            .append("\n")
            .toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(timestamp+ " " + user+ " " + operation+ " " + house+ " ");
        if (operation == Operation.CREATE){
            sb.append("# Criar Casa");
        } else if (operation == Operation.ADD){
            sb = new StringBuilder(timestamp+ " " + user+ " " + operation+ " " + addedUser+ " " + house + " " + sectionType + " # Adicionar Utilizador");
        } else if (operation == Operation.RD){
            sb.append(sectionType + " # Registar Dispositivo");
        } else if (operation == Operation.EC){
            sb.append(device + " " + devicesValue + " # Editar Valor");
        } else if (operation == Operation.RT){
            sb.append("# Receber Info sobre Casa");
        } else if (operation == Operation.RH){
            sb.append(device + " # Receber Histórico de Dispositivo");
        } else {
            sb.append("# OP UNDEFINED");
        }
        return sb.toString();
    }


}
