package com.saloeater.voteforcommand;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

    private int agreeVotes = 0;
    private int disagreeVotes = 0;
    private int voteDuration;
    private int timerTask = -1;
    private int voteTask = -1;

    private boolean voteStarted = false;
    private boolean voteStopped = false;

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
        getLogger().info("Plugin enabled");
        mainLang=getConfig().getString("mainLanguage").replace(" ", "");
        getDurFromCFG();
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
        clearConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(sender instanceof Player){
            Player player = (Player) sender;
            switch(label){
                case "vote":
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
                                    //player.sendMessage(locStrings.get(8));
                                    getServer().dispatchCommand(sender, "help " + commForVote);
                                    return true;
                                }
                                voteStarted = true;
                                voteInfo.add(player.getName());
                                StringBuilder fullCom = new StringBuilder().append(commForVote).append(" ");
                                for(int i=1; i<args.length; i++){
                                    fullCom.append(args[i]).append(" ");
                                }
                                voteInfo.add(fullCom.toString());
                                voteInfo.add(getConfig().getString("timeForVote").replace(" ", "").replace(",", " "));
                                startVote(commForVote, args);

                                getServer().broadcastMessage(player.getName()+(mainLang.equals("en")?" create vote!":" создал голосование!"));
                                for (org.bukkit.entity.Player player1 : getServer().getOnlinePlayers()){
                                    player1.performCommand("vote info");
                                    /*player1.spigot().sendMessage(voteYes);
                                    player1.spigot().sendMessage(voteNo);*/
                                }




                                return true;
                            }
                        } else {
                             player.sendMessage(ChatColor.RED + locStrings.get(0));
                        }
                        player.sendMessage(ChatColor.RED + locStrings.get(8));
                        getServer().dispatchCommand(sender, "help " + commForVote);
                        return true;
                    } else {
                        switch (args[0]) {
                            case "agree":
                                if (voteStarted) {
                                    if(!votedPlayers.contains(player.getName())){
                                        agreeVotes++;
                                        getConfig().set("vote." + player.getName(), true);
                                        votedPlayers.add(player.getName());
                                         player.sendMessage(ChatColor.GREEN + locStrings.get(6));
                                         player.sendMessage("Current result: " + ChatColor.GREEN + agreeVotes + ChatColor.WHITE + " : " + ChatColor.RED + disagreeVotes);
                                        return true;
                                    } else {
                                         player.sendMessage(ChatColor.RED + locStrings.get(5));
                                        return true;
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + locStrings.get(11));
                                }
                                return true;

                            case "disagree":
                                if (voteStarted) {
                                    if(!votedPlayers.contains(player.getName())){
                                        disagreeVotes++;
                                        votedPlayers.add(player.getName());
                                         player.sendMessage(ChatColor.DARK_RED + locStrings.get(7));
                                        player.sendMessage("Current result: " + ChatColor.GREEN + agreeVotes + ChatColor.WHITE + " - " + ChatColor.RED + disagreeVotes);
                                        return true;
                                    } else {
                                         player.sendMessage(ChatColor.RED + locStrings.get(5));
                                        return true;
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + locStrings.get(11));
                                }
                                return true;

                            case "info":
                                if (voteStarted) {
                                    player.sendMessage("Vote Information:");
                                    for(int i=0; i<voteInfo.size(); i++){
                                        player.sendMessage(voteInfoString.get(i) + " - " + voteInfo.get(i));
                                    }
                                    player.sendMessage("Current result: " + ChatColor.GREEN + agreeVotes + ChatColor.WHITE + " - " + ChatColor.RED + disagreeVotes);
                                    if(!votedPlayers.contains(player.getName())){
                                        player.sendMessage("You still didn't vote. Vote: ");
                                        player.sendMessage("");
                                        player.spigot().sendMessage(voteYes);
                                        player.sendMessage("OR");
                                        player.spigot().sendMessage(voteNo);
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + locStrings.get(11));
                                }
                                return true;

                            case "help":
                                player.sendMessage("Below posted commands to voting. Type /help \"command\"");
                                player.sendMessage("vote - command");
                                for (int i = 0; i < commandsList.length; i++) {
                                    player.sendMessage(commandsList[i] + " - " + getConfig().get(commandsList[i] + ".command").toString().replace(" ", ""));
                                }
                                return true;

                            default:
                                player.sendMessage(ChatColor.RED + locStrings.get(8));
                                return true;
                        }
                    }
            }
        }
        return true;
    }

    private void getDurFromCFG(){
        int voteDuration =0;
        String[] obgTime = getConfig().getString("timeForVote").replace(" ", "").split(",");
        for(int i=0; i<obgTime.length; i++){
            if(obgTime[i].contains("d")){
                obgTime[i]=obgTime[i].replace("d","");
                voteDuration +=(Integer.valueOf(obgTime[i])*3600);
            }
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
        this.voteDuration=voteDuration;
    }

    private void getCommandsFromCFG(){
       commandsList=getConfig().getString("commands").replace(" ", "").split(",");
    }

    private void registerLocalization() {
        locStrings.add(getConfig().get("locale.voteStarted").toString()); //0
        locStrings.add(getConfig().get("locale.voteNotPass").toString()); //1
        locStrings.add(getConfig().get("locale.votePass").toString()); //2
        locStrings.add(getConfig().get("locale.votedSucc").toString()); //3
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
        voteInfoString.add((mainLang.equals("en")?"Nickname: ":"Ник: "));
        voteInfoString.add((mainLang.equals("en")?"Command: ":"Команда: "));
        voteInfoString.add((mainLang.equals("en")?"Time left: ":"Оставшееся время: "));
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
            voteStopped=true;
            if(agreeVotes>=disagreeVotes){
                getServer().broadcastMessage(locStrings.get(2));
                dispatchCommand(command, args);
            } else {
                getServer().broadcastMessage(locStrings.get(1));
            }
            agreeVotes=0;
            disagreeVotes=0;
            clearConfig();
        }, voteDuration*20).getTaskId();
        timerTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int minutes = 0;
            int seconds = 0;
            @Override
            public void run() {
                if (voteStopped){
                    Bukkit.getScheduler().cancelTask(timerTask);
                    Bukkit.getScheduler().cancelTask(voteTask);
                }
                if((voteDuration-minutes*60-seconds)<0)Bukkit.getScheduler().cancelTask(timerTask);
                if(voteDuration-seconds==30)getServer().broadcastMessage(ChatColor.RED + "30 " + ChatColor.WHITE + "seconds remaining");
                if(voteDuration-seconds==10)getServer().broadcastMessage(ChatColor.RED + "10 " + ChatColor.WHITE + "seconds remaining");
                if(seconds == 60){
                    minutes += 1;
                    seconds = 0;
                    getServer().broadcastMessage("Time left for vote: " + (voteDuration-minutes*60-seconds)/60 + "m " + (voteDuration-minutes*60-seconds)%60 + "s");
                }
                //if(seconds%60==0)getServer().broadcastMessage("Time left for vote: " + (voteDuration-minutes*60-seconds)/60 + "m " + (voteDuration-minutes*60-seconds)%60 + "s");
                seconds++;
                voteInfo.set(2, (voteDuration-minutes*60-seconds)/60 + "m " + (voteDuration-minutes*60-seconds)%60 + "s");
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
