package com.saloeater.voteforcommand;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;

/**
 * Created by admin1 on 17.07.2016.
 */
public class Main extends JavaPlugin implements Listener{


    private boolean voteStarted=false;

    private String[] commandList;
    private String[] voteInfoString={"&fНик: ", "&fКоманда: ", "&fОставшееся время: "};
    private String[] voteInfo={"", "", ""};

    private String prefix;

    private int voteTimer = -1;
    private int voteDuration=0;
    private int agreeVotes=0;
    private int disagreeVotes=0;

    private LinkedList<String> votedPlayers = new LinkedList<>();

    TextComponent voteYes;
    TextComponent voteNo;

    @Override
    public void onEnable(){
        if(!getDataFolder().exists()){
            getLogger().info("Create config");
            saveDefaultConfig();
        }
        registerButtons();
        prefix = ChatColor.translateAlternateColorCodes('&', "&1[&fVFC&1] ");
        commandList = String.valueOf(getConfig().getConfigurationSection("commands").getKeys(false)).replace("[", "").replace("]", "").replace(" ", "").split(",");
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void registerButtons() {
        voteYes = new TextComponent("Yes");
        voteYes.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        voteYes.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/vote agree") );
        voteYes.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Поддержать").create()));

        voteNo = new TextComponent("No");
        voteNo.setColor(net.md_5.bungee.api.ChatColor.RED);
        voteNo.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/vote disagree") );
        voteNo.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Отказать").create()));
    }

    @Override
    public void onDisable() {

        super.onDisable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        Player player = (Player) sender;
        if(args.length>0){
            switch (args[0]){
                case "start":
                        if(cmdExist(args)){
                            voteStarted=true;
                            StringBuilder comToVote = new StringBuilder().append(getConfig().getString("commands."+args[1]+".command")+" ");
                            for(int i=2; i<args.length; i++){
                                comToVote.append(args[i]+" ");
                            }
                            voteDuration = registerTime(getConfig().getString("commands."+args[1]+".timeForVote").replace(" ", "").split(","));
                            voteTask();
                            voteInfo[0]="&6"+player.getName();
                            voteInfo[1]="&b"+comToVote.toString();
                            voteInfo[2]=getVoteTime();
                            getServer().broadcastMessage(prefix+ChatColor.GOLD+player.getName()+ChatColor.GRAY+" начал голосование.");
                            getServer().broadcastMessage(prefix+ChatColor.GRAY+"Вся информация ниже:");
                            for (org.bukkit.entity.Player player1 : getServer().getOnlinePlayers()) {
                                player1.performCommand("vote info");
                            }
                        } else {
                            player.sendMessage(prefix + ChatColor.GRAY + "Неправильно введена команда.");
                        }
                    break;

                case "agree":
                    if(sender instanceof Player){
                        if(voteStarted){
                            if(!votedPlayers.contains(player.getName())) {
                                 agreeVotes++;
                                 votedPlayers.add(player.getName());
                                 player.sendMessage(prefix+ChatColor.GRAY+"Текущий результат: "+ChatColor.GREEN+agreeVotes+ChatColor.GRAY+" - "+ChatColor.RED+disagreeVotes+".");
                            } else {
                                player.sendMessage(prefix+ChatColor.GRAY+"Вы уже голосовали.");
                            }
                        } else {
                            player.sendMessage(prefix+ChatColor.GRAY+"Голосование еще не началось.");
                        }
                    }
                    break;

                case "disagree":
                    if(sender instanceof Player){
                        if(voteStarted){
                            if(!votedPlayers.contains(player.getName())) {
                                disagreeVotes++;
                                votedPlayers.add(player.getName());
                                player.sendMessage(prefix+ChatColor.GRAY+"Текущий результат: "+ChatColor.GREEN+agreeVotes+ChatColor.GRAY+" - "+ChatColor.RED+disagreeVotes+".");
                            } else {
                                player.sendMessage(prefix+ChatColor.GRAY+"Вы уже голосовали.");
                            }
                        } else {
                            player.sendMessage(prefix+ChatColor.GRAY+"Голосование еще не началось.");
                        }
                    }
                    break;

                case "help":
                    if(sender instanceof Player){
                        player.sendMessage(prefix+ChatColor.GRAY+"Для создание голосования введите "+ChatColor.AQUA+"/vote start \"команда\" \"аргументы к команде\".");
                        player.sendMessage(prefix+ChatColor.GRAY+"Для получение аргументов наберите "+ChatColor.AQUA+"/help \"полная команда\".");
                        player.sendMessage(prefix+ChatColor.GRAY+"Список команд ниже:");
                        player.sendMessage(prefix+ChatColor.AQUA+"\"команда\" "+ChatColor.GRAY+"-"+ChatColor.AQUA+"\"полная команда\".");
                        for(int i=0; i<commandList.length; i++){
                            player.sendMessage(ChatColor.RED+"#"+ChatColor.AQUA+commandList[i]+ChatColor.GRAY+" - "+ChatColor.AQUA+getConfig().getString("commands."+commandList[i]+".command"));
                        }
                    }
                    break;

                case "info":
                    if(sender instanceof Player){
                        if(voteStarted) {
                            player.sendMessage(prefix+ChatColor.GRAY+"Текущее голосование:");
                            for (int i = 0; i < 3; i++) {
                                player.sendMessage(ChatColor.RED+"#"+ChatColor.translateAlternateColorCodes('&',voteInfoString[i] + voteInfo[i]));
                            }
                            player.sendMessage(prefix+ChatColor.GRAY+"Текущий результат: "+ChatColor.GREEN+agreeVotes+ChatColor.GRAY+" - "+ChatColor.RED+disagreeVotes+".");
                            if (!votedPlayers.contains(player.getName())) {
                                player.sendMessage(prefix + ChatColor.GRAY + "Вы еще не отдали свой голос. Проголосуйте ниже: ");
                                player.sendMessage(ChatColor.DARK_AQUA + "====");
                                player.spigot().sendMessage(voteYes);
                                player.sendMessage(ChatColor.DARK_AQUA + "====");
                                player.spigot().sendMessage(voteNo);
                                player.sendMessage(ChatColor.DARK_AQUA + "====");
                            }
                        } else {
                            player.sendMessage(prefix+ChatColor.GRAY+"Голосование еще не началось.");
                        }
                    }
                    break;

                case "stop":
                    if(sender.hasPermission("vote.stop")){
                        voteStarted=false;
                        getServer().broadcastMessage(prefix+ChatColor.RED+"Голосование было остановлено!");
                        clearCFG();
                    }
                    break;
            }
        }

        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        getLogger().info("Joined");
        Player player = e.getPlayer();
        player.performCommand("vote start give " + player.getName()+" sand 1");
    }

    private String getVoteTime() {
        int hours=voteDuration/3600;
        int minutes=(voteDuration/60>60?voteDuration/60-60:voteDuration/60);
        int seconds=voteDuration%60;
        StringBuilder voteTime = new StringBuilder();
        voteTime.append("&f"+(hours>0?(hours<10?"0"+hours+"&7:":hours+"&7:"):"")).append("&f"+(minutes>0?(minutes<10?"0"+minutes+"&7:":minutes+"&7:"):"")).append("&f"+(seconds>0?(seconds<10?"0"+seconds:seconds):"00"));
        getLogger().info(voteTime.toString());
        return voteTime.toString();
    }

    private void voteTask() {

        voteTimer = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int seconds = 0;
            int builtInVD = voteDuration;
            @Override
            public void run() {
                if(!voteStarted)Bukkit.getScheduler().cancelTask(voteTimer);
                switch (builtInVD-seconds){
                    case 30:
                        getServer().broadcastMessage(prefix + ChatColor.RED + "30 " + ChatColor.GRAY + "секунд до конца голосования.");
                        break;

                    case 10:
                        getServer().broadcastMessage(prefix + ChatColor.RED + "10 " + ChatColor.GRAY + "секунд до конца голосования.");
                        break;

                    case 0:
                        if(agreeVotes>disagreeVotes){
                            getServer().broadcastMessage(prefix + ChatColor.GRAY + "Голосование прошло успешно.");
                            getLogger().info(voteInfo[1]);
                            getServer().dispatchCommand(getServer().getConsoleSender(), voteInfo[1].replace("&b", ""));
                        } else {
                            getServer().broadcastMessage(prefix + ChatColor.GRAY + "Голосование не удалось.");
                        }
                        voteStarted=false;
                        clearCFG();
                        Bukkit.getScheduler().cancelTask(voteTimer);
                        break;
                }
                seconds++;
                voteDuration--;
                voteInfo[2]=getVoteTime();
            }
        }, 20, 20).getTaskId();

    }

    private void clearCFG() {
        for(int i=0; i<votedPlayers.size(); i++){
            votedPlayers.remove(i);
        }
        agreeVotes=0;
        disagreeVotes=0;
    }

    private int registerTime(String[] timeS) {
        int seconds=0;
        switch (timeS.length){
            case 3:
                seconds=Integer.valueOf(timeS[0])*3600+Integer.valueOf(timeS[1])*60+Integer.valueOf(timeS[2]);
                break;

            case 2:
                seconds = Integer.valueOf(timeS[0])*60+Integer.valueOf(timeS[1]);
                break;

            case 1:
                seconds = Integer.valueOf(timeS[0]);
                break;
        }
        return seconds;
    }

    private boolean cmdExist(String[] args){
        String[] voteArgs;
        boolean rightCmd = false;
        for(int i=0; i<commandList.length; i++){
            if(commandList[i].equalsIgnoreCase(args[1])){
                rightCmd = true;
                break;
            }
        }
        if(rightCmd){
            voteArgs = getConfig().getString("commands." + args[1] + ".arguments").replace(" ", "").split(",");
            if(args.length-2==voteArgs.length) {
                for (int i = 0; i < voteArgs.length; i++) {
                    if (!args[i + 2].getClass().getName().replace("java.lang.", "").equalsIgnoreCase(voteArgs[i])) {
                        if (!NumberUtils.isNumber(args[i + 2])) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }


}
