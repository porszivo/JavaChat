package model;

import util.Config;

import java.util.ArrayList;

/**
 * Created by Marcel on 07.11.2016.
 */
public class UserMap {

    private ArrayList<UserModel> map;


    public UserMap(Config userConfig) {

        map = new ArrayList<>();

        for(String key : userConfig.listKeys()) {

            if(key.contains("password")) {

                String username = key.replace(".password", "");
                map.add(new UserModel(username, userConfig.getString(key)));

            }

        }
    }

    public String users() {
        StringBuilder out = new StringBuilder();

        String newline = "";
        for(UserModel user : map) {
            out.append(newline)
                    .append(user.getName())
                    .append(" - ")
                    .append((user.isLoggedIn() ? "online" : "offline"));
            newline = "\n";
        }
        return out.toString();

    }

    public boolean contains(String username) {
        for(UserModel user : map) {
            if(user.getName().equals(username)) return true;
        }
        return false;
    }

    public UserModel getUser(String username) {
        for(UserModel user : map) {
            if(user.getName().equals(username)) return user;
        }
        return null;
    }

    public ArrayList<UserModel> getOnlineUser() {
        ArrayList<UserModel> userMap = new ArrayList<>();
        for(UserModel user : map) {
            if(user.isLoggedIn()) userMap.add(user);
        }
        return userMap;
    }

}
