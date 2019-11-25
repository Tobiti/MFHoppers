package net.squidstudios.mfhoppers.util.cmd;

public class Cmd {
    private String[] args;
    private Sender sender;
    public Cmd(String[] args, Sender sender){
        this.args = args;
        this.sender = sender;
    }

    public Sender getSender() {
        return sender;
    }

    public String[] args() {
        return args;
    }
}
