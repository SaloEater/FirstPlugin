package com.saloeater.voteforcommand;

import java.util.LinkedList;

//import com.coloredcarrot.mcapi.json.*;
//import com.coloredcarrot.mcapi.json.nms.NMSSetupResponse;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by admin1 on 03.07.2016.
 */
public class Main extends JavaPlugin{

    private String[] commandsList;
    private String mainLang;
    private String prefix;

    private int agreeVotes = 0;
    private int disagreeVotes = 0;
    private int voteDuration = 0;
    private int timerTask = -1;
    private int voteTask = -1;

    private boolean voteStarted = false;
    //private boolean voteStopped = false;

    private LinkedList<String> locStrings = new LinkedList<>();
    private LinkedList<String> voteInfoString = new LinkedList<>();
    private LinkedList<String> votedPlayers = new LinkedList<>();
    private LinkedList<String> voteInfo = new LinkedList<>();

    TextComponent voteYes;
    TextComponent voteNo;

    @Override
    public  void onEnable(){
        if (!getDataFolder().exists()) {
            getLogger().info("Create config");
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        prefix = ChatColor.translateAlternateColorCodes('&', "&1[&fVFC&1] ");
        mainLang=getConfig().getString("mainLanguage").replace(" ", "");
        getCommandsFromCFG();
        registerLocalization();
        registerVoteInfoText();

        voteYes = new TextComponent("Yes");
        voteYes.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        voteYes.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/vote agree") );
        voteYes.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(locStrings.get(9)).create() ) );

        voteNo = new TextComponent("No");
        voteNo.setColor(net.md_5.bungee.api.ChatColor.RED);
        voteNo.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/vote disagree") );
        voteNo.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(locStrings.get(10)).create() ) );
    }

    @Override
    public void onDisable(){
        renewDB();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(sender instanceof Player){
            Player player = (Player) sender;
            switch(label){
                case "vote":
                    if(args.length>0) {
                        if (arrContain(args[0].replace(" ", ""))) {
                            String commForVote = getConfig().get(args[0] + ".command").toString().replace(" ", "");
                            String[] argsForVote = getConfig().get(args[0] + ".arguments").toString().replace(" ", "").split(",");
                            if (!voteStarted) {
                                int erChecker = 0;
                                if (argsForVote.length == args.length - 1) {
                                    for (int i = 0; i < argsForVote.length; i++) {
                                        //getLogger().info(args[i + 1].getClass().getName() + "-" + argsForVote[i]);
                                        if (!(args[i + 1].getClass().getName().replace("java.lang.", "").equals(argsForVote[i]))) {
                                            if (!NumberUtils.isNumber(args[i + 1])) {
                                                erChecker = 1;
                                            }
                                        }
                                    }
                                    if (erChecker == 1) {
                                        player.sendMessage(prefix + ChatColor.RED + locStrings.get(8));
                                        getServer().dispatchCommand(sender, "help " + commForVote);
                                        return true;
                                    }
                                    voteStarted = true;
                                    StringBuilder fullCom = new StringBuilder().append(commForVote).append(" ");
                                    for (int i = 1; i < args.length; i++) {
                                        fullCom.append(args[i]).append(" ");
                                    }
                                    getDurFromCFG(args[0]);
                                    voteInfo.add(player.getName());
                                    voteInfo.add(fullCom.toString());
                                    voteInfo.add(getConfig().getString(args[0] + ".timeForVote").replace(" ", "").replace(",", " "));
                                    startVote(commForVote, args);
                                    getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + ChatColor.GRAY + (mainLang.equals("en") ? " create vote!" : " создал голосование!"));
                                    for (org.bukkit.entity.Player player1 : getServer().getOnlinePlayers()) {
                                        player1.performCommand("vote info");
                                    }
                                    return true;
                                }
                            } else {
                                player.sendMessage(prefix + ChatColor.RED + locStrings.get(0));
                                return true;
                            }
                            player.sendMessage(prefix + ChatColor.RED + locStrings.get(8));
                            getServer().dispatchCommand(sender, "help " + commForVote);
                            return true;
                        } else {
                            switch (args[0]) {
                                case "agree":
                                    if (voteStarted) {
                                        if (!votedPlayers.contains(player.getName())) {
                                            agreeVotes++;
                                            getConfig().set("vote." + player.getName(), true);
                                            votedPlayers.add(player.getName());
                                            player.sendMessage(prefix + ChatColor.GREEN + locStrings.get(6));
                                            player.sendMessage(prefix + ChatColor.GRAY + "Current result: " + ChatColor.GREEN + agreeVotes + ChatColor.GRAY + " : " + ChatColor.RED + disagreeVotes);
                                            return true;
                                        } else {
                                            player.sendMessage(prefix + ChatColor.RED + locStrings.get(5));
                                            return true;
                                        }
                                    } else {
                                        player.sendMessage(prefix + ChatColor.RED + locStrings.get(11));
                                    }
                                    return true;

                                case "disagree":
                                    if (voteStarted) {
                                        if (!votedPlayers.contains(player.getName())) {
                                            disagreeVotes++;
                                            votedPlayers.add(player.getName());
                                            player.sendMessage(prefix + ChatColor.RED + locStrings.get(7));
                                            player.sendMessage(prefix + ChatColor.GRAY + "Current result: " + ChatColor.GREEN + agreeVotes + ChatColor.GRAY + " - " + ChatColor.RED + disagreeVotes);
                                            return true;
                                        } else {
                                            player.sendMessage(prefix + ChatColor.RED + locStrings.get(5));
                                            return true;
                                        }
                                    } else {
                                        player.sendMessage(prefix + ChatColor.RED + locStrings.get(11));
                                    }
                                    return true;

                                case "info":
                                    if (voteStarted) {
                                        player.sendMessage(prefix + ChatColor.GRAY + "Vote Information:");
                                        for (int i = 0; i < voteInfo.size(); i++) {
                                            player.sendMessage(prefix + ChatColor.RED + "#" + voteInfoString.get(i) + ChatColor.GRAY + " - " + voteInfo.get(i));
                                        }
                                        player.sendMessage(prefix + ChatColor.GRAY + "Current result: " + ChatColor.GREEN + agreeVotes + ChatColor.WHITE + " - " + ChatColor.RED + disagreeVotes);
                                        if (!votedPlayers.contains(player.getName())) {
                                            player.sendMessage(prefix + ChatColor.GRAY + "You still didn't vote. Vote: ");
                                            player.sendMessage(ChatColor.DARK_AQUA + "====");
                                            player.spigot().sendMessage(voteYes);
                                            player.sendMessage(ChatColor.DARK_AQUA + "====");
                                            player.spigot().sendMessage(voteNo);
                                            player.sendMessage(ChatColor.DARK_AQUA + "====");
                                        }
                                    } else {
                                        player.sendMessage(prefix + ChatColor.RED + locStrings.get(11));
                                    }
                                    return true;

                                case "help":
                                    player.sendMessage(prefix + ChatColor.GRAY + "To create a vote write: " + ChatColor.AQUA + "/vote \"vote\" \"arguments\"");
                                    player.sendMessage(prefix + ChatColor.GRAY + "Below posted commands to voting");
                                    player.sendMessage(prefix + ChatColor.GRAY + "Type " + ChatColor.AQUA + "/help \"command\"" + ChatColor.GRAY + " to acquire information about command");
                                    player.sendMessage(ChatColor.RED + "->" + ChatColor.WHITE + "vote" + ChatColor.GRAY + " - " + ChatColor.WHITE + "command" + ChatColor.RED + "<-");
                                    for (int i = 0; i < commandsList.length; i++) {
                                        player.sendMessage(ChatColor.RED + "#" + ChatColor.WHITE + commandsList[i] + ChatColor.GRAY + " - " + ChatColor.WHITE + getConfig().get(commandsList[i] + ".command").toString().replace(" ", ""));
                                    }
                                    return true;

                                case "stop":
                                    if (player.hasPermission("vote.stop")) {
                                        if (voteStarted) {
                                            getServer().broadcastMessage(prefix + ChatColor.YELLOW + player.getName() + ChatColor.RED + " stop the vote!");
                                            voteStarted = false;
                                            agreeVotes=0;
                                            disagreeVotes=0;
                                            voteDuration=0;
                                        } else {
                                            player.sendMessage(prefix + ChatColor.RED + "There is no vote to stop");
                                        }
                                    }
                                    return true;

                                default:
                                    player.sendMessage(prefix + ChatColor.RED + "No such command.");
                                    player.performCommand("vote help");
                                    return true;
                            }
                        }
                    }
                    break;
            }
        }
        return true;
    }

    private void getDurFromCFG(String command){
        String[] obgTime = getConfig().getString(command+".timeForVote").replace(" ", "").split(",");
        for(int i=0; i<obgTime.length; i++){
            /*if(obgTime[i].contains("h")){
                obgTime[i]=obgTime[i].replace("h","");
                voteDuration +=(Integer.valueOf(obgTime[i])*3600);
            }*/
            if(obgTime[i].contains("m")){
                obgTime[i]=obgTime[i].replace("m","");
                voteDuration +=(Integer.valueOf(obgTime[i])*60);
            }
            if(obgTime[i].contains("s")){
                obgTime[i]=obgTime[i].replace("s","");
                voteDuration +=(Integer.valueOf(obgTime[i]));
            }
        }
        getLogger().info(String.valueOf(voteDuration));
    }

    private void getCommandsFromCFG(){
       commandsList=getConfig().getString("commands").replace(" ", "").split(",");
    }

    private void registerLocalization() {
        locStrings.add(getConfig().get("locale.voteStarted").toString()); //0
        locStrings.add(getConfig().get("locale.voteNotPass").toString()); //1
        locStrings.add(getConfig().get("locale.votePass").toString()); //2
        locStrings.add(" "); //3
        locStrings.add(" "); //4
        locStrings.add(getConfig().get("locale.cantVoteVoted").toString()); //5
        locStrings.add(getConfig().get("locale.agreeVote").toString()); //6
        locStrings.add(getConfig().get("locale.disagreeVote").toString()); //7
        locStrings.add(getConfig().get("locale.wrongCom").toString()); //8
        locStrings.add(getConfig().get("locale.clickToAgree").toString()); //9
        locStrings.add(getConfig().get("locale.clickToDisagree").toString()); //10
        locStrings.add(getConfig().get("locale.lackOfVote").toString()); //11
    }

    private void registerVoteInfoText(){
        voteInfoString.add(mainLang.equals("en")?"Nickname:":"Ник:");
        voteInfoString.add(mainLang.equals("en")?"Command:":"Команда:");
        voteInfoString.add(mainLang.equals("en")?"Time left:":"Оставшееся время:");
    }

    private boolean arrContain(String command){
        for(int i=0; i<commandsList.length; i++){
            if(commandsList[i].equalsIgnoreCase(command)) return true;
        }
        return false;
    }

    private void dispatchCommand(String command, String[] args){
        StringBuilder finishCom = new StringBuilder();
        finishCom.append(command).append(" ");
        for(int i=1; i<args.length; i++){
            finishCom.append(args[i]).append(" ");
        }
        getServer().dispatchCommand(getServer().getConsoleSender(), finishCom.toString());
    }

    private void startVote(String command, String[] args){
        voteTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.getScheduler().cancelTask(timerTask);
            voteStarted=false;
            agreeVotes=0;
            disagreeVotes=0;
            voteDuration=0;
            if(agreeVotes>=disagreeVotes&&agreeVotes>0){
                getServer().broadcastMessage(prefix + ChatColor.GREEN + locStrings.get(2));
                dispatchCommand(command, args);
            } else {
                getServer().broadcastMessage(prefix + ChatColor.RED + locStrings.get(1));
            }
            renewDB();
        }, voteDuration*20).getTaskId();
        timerTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int minutes = 0;
            int seconds = 0;
            @Override
            public void run() {
                if (!voteStarted){
                    Bukkit.getScheduler().cancelTask(timerTask);
                    Bukkit.getScheduler().cancelTask(voteTask);
                    voteDuration=0;
                    renewDB();
                }
                voteInfo.set(2, ((voteDuration-minutes*60-seconds)/60>=1?((voteDuration-minutes*60-seconds)/60 + "m "):"") + (voteDuration-minutes*60-seconds)%60 + "s");
                if(voteDuration-seconds==30)getServer().broadcastMessage(prefix + ChatColor.RED + "30 " + ChatColor.WHITE + "seconds remaining");
                if(voteDuration-seconds==10)getServer().broadcastMessage(prefix + ChatColor.RED + "10 " + ChatColor.WHITE + "seconds remaining");
                if(seconds == 60){
                    minutes += 1;
                    seconds = 0;
                    getServer().broadcastMessage(prefix + ChatColor.GRAY + "Time left for vote: " + ChatColor.RED + (voteDuration-minutes*60-seconds)/60 + ChatColor.GRAY + "m " + ChatColor.RED + (voteDuration-minutes*60-seconds)%60 + ChatColor.GRAY +"s");
                }
                //if(seconds%60==0)getServer().broadcastMessage("Time left for vote: " + (voteDuration-minutes*60-seconds)/60 + "m " + (voteDuration-minutes*60-seconds)%60 + "s");
                seconds++;

            }
        }, 20, 20).getTaskId();
    }

    private void renewDB(){
        for(int i=0; i<votedPlayers.size(); i++){
            votedPlayers.remove(i);
        }
        for(int i=0; i<voteInfo.size(); i++){
            voteInfo.remove(i);
        }
        for(int i=0; i<voteInfo.size(); i++){
            voteInfo.remove(i);
        }
    }
}
