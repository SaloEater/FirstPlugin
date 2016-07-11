package com.salo.testfp;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by admin1 on 03.07.2016.
 */
public class Main extends JavaPlugin{

    private String[] commandsList;

    private int agreeVotes = 0;
    private int disagreeVotes = 0;
    private int voteDuration;
    private int taskId = -1;

    private boolean voteStarted = false;
    private boolean voteStopped = false;

    private LinkedList<String> votedPlayers = new LinkedList<String>();

    @Override
    public  void onEnable(){
        if (!this.getConfig().contains("commands")) {
            getLogger().info("Create config");
            registerConfig();
        }
        commandsList = this.getConfig().get("commands").toString().replace(" ", "").split(",");
        for(int i=0; i<commandsList.length; i++){
            getLogger().info(commandsList[i]);
        }
        voteDuration = getDurFromCFG();
    }

    public boolean PlayerJoinEvent(Player p, String mOnJoin){

        return false;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if((Player) sender instanceof Player){
            Player player = (Player) sender;
            switch(label){
                case "vote":
                    String commForVote = this.getConfig().get(args[0] + ".command").toString().replace(" ", "");
                    String[] argsForVote = this.getConfig().get(args[0] + ".arguments").toString().replace(" ", "").split(",");
                    if(arrContain(args[0].replace(" ", ""))){
                        if(!voteStarted) {
                            int erChecker = 0;
                            if (argsForVote.length == args.length - 1) {
                                for (int i = 0; i < argsForVote.length; i++) {
                                    getLogger().info(args[i + 1].getClass().getName() + "-" + argsForVote[i]);
                                    if (!(args[i + 1].getClass().getName().replace("java.lang.", "").equals(argsForVote[i]))) {
                                        if (!NumberUtils.isNumber(args[i + 1])) {
                                            erChecker = 1;
                                        }
                                    }
                                }
                                if (erChecker == 1) {
                                    String wrongVoteWarn = new StringBuilder().append(ChatColor.RED + "Wrong vote.").toString();// + ChatColor.COLOR_CHAR + " Correct: " + "/vote " + ChatColor.DARK_BLUE).append(args[0].toString()).append(" ").append(buildArgs(argsForVote)).toString();
                                    sender.sendMessage(ChatColor.RED + wrongVoteWarn);
                                    getServer().dispatchCommand(sender, "help " + commForVote);
                                    return false;
                                }
                                voteStarted = true;
                                startVote(commForVote, args);
                                return true;
                                //dispatchCommand(commForVote, args);
                            }

                        } else {
                            sender.sendMessage(ChatColor.RED + "Vote already started");
                        }
                        player.sendMessage("Wrond command :/ Check help");
                        getServer().dispatchCommand(sender, "help " + commForVote);
                        return true;
                    } else {
                        switch (args[0]) {
                            case "agree":
                                if(voteStarted){
                                    if(this.getConfig().get("vote." + sender.getName())==null) {
                                        agreeVotes++;
                                        this.getConfig().set("vote." + sender.getName(), true);
                                        votedPlayers.add(sender.getName());
                                        sender.sendMessage(ChatColor.GREEN+"You agree with vote");
                                        sender.sendMessage("Current result: " + ChatColor.GREEN + agreeVotes + " " + ChatColor.COLOR_CHAR + " : " + ChatColor.RED + disagreeVotes);
                                        return true;
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "You already voted");
                                        return true;
                                    }
                                }
                                sender.sendMessage(ChatColor.RED + "Vote already started.");
                                return true;

                            case "disagree":
                                if(voteStarted){
                                    if(this.getConfig().get("vote." + sender.getName())==null) {
                                        disagreeVotes++;
                                        this.getConfig().set("vote." + sender.getName(), false);
                                        votedPlayers.add(sender.getName());
                                        sender.sendMessage(ChatColor.DARK_RED+"You disagree with vote");
                                        return true;
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "You already voted");
                                        return true;
                                    }
                                }
                                sender.sendMessage(ChatColor.RED + "Vote already started.");
                                return true;

                            default:
                                player.sendMessage("Wrond command :/ Check help");
                                getServer().dispatchCommand(sender, "help " + commForVote);
                                break;

                        }
                    }
                    break;
            }
        }
        return false;
    }

    private void registerConfig() {
       /*try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info("Config.yml not found, creating!");
                file.createNewFile();
            } else {
                getLogger().info("Config.yml found, loading!");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }*/
        this.getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private int getDurFromCFG(){
        int time=0;
        String[] obgTime = getConfig().getString("timeForVote").replace(" ", "").split(",");
        for(int i=0; i<obgTime.length; i++){
            if(obgTime[i].contains("d")){
                obgTime[i]=obgTime[i].replace("d","");
                time+=(Integer.valueOf(obgTime[i])*3600);
            }
            if(obgTime[i].contains("m")){
                obgTime[i]=obgTime[i].replace("m","");
                time+=(Integer.valueOf(obgTime[i])*60);
            }
            if(obgTime[i].contains("s")){
                obgTime[i]=obgTime[i].replace("s","");
                time+=(Integer.valueOf(obgTime[i]));
            }
        }
        getLogger().info(String.valueOf(time));
        return time;
    }

    private boolean arrContain(String command){
        for(int i=0; i<commandsList.length; i++){
            if(commandsList[i].equals(command)) return true;
        }
        return false;
    }

    private String buildArgs(String[] argsInVote){
        StringBuilder argsBuilder = new StringBuilder();
        for(int i=0; i< argsInVote.length; i++){
            argsBuilder.append(argsInVote[i]).append(" ");
        }
        return argsBuilder.toString();
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
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                voteStopped=true;
                if(agreeVotes>=disagreeVotes){
                    getServer().broadcastMessage("Vote Passed");
                    dispatchCommand(command, args);
                } else {
                    getServer().broadcastMessage("Vote Not Passed");
                }
                agreeVotes=0;
                disagreeVotes=0;
                clearConfig();
            }
        }, voteDuration*20);
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                voteStopped=true;
                if(agreeVotes>=disagreeVotes){
                    getServer().broadcastMessage("Vote Passed");
                    dispatchCommand(command, args);
                } else {
                    getServer().broadcastMessage("Vote Not Passed");
                }
                agreeVotes=0;
                disagreeVotes=0;
                clearConfig();
            }
        }, voteDuration*20);
        taskId = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int minutes = 0;
            int seconds = 0;
            @Override
            public void run() {
                if (voteStopped)Bukkit.getScheduler().cancelTask(taskId);
                if((voteDuration-minutes*60-seconds)<0)Bukkit.getScheduler().cancelTask(taskId);
                if(voteDuration-seconds==30)getServer().broadcastMessage("30 seconds remaining");
                if(voteDuration-seconds==10)getServer().broadcastMessage("10 seconds remaining");
                if(seconds == 60){
                    minutes += 1;
                    seconds = 0;
                }
                if(seconds%10==0)getServer().broadcastMessage("Time left for vote: " + (voteDuration-minutes*60-seconds)/60 + "m " + (voteDuration-minutes*60-seconds)%60 + "s");
                seconds++;
            }
        }, 20, 20).getTaskId();


    }

    private void clearConfig(){
        for(int i=0; i<votedPlayers.size(); i++){
            getConfig().set("vote."+votedPlayers.get(i), null);
            votedPlayers.remove(i);
        }
    }

}
