package model;

public class UserModelNS {

    private String name;
    private String ip;
    private int port;

    public UserModelNS(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return name + "\t" + ip + ":" + port;
    }
}
