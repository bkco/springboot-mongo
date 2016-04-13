package co.bk.springbootmongo.cursor;

import java.util.StringJoiner;

/**
 * Class representing a player in a band.
 */
public class Player {

    private String id;
    private String name;
    private String instrument;
    private String creationDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString(){
        return "id: " + getId() + " name: " + getName() + " instrument: " + getInstrument() + " creationDate: " + getCreationDate();
    }

    public String toCsvString() {
        return String.join(",", getId(), getName(), getInstrument(), getCreationDate() );
    }

    public static String getCsvHeader() {
        StringJoiner joiner = new StringJoiner(",");
        joiner.add("id");
        joiner.add("name");
        joiner.add("instrument");
        joiner.add("creationDate");
        return joiner.toString();
    }

}
