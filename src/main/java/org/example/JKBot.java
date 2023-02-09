package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.example.Commands.CommandManager;

import javax.security.auth.login.LoginException;

public class JKBot {

    private final Dotenv config;
    private ShardManager shardManager;

    public JKBot() throws LoginException {
        config = Dotenv.configure().load();
        String JKToken = config.get("TOKEN");

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(JKToken);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing("2048"));
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES);
        shardManager = builder.build();

        shardManager.addEventListener(new CommandManager());
    }

    public Dotenv getConfig(){
        return config;
    }
    public ShardManager getShardManager(){
        return shardManager;
    }

    public static void main(String[] args){
        try {
            JKBot bot = new JKBot();
        } catch (LoginException e){
            System.out.println("ERROR: Provided bot token is invalid!");
        }
    }
}
