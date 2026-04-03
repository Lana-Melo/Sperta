package sperta.utils;

public enum SectionType {
    ELECTROS, GARDEN, LUZES, MULTIMEDIA, PORTAS, STORES, ALL;

    public String getLabel() {
        switch (this) {
            case ELECTROS: return "E";
            case GARDEN: return "G";
            case LUZES: return "L";
            case MULTIMEDIA: return "M";
            case PORTAS: return "P";
            case STORES: return "S";
            case ALL: return "all";
            default: throw new IllegalStateException();
        }
    }

    public SectionType getEnum(String label){
        switch (label) {
            case "E": return  ELECTROS;
            case "G": return GARDEN;
            case "L": return LUZES;
            case "M": return MULTIMEDIA;
            case "P": return PORTAS;
            case "S": return STORES;
            case "all": return ALL;
            default: return null;
        }
        }
}
